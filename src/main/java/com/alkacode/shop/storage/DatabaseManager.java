package com.alkacode.shop.storage;

import com.alkacode.shop.model.PlayerShopData;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM player_shop_data WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new PlayerShopData(uuid,
                            rs.getInt("auto_sell_enabled") != 0,
                            rs.getDouble("total_sold_coins"),
                            rs.getDouble("total_sold_escarion"),
                            rs.getInt("total_items_sold"),
                            rs.getInt("total_transactions"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao carregar dados de " + uuid, e);
        }
        return new PlayerShopData(uuid, false, 0, 0, 0, 0);
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
            statement.setInt(2, data.autoSellEnabled() ? 1 : 0);
            statement.setDouble(3, data.totalSoldCoins());
            statement.setDouble(4, data.totalSoldEscarion());
            statement.setInt(5, data.totalItemsSold());
            statement.setInt(6, data.totalTransactions());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao salvar dados de " + data.uuid(), e);
        }
    }
}
