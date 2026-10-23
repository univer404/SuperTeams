package com.superteams.storage;

import com.superteams.SuperTeams;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Stores the serialized team document in a single database row.
 * TeamStore remains responsible for validating and mapping the document.
 */
public final class StorageDatabase implements AutoCloseable {
    private static final String STATE_KEY = "teams";
    private final SuperTeams plugin;
    private Connection connection;
    private String type;

    public StorageDatabase(SuperTeams plugin) {
        this.plugin = plugin;
    }

    public synchronized boolean enabled() {
        String configured = plugin.getConfig().getString("storage.type", "YAML");
        return configured.equalsIgnoreCase("SQLITE") || configured.equalsIgnoreCase("MYSQL");
    }

    public synchronized void open() throws SQLException, IOException {
        type = plugin.getConfig().getString("storage.type", "YAML").trim().toUpperCase(java.util.Locale.ROOT);
        if (type.equals("YAML")) return;
        if (type.equals("SQLITE")) {
            String configuredPath = plugin.getConfig().getString("storage.sqlite.file", "teams.db");
            Path base = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
            Path path = base.resolve(configuredPath).normalize();
            if (!path.startsWith(base)) {
                throw new IOException("SQLite file must be inside the SuperTeams data folder.");
            }
            Files.createDirectories(path.getParent());
            connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        } else if (type.equals("MYSQL")) {
            String host = plugin.getConfig().getString("storage.mysql.host", "127.0.0.1");
            int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
            String database = plugin.getConfig().getString("storage.mysql.database", "superteams");
            String params = plugin.getConfig().getString("storage.mysql.parameters", "useSSL=false&serverTimezone=UTC");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + (params.isBlank() ? "" : "?" + params);
            connection = DriverManager.getConnection(url,
                    plugin.getConfig().getString("storage.mysql.username", "superteams"),
                    plugin.getConfig().getString("storage.mysql.password", "CHANGE_ME"));
        } else {
            throw new IllegalArgumentException("Unknown storage.type '" + type + "'. Choose YAML, SQLITE, or MYSQL.");
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS superteams_state (state_key VARCHAR(32) PRIMARY KEY, state_data " +
                    (type.equals("MYSQL") ? "LONGTEXT" : "TEXT") + " NOT NULL)");
        }
    }

    public synchronized String loadTeams() throws SQLException {
        if (connection == null) return null;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT state_data FROM superteams_state WHERE state_key = ?")) {
            statement.setString(1, STATE_KEY);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    public synchronized void saveTeams(String yaml) throws SQLException {
        if (connection == null) return;
        String sql = type.equals("MYSQL")
                ? "INSERT INTO superteams_state (state_key, state_data) VALUES (?, ?) ON DUPLICATE KEY UPDATE state_data = VALUES(state_data)"
                : "INSERT INTO superteams_state (state_key, state_data) VALUES (?, ?) ON CONFLICT(state_key) DO UPDATE SET state_data = excluded.state_data";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, STATE_KEY);
            statement.setString(2, yaml);
            statement.executeUpdate();
        }
    }

    public synchronized boolean isConnected() {
        try { return connection != null && !connection.isClosed(); }
        catch (SQLException ignored) { return false; }
    }

    @Override public synchronized void close() {
        if (connection != null) try { connection.close(); }
        catch (SQLException ex) { plugin.getLogger().warning("Could not close storage connection: " + ex.getMessage()); }
        finally { connection = null; }
    }
}
