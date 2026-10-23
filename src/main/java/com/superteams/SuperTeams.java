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
        saveResource("messages.yml", false);
        this.store = new TeamStore(this);
        store.openStorage();
        store.load();
        PluginCommand command = getCommand("team");
        if (command == null) throw new IllegalStateException("team command missing from plugin.yml");
        this.gui = new TeamGui(this, store);
        getServer().getPluginManager().registerEvents(gui, this);
        TeamCommand handler = new TeamCommand(this, store);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        getServer().getPluginManager().registerEvents(new com.superteams.listener.TeamListener(this, store, handler), this);
        getLogger().info("SuperTeams enabled. Storage: "+getConfig().getString("storage.type","YAML").toUpperCase(java.util.Locale.ROOT)+".");
    }

    @Override public void onDisable() { if (store != null) { store.save(); store.closeStorage(); } }
    public TeamStore store() { return store; }
    public TeamGui gui() { return gui; }
}