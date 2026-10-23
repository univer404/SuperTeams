package com.superteams.command;
import com.superteams.SuperTeams;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
/** YAML-backed invitations with expiry. */
final class InvitationStore {
 record Invite(UUID teamId, UUID inviterId) {}
 private final SuperTeams plugin; private final File file; private final YamlConfiguration data;
 InvitationStore(SuperTeams plugin){this.plugin=plugin;file=new File(plugin.getDataFolder(),"invitations.yml");data=YamlConfiguration.loadConfiguration(file);prune();}
 synchronized void put(UUID player,UUID team,UUID inviter){String p="invitations."+player;data.set(p+".team",team.toString());data.set(p+".inviter",inviter.toString());data.set(p+".expires",System.currentTimeMillis()+Math.max(60,plugin.getConfig().getLong("teams.invite-expiry-seconds",86400))*1000L);save();}
 synchronized Invite get(UUID player){String p="invitations."+player;if(!data.contains(p+".team"))return null;try{if(data.getLong(p+".expires")<System.currentTimeMillis()){remove(player);return null;}return new Invite(UUID.fromString(data.getString(p+".team")),UUID.fromString(data.getString(p+".inviter")));}catch(Exception e){remove(player);return null;}}
 synchronized boolean remove(UUID player){String p="invitations."+player;if(!data.contains(p))return false;data.set(p,null);save();return true;}
 private void prune(){var root=data.getConfigurationSection("invitations");if(root!=null)for(String id:root.getKeys(false))if(data.getLong("invitations."+id+".expires")<System.currentTimeMillis())data.set("invitations."+id,null);save();}
 private void save(){try{plugin.getDataFolder().mkdirs();File tmp=new File(plugin.getDataFolder(),"invitations.yml.tmp");data.save(tmp);try{Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException e){Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}}catch(IOException e){plugin.getLogger().severe("Could not save invitations.yml: "+e.getMessage());}}
}