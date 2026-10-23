package com.superteams.storage;

import com.superteams.SuperTeams;
import com.superteams.team.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class TeamStore {
    private final SuperTeams plugin;
    private final Map<UUID,TeamData> teams=new LinkedHashMap<>();
    private final Map<UUID,UUID> membership=new HashMap<>();
    private final Map<String,UUID> names=new HashMap<>();
    private final File file;\n    private final StorageDatabase database;
    public TeamStore(SuperTeams plugin){this.plugin=plugin;this.file=new File(plugin.getDataFolder(),"teams.yml");this.database=new StorageDatabase(plugin);}\n    public void openStorage(){try{database.open();if(database.isConnected())plugin.getLogger().info("Storage connected: "+plugin.getConfig().getString("storage.type"));}catch(Exception ex){throw new IllegalStateException("Could not initialize configured storage backend: "+ex.getMessage(),ex);}}\n    public void closeStorage(){database.close();}
    public synchronized Collection<TeamData> teams(){return List.copyOf(teams.values());}
    public synchronized TeamData byId(UUID id){return teams.get(id);}
    public synchronized TeamData byName(String name){UUID id=names.get(key(name));return id==null?null:teams.get(id);}
    public synchronized TeamData byMember(UUID uuid){UUID id=membership.get(uuid);return id==null?null:teams.get(id);}
    private String key(String n){return plugin.getConfig().getBoolean("teams.case-sensitive",false)?n:n.toLowerCase(Locale.ROOT);}
    public synchronized boolean create(String name,UUID owner,String ownerName){
        if(byName(name)!=null||membership.containsKey(owner))return false;
        TeamData team=new TeamData(UUID.randomUUID(),name,owner,System.currentTimeMillis());
        team.members().get(owner).lastKnownName(ownerName);
        teams.put(team.id(),team);names.put(key(name),team.id());membership.put(owner,team.id());save();return true;
    }
    public synchronized boolean join(TeamData team,UUID player,String playerName){
        if(team==null||membership.containsKey(player)||team.members().size()>=plugin.getConfig().getInt("teams.max-members",10))return false;
        if(!team.addMember(new MemberData(player,playerName,System.currentTimeMillis(),Set.of())))return false;
        membership.put(player,team.id());save();return true;
    }
    public synchronized boolean disband(TeamData team){if(team==null||teams.remove(team.id())==null)return false;for(UUID member:team.members().keySet())membership.remove(member);names.remove(key(team.name()));save();return true;}
    public synchronized boolean leave(UUID player){
        TeamData team=byMember(player);if(team==null||team.owner().equals(player))return false;
        team.removeMember(player);membership.remove(player);save();return true;
    }
    public synchronized boolean kick(TeamData team,UUID player){
        if(team==null||team.owner().equals(player)||!team.members().containsKey(player))return false;
        team.removeMember(player);membership.remove(player);save();return true;
    }
    public synchronized void save(){
        YamlConfiguration y=new YamlConfiguration(); y.set("schema-version",1);
        for(TeamData t:teams.values()){
            String p="teams."+t.id(); y.set(p+".name",t.name());y.set(p+".owner",t.owner().toString());y.set(p+".created-at",t.createdAt());
            y.set(p+".friendly-fire",t.friendlyFire());y.set(p+".bank-balance",t.bankBalance());
            y.set(p+".home.set",t.homeSet());
            if(t.homeSet()){y.set(p+".home.world",t.homeWorld());y.set(p+".home.x",t.homeX());y.set(p+".home.y",t.homeY());y.set(p+".home.z",t.homeZ());y.set(p+".home.yaw",t.homeYaw());y.set(p+".home.pitch",t.homePitch());}
            for(int slot=0;slot<27;slot++){org.bukkit.inventory.ItemStack chestItem=t.enderChestContents()[slot];if(chestItem!=null)y.set(p+".enderchest."+slot,chestItem);}
            for(MemberData m:t.members().values()){String mp=p+".members."+m.uuid();y.set(mp+".name",m.lastKnownName());y.set(mp+".joined-at",m.joinedAt());y.set(mp+".rights",m.rights().stream().map(Enum::name).toList());}
        }
        File tmp=new File(plugin.getDataFolder(),"teams.yml.tmp");
        try{plugin.getDataFolder().mkdirs();y.save(tmp);try{java.nio.file.Files.move(tmp.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}catch(IOException ex){java.nio.file.Files.move(tmp.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}}
        catch(IOException ex){plugin.getLogger().severe("Could not safely save teams.yml: "+ex.getMessage());}
    }
    public synchronized void load(){
        if(!file.exists())return;
        YamlConfiguration y=YamlConfiguration.loadConfiguration(file);ConfigurationSection root=y.getConfigurationSection("teams");if(root==null)return;
        for(String idText:root.getKeys(false))try{
            UUID id=UUID.fromString(idText);String p="teams."+idText;String name=y.getString(p+".name");UUID owner=UUID.fromString(y.getString(p+".owner"));
            TeamData t=new TeamData(id,name,owner,y.getLong(p+".created-at"));t.members().clear();t.friendlyFire(y.getBoolean(p+".friendly-fire"));t.bankBalance(Math.max(0,y.getDouble(p+".bank-balance")));
            if(y.getBoolean(p+".home.set"))t.home(y.getString(p+".home.world"),y.getDouble(p+".home.x"),y.getDouble(p+".home.y"),y.getDouble(p+".home.z"),(float)y.getDouble(p+".home.yaw"),(float)y.getDouble(p+".home.pitch"));
            for(int slot=0;slot<27;slot++){org.bukkit.inventory.ItemStack chestItem=y.getItemStack(p+".enderchest."+slot);if(chestItem!=null){org.bukkit.inventory.ItemStack[] contents=t.enderChestContents();contents[slot]=chestItem;t.enderChestContents(contents);}}
            ConfigurationSection ms=y.getConfigurationSection(p+".members");if(ms!=null)for(String mid:ms.getKeys(false)){UUID mu=UUID.fromString(mid);Set<TeamRight> rights=EnumSet.noneOf(TeamRight.class);for(String r:y.getStringList(p+".members."+mid+".rights"))try{rights.add(TeamRight.valueOf(r));}catch(IllegalArgumentException ignored){}t.members().put(mu,new MemberData(mu,y.getString(p+".members."+mid+".name","unknown"),y.getLong(p+".members."+mid+".joined-at"),rights));membership.put(mu,id);}
            if(!t.members().containsKey(owner)){t.members().put(owner,new MemberData(owner,"unknown",t.createdAt(),EnumSet.allOf(TeamRight.class)));plugin.getLogger().warning("Repaired missing owner membership for team "+name);}
            t.members().get(owner).rights().addAll(EnumSet.allOf(TeamRight.class));teams.put(id,t);names.put(key(name),id);
        }catch(Exception ex){plugin.getLogger().severe("Skipped invalid team record "+idText+": "+ex.getMessage());}
    }
}