package com.superteams;

import com.superteams.command.TeamCommand;
import com.superteams.storage.TeamStore;
import com.superteams.gui.TeamGui;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SuperTeams extends JavaPlugin {
    private TeamStore store;
    private TeamGui gui;

    @Override public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("gui.yml");
        this.store = new TeamStore(this);
        store.load();
        PluginCommand command = getCommand("team");
        if (command == null) throw new IllegalStateException("team command missing from plugin.yml");
        this.gui = new TeamGui(this, store);
        getServer().getPluginManager().registerEvents(gui, this);
        TeamCommand handler = new TeamCommand(this, store);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        getServer().getPluginManager().registerEvents(new com.superteams.listener.TeamListener(this, store, handler), this);
        getLogger().info("SuperTeams enabled. Using lightweight YAML storage.");
    }

    private void saveResourceIfMissing(String path) {
        java.io.File target = new java.io.File(getDataFolder(), path);
        if (target.exists()) return;
        java.io.File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            getLogger().severe("Could not create configuration directory for " + path);
            return;
        }
        saveResource(path, false);
    }

    @Override public void onDisable() { if (store != null) { store.save(); } }
    public TeamStore store() { return store; }
    public TeamGui gui() { return gui; }
}