package com.alkacode.shop.storage;

import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.database.AbstractRepository;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Substitui o antigo DatabaseManager (JDBC/SQLite proprio embarcado no plugin) -
 * agora sobre a conexao unica do AlkaCore (AlkaAPI#getDatabase()). Mesmo schema
 * (com prefixo alka_shop_ pra nao colidir com tabela de outro plugin no banco
 * compartilhado), mesmas queries; so a fonte da Connection e o jeito de mandar
 * pra async mudaram (AlkaAPI#getScheduler() em vez de um ExecutorService
 * proprio do plugin).
 */
public final class AlkaShopRepository extends AbstractRepository {

    private final AlkaAPI api;
    private final Logger logger;

    public AlkaShopRepository(AlkaAPI api, Logger logger, File dataFolder) {
        super(api.getDatabase());
        this.api = api;
        this.logger = logger;
        createTables();
        migrateLegacyStandaloneDb(dataFolder);
    }

    /**
     * O DatabaseManager antigo abria seu PROPRIO arquivo shop.db dentro da pasta
     * do plugin - banco fisico diferente do compartilhado do AlkaCore. Sem isso,
     * migrar pro Core apagaria silenciosamente todo historico de auto-venda e
     * estatisticas ja salvos. So roda uma vez: se achar o arquivo antigo E a
     * tabela nova estiver vazia, copia tudo e renomeia o arquivo antigo pra
     * marcar como ja migrado.
     */
    private void migrateLegacyStandaloneDb(File dataFolder) {
        File legacyFile = new File(dataFolder, "shop.db");
        if (!legacyFile.exists()) return;

        try (Connection newConn = db.getConnection();
             Statement check = newConn.createStatement();
             ResultSet rs = check.executeQuery("SELECT COUNT(*) AS c FROM alka_shop_player_data")) {
            if (rs.next() && rs.getInt("c") > 0) {
                logger.info("[AlkaShopRepository] Tabela nova ja tem dados - ignorando shop.db legado (ja migrado antes).");
                return;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao checar se a migracao de shop.db ja rodou", e);
            return;
        }

        logger.info("[AlkaShopRepository] Migrando shop.db legado pro banco compartilhado do AlkaCore...");
        int migratedPlayers = 0;
        try (Connection legacyConn = DriverManager.getConnection("jdbc:sqlite:" + legacyFile.getAbsolutePath());
             Connection newConn = db.getConnection()) {

            try (Statement legacyStmt = legacyConn.createStatement();
                 ResultSet rs = legacyStmt.executeQuery("SELECT * FROM player_shop_data");
                 PreparedStatement insert = newConn.prepareStatement("""
                        INSERT INTO alka_shop_player_data
                            (uuid, auto_sell_enabled, total_sold_coins, total_sold_escarion, total_items_sold, total_transactions)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """)) {
                while (rs.next()) {
                    insert.setString(1, rs.getString("uuid"));
                    insert.setInt(2, rs.getInt("auto_sell_enabled"));
                    insert.setDouble(3, rs.getDouble("total_sold_coins"));
                    insert.setDouble(4, rs.getDouble("total_sold_escarion"));
                    insert.setInt(5, rs.getInt("total_items_sold"));
                    insert.setInt(6, rs.getInt("total_transactions"));
                    insert.addBatch();
                    migratedPlayers++;
                }
                insert.executeBatch();
            }

            try (Statement legacyStmt = legacyConn.createStatement();
                 ResultSet rs = legacyStmt.executeQuery("SELECT * FROM player_shop_autosell_materials");
                 PreparedStatement insert = newConn.prepareStatement(
                         "INSERT INTO alka_shop_autosell_materials (uuid, material) VALUES (?, ?)")) {
                int count = 0;
                while (rs.next()) {
                    insert.setString(1, rs.getString("uuid"));
                    insert.setString(2, rs.getString("material"));
                    insert.addBatch();
                    count++;
                }
                if (count > 0) insert.executeBatch();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao migrar shop.db legado - dados antigos NAO foram tocados, "
                    + "banco novo pode estar incompleto. Investigar antes de apagar shop.db.", e);
            return;
        }

        File renamed = new File(dataFolder, "shop.db.migrated-" + System.currentTimeMillis());
        boolean ok = legacyFile.renameTo(renamed);
        logger.info("[AlkaShopRepository] Migracao concluida: " + migratedPlayers + " jogadores copiados. "
                + "shop.db " + (ok ? "renomeado pra " + renamed.getName() : "NAO PUDE ser renomeado (mas dados ja foram copiados)."));
    }

    private void createTables() {
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS alka_shop_player_data (
                        uuid VARCHAR(36) PRIMARY KEY,
                        auto_sell_enabled INTEGER NOT NULL DEFAULT 0,
                        total_sold_coins DOUBLE NOT NULL DEFAULT 0,
                        total_sold_escarion DOUBLE NOT NULL DEFAULT 0,
                        total_items_sold INTEGER NOT NULL DEFAULT 0,
                        total_transactions INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS alka_shop_autosell_materials (
                        uuid VARCHAR(36) NOT NULL,
                        material VARCHAR(64) NOT NULL,
                        PRIMARY KEY (uuid, material)
                    )
                    """);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar tabelas do AlkaShop", e);
        }
    }

    /** Chamado no join / primeiro acesso - sincrono de proposito nesses dois pontos, igual o resto da rede Alka* faz pro cache local. */
    public PlayerShopData loadSync(UUID uuid) {
        boolean autoSellAll = false;
        double totalSoldCoins = 0;
        double totalSoldEscarion = 0;
        int totalItemsSold = 0;
        int totalTransactions = 0;

        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "SELECT * FROM alka_shop_player_data WHERE uuid = ?")) {
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
        try (Connection conn = db.getConnection();
             PreparedStatement statement = conn.prepareStatement(
                     "SELECT material FROM alka_shop_autosell_materials WHERE uuid = ?")) {
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

    /** Fire-and-forget: despacha pro AlkaScheduler (R7 - nunca sincrono na main). */
    public void save(PlayerShopData data) {
        api.getScheduler().runAsync(() -> saveSync(data));
    }

    public void saveSync(PlayerShopData data) {
        String sql = upsert("alka_shop_player_data",
                new String[]{"uuid", "auto_sell_enabled", "total_sold_coins", "total_sold_escarion",
                        "total_items_sold", "total_transactions"},
                new String[]{"uuid"});
        try {
            execute(sql, ps -> {
                ps.setString(1, data.uuid().toString());
                ps.setInt(2, data.autoSellAll() ? 1 : 0);
                ps.setDouble(3, data.totalSoldCoins());
                ps.setDouble(4, data.totalSoldEscarion());
                ps.setInt(5, data.totalItemsSold());
                ps.setInt(6, data.totalTransactions());
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Falha ao salvar dados de " + data.uuid(), e);
        }
        saveMaterialsSync(data);
    }

    /** Sincroniza a tabela de materiais: deleta tudo do jogador e reinsere o estado atual. */
    private void saveMaterialsSync(PlayerShopData data) {
        try (Connection conn = db.getConnection();
             PreparedStatement delete = conn.prepareStatement(
                     "DELETE FROM alka_shop_autosell_materials WHERE uuid = ?")) {
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
        try (Connection conn = db.getConnection();
             PreparedStatement insert = conn.prepareStatement(
                     "INSERT INTO alka_shop_autosell_materials (uuid, material) VALUES (?, ?)")) {
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
