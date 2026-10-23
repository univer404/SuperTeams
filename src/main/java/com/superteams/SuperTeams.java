package com.superteams;

import com.superteams.command.TeamCommand;
import com.superteams.storage.TeamStore;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SuperTeams extends JavaPlugin {
    private TeamStore store;

    @Override public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        this.store = new TeamStore(this);
        store.load();
        PluginCommand command = getCommand("team");
        if (command == null) throw new IllegalStateException("team command missing from plugin.yml");
        TeamCommand handler = new TeamCommand(this, store);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        getServer().getPluginManager().registerEvents(new com.superteams.listener.TeamListener(store), this);
        getServer().getPluginManager().registerEvents(new com.superteams.gui.TeamGui(store), this);
        getLogger().info("SuperTeams enabled. Storage: YAML. Development build; not yet production-ready.");
    }

    @Override public void onDisable() { if (store != null) store.save(); }
    public TeamStore store() { return store; }
}