package com.alkacode.shop.storage;

import com.alkacode.shop.model.PlayerShopData;
import org.bukkit.Material;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite nao lida bem com escrita concorrente, entao todo acesso ao JDBC e serializado
 * num unico executor single-thread - mesmo padrao usado em todo plugin Alka*.
 */
public final class DatabaseManager {

    private final Logger logger;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "AlkaShop-SQLite");
        thread.setDaemon(true);
        return thread;
    });

    private Connection connection;

    public DatabaseManager(Logger logger) {
        this.logger = logger;
    }

    public void open(File dataFolder) throws SQLException {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "shop.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_shop_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    auto_sell_enabled INTEGER NOT NULL DEFAULT 0,
                    total_sold_coins DOUBLE NOT NULL DEFAULT 0,
                    total_sold_escarion DOUBLE NOT NULL DEFAULT 0,
                    total_items_sold INTEGER NOT NULL DEFAULT 0,
                    total_transactions INTEGER NOT NULL DEFAULT 0
                )
                """);
            // auto_sell_enabled agora significa "vender TUDO automaticamente" (PlayerShopData#autoSellAll) -
            // mesma coluna, sem migracao de schema, so mudou o que o campo booleano representa em Java.
            // Selecao por material fica numa tabela separada - feature nova, comeca vazia pra todo mundo,
            // nao precisa de migracao nenhuma.
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_shop_autosell_materials (
                    uuid VARCHAR(36) NOT NULL,
                    material VARCHAR(64) NOT NULL,
                    PRIMARY KEY (uuid, material)
                )
                """);
        }
    }

    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.log(Level.WARNING, "Timeout aguardando saves pendentes do AlkaShop.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Falha ao fechar conexao SQLite", e);
            }
        }
    }

    public PlayerShopData loadSync(UUID uuid) {
        boolean autoSellAll = false;
        double totalSoldCoins = 0;
        double totalSoldEscarion = 0;
        int totalItemsSold = 0;
        int totalTransactions = 0;

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM player_shop_data WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    autoSellAll = rs.getInt("auto_sell_enabled") != 0;
                    totalSoldCoins = rs.getDouble("total_sold_coins");
                    totalSoldEscarion = rs.getDouble("total_sold_escarion");
                    totalItemsSold = rs.getInt("total_items_sold");
                    totalTransactions = rs.getInt("total_transactions");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar dados de " + uuid, e);
        }

        Set<Material> materials = loadMaterialsSync(uuid);
        return new PlayerShopData(uuid, autoSellAll, materials, totalSoldCoins, totalSoldEscarion,
                totalItemsSold, totalTransactions);
    }

    private Set<Material> loadMaterialsSync(UUID uuid) {
        Set<Material> materials = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT material FROM player_shop_autosell_materials WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Material material = Material.matchMaterial(rs.getString("material"));
                    if (material != null) {
                        materials.add(material);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar materiais de auto-venda de " + uuid, e);
        }
        return materials;
    }

    public CompletableFuture<PlayerShopData> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> loadSync(uuid), executor);
    }

    public CompletableFuture<Void> save(PlayerShopData data) {
        return CompletableFuture.runAsync(() -> saveSync(data), executor);
    }

    public void saveSync(PlayerShopData data) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO player_shop_data
                    (uuid, auto_sell_enabled, total_sold_coins, total_sold_escarion, total_items_sold, total_transactions)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET auto_sell_enabled = excluded.auto_sell_enabled,
                    total_sold_coins = excluded.total_sold_coins, total_sold_escarion = excluded.total_sold_escarion,
                    total_items_sold = excluded.total_items_sold, total_transactions = excluded.total_transactions
                """)) {
            statement.setString(1, data.uuid().toString());
            statement.setInt(2, data.autoSellAll() ? 1 : 0);
            statement.setDouble(3, data.totalSoldCoins());
            statement.setDouble(4, data.totalSoldEscarion());
            statement.setInt(5, data.totalItemsSold());
            statement.setInt(6, data.totalTransactions());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao salvar dados de " + data.uuid(), e);
        }
        saveMaterialsSync(data);
    }

    /** Sincroniza a tabela de materiais: deleta tudo do jogador e reinsere o estado atual. */
    private void saveMaterialsSync(PlayerShopData data) {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM player_shop_autosell_materials WHERE uuid = ?")) {
            delete.setString(1, data.uuid().toString());
            delete.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao limpar materiais de auto-venda de " + data.uuid(), e);
            return;
        }

        Set<Material> materials = data.autoSellMaterials();
        if (materials.isEmpty()) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO player_shop_autosell_materials (uuid, material) VALUES (?, ?)")) {
            for (Material material : materials) {
                insert.setString(1, data.uuid().toString());
                insert.setString(2, material.name());
                insert.addBatch();
            }
            insert.executeBatch();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao salvar materiais de auto-venda de " + data.uuid(), e);
        }
    }
}
