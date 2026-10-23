package com.superteams.storage;

import com.superteams.SuperTeams;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Durable audit journal for Vault-backed team-bank operations.
 * An unfinished entry is deliberately not auto-replayed: Vault providers do not expose
 * idempotency keys, so blindly replaying after a crash could duplicate money.
 */
public final class BankJournal {
    private final SuperTeams plugin;
    private final File file;
    private final YamlConfiguration data;
    public BankJournal(SuperTeams plugin) {
        this.plugin=plugin;
        this.file=new File(plugin.getDataFolder(),"bank-transactions.yml");
        this.data=YamlConfiguration.loadConfiguration(file);
        warnPending();
    }
    public synchronized String begin(UUID team,UUID player,String action,double amount) {
        String id=UUID.randomUUID().toString(), p="transactions."+id;
        data.set(p+".team",team.toString());data.set(p+".player",player.toString());
        data.set(p+".action",action);data.set(p+".amount",amount);
        data.set(p+".state","PREPARED");data.set(p+".created-at",System.currentTimeMillis());
        save();return id;
    }
    public synchronized void state(String id,String state) {data.set("transactions."+id+".state",state);data.set("transactions."+id+".updated-at",System.currentTimeMillis());save();}
    public synchronized void complete(String id) {data.set("transactions."+id+".state","COMPLETED");data.set("transactions."+id+".completed-at",System.currentTimeMillis());save();}
    private void warnPending() {
        ConfigurationSection root=data.getConfigurationSection("transactions");if(root==null)return;
        for(String id:root.getKeys(false)) {
            String state=data.getString("transactions."+id+".state","UNKNOWN");
            if(!"COMPLETED".equals(state))plugin.getLogger().severe("Bank transaction "+id+" needs manual reconciliation. state="+state+", action="+data.getString("transactions."+id+".action")+", team="+data.getString("transactions."+id+".team")+", player="+data.getString("transactions."+id+".player")+", amount="+data.getDouble("transactions."+id+".amount")+". Do not replay it blindly.");
        }
    }
    private void save() {
        try {
            plugin.getDataFolder().mkdirs();File temp=new File(plugin.getDataFolder(),"bank-transactions.yml.tmp");data.save(temp);
            try {Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(IOException ex){Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}
        } catch(IOException ex){throw new IllegalStateException("Could not persist bank transaction journal",ex);}
    }
}
