package com.superteams.command;

import com.superteams.SuperTeams;
import com.superteams.storage.TeamStore;
import com.superteams.team.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TeamCommand implements TabExecutor {
    private final SuperTeams plugin; private final TeamStore store;
    public TeamCommand(SuperTeams plugin,TeamStore store){this.plugin=plugin;this.store=store;}
    private void msg(CommandSender s,String m){s.sendMessage(ChatColor.translateAlternateColorCodes('&',"&b[SuperTeams] &f"+m));}
    private boolean right(Player p,TeamData t,TeamRight r){MemberData m=t.member(p.getUniqueId());return t.owner().equals(p.getUniqueId())||(m!=null&&m.has(r));}
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] a){
        if(a.length==0){if(!(sender instanceof Player p)){msg(sender,"Players only.");return true;}new com.superteams.gui.TeamGui(store).openMain(p);return true;}
        String sub=a[0].toLowerCase(Locale.ROOT);
        if(sub.equals("create")){if(!(sender instanceof Player p)){msg(sender,"Players only.");return true;}if(!sender.hasPermission("superteams.create")){msg(sender,"No permission.");return true;}if(a.length<2){msg(sender,"Usage: /team create <name>");return true;}String name=a[1];int min=plugin.getConfig().getInt("teams.name-min-length",3),max=plugin.getConfig().getInt("teams.name-max-length",16);if(name.length()<min||name.length()>max||!Pattern.matches(plugin.getConfig().getString("teams.allowed-name-regex","^[A-Za-z0-9_]+$"),name)){msg(p,"Invalid team name.");return true;}if(store.byMember(p.getUniqueId())!=null){msg(p,"You already belong to a team.");return true;}if(!store.create(name,p.getUniqueId(),p.getName())){msg(p,"That team name is already taken.");return true;}msg(p,"Created team "+name+".");return true;}
        if(!(sender instanceof Player p)){msg(sender,"Players only.");return true;}TeamData t=store.byMember(p.getUniqueId());
        switch(sub){
            case "info"->{TeamData target=a.length>1?store.byName(a[1]):t;if(target==null){msg(p,"Team not found.");return true;}msg(p,target.name()+" | members "+target.members().size()+" | friendly fire "+target.friendlyFire());}
            case "leave"->{if(t==null){msg(p,"You are not in a team.");return true;}if(store.leave(p.getUniqueId()))msg(p,"You left "+t.name()+".");else msg(p,"Owner must transfer ownership or disband the team (not yet implemented).");}
            case "invite"->{if(t==null){msg(p,"You are not in a team.");return true;}if(!right(p,t,TeamRight.INVITE)){msg(p,"Missing INVITE right.");return true;}if(a.length<2){msg(p,"Usage: /team invite <player>");return true;}Player target=Bukkit.getPlayerExact(a[1]);if(target==null){msg(p,"Target must be online.");return true;}if(target.equals(p)||store.byMember(target.getUniqueId())!=null||t.members().size()>=plugin.getConfig().getInt("teams.max-members",10)){msg(p,"Target cannot join this team.");return true;}plugin.getLogger().warning("Invitation workflow is not yet implemented; refusing to pretend an invitation was sent.");msg(p,"Invitations are not available in this development build.");}
            case "kick"->{if(t==null||a.length<2){msg(p,"Usage: /team kick <player>");return true;}if(!right(p,t,TeamRight.KICK)){msg(p,"Missing KICK right.");return true;}MemberData target=t.members().values().stream().filter(m->m.lastKnownName().equalsIgnoreCase(a[1])).findFirst().orElse(null);if(target==null||target.uuid().equals(p.getUniqueId())){msg(p,"Member not found or invalid target.");return true;}if(store.kick(t,target.uuid())){msg(p,"Removed "+target.lastKnownName()+".");Player online=Bukkit.getPlayer(target.uuid());if(online!=null)msg(online,"You were removed from "+t.name()+".");}else msg(p,"Could not remove that member.");}
            case "pvp"->{if(t==null||!right(p,t,TeamRight.PVP)){msg(p,"Missing PVP right or no team.");return true;}t.friendlyFire(!t.friendlyFire());store.save();msg(p,"Friendly fire "+(t.friendlyFire()?"enabled":"disabled")+".");}
            case "chat"->{if(t==null||!right(p,t,TeamRight.CHAT)){msg(p,"Missing CHAT right or no team.");return true;}if(a.length<2){msg(p,"Usage: /team chat <message>");return true;}String text=Arrays.stream(a,1,a.length).collect(Collectors.joining(" "));for(MemberData m:t.members().values()){Player receiver=Bukkit.getPlayer(m.uuid());if(receiver!=null&&right(receiver,t,TeamRight.CHAT))msg(receiver,"[Team "+t.name()+"] "+p.getName()+": "+text);}if(plugin.getConfig().getBoolean("chat.spy-enabled",true))for(Player staff:Bukkit.getOnlinePlayers())if(staff.hasPermission("superteams.chatspy")&&t.member(staff.getUniqueId())==null)msg(staff,"[SPY:"+t.name()+"] "+p.getName()+": "+text);return true;}
            default->msg(p,"Commands: create, info, invite, kick, leave, chat, pvp. Main GUI and remaining features are still under development.");
        }return true;
    }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){if(args.length==1)return List.of("create","invite","kick","leave","info","chat","pvp","home","sethome","accept","decline","rights","bank","reload").stream().filter(x->x.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();if(args.length==2&&args[0].equalsIgnoreCase("kick")){TeamData t=s instanceof Player p?store.byMember(p.getUniqueId()):null;return t==null?List.of():t.members().values().stream().map(MemberData::lastKnownName).filter(n->n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();}return List.of();}
}