package com.superteams.listener;

import com.superteams.SuperTeams;
import com.superteams.command.TeamCommand;
import com.superteams.storage.TeamStore;
import com.superteams.team.TeamData;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class TeamListener implements Listener {
    private final TeamStore store;
    private final SuperTeams plugin;
    private final TeamCommand command;
    public TeamListener(SuperTeams plugin,TeamStore store,TeamCommand command){this.plugin=plugin;this.store=store;this.command=command;}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void onDamage(EntityDamageByEntityEvent event){
        if(!(event.getEntity() instanceof Player victim))return;
        Player attacker=null;Entity damager=event.getDamager();
        if(damager instanceof Player p)attacker=p;
        else if(damager instanceof Projectile projectile&&projectile.getShooter() instanceof Player p)attacker=p;
        if(attacker!=null){
            TeamData a=store.byMember(attacker.getUniqueId()),v=store.byMember(victim.getUniqueId());
            if(a!=null&&v!=null&&a.id().equals(v.id())&&!a.friendlyFire())event.setCancelled(true);
        }
        if(plugin.getConfig().getBoolean("home.cancel-on-damage",true)){
            if(command.hasPendingHome(victim.getUniqueId()))command.cancelPendingHome(victim,"you took damage");
            if(attacker!=null&&command.hasPendingHome(attacker.getUniqueId()))command.cancelPendingHome(attacker,"you dealt damage");
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onAnyDamage(EntityDamageEvent event){
        if(!(event.getEntity() instanceof Player player)||event instanceof EntityDamageByEntityEvent)return;
        if(plugin.getConfig().getBoolean("home.cancel-on-damage",true)&&command.hasPendingHome(player.getUniqueId()))
            command.cancelPendingHome(player,"you took damage");
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onMove(PlayerMoveEvent event){
        if(!plugin.getConfig().getBoolean("home.cancel-on-move",true)||event.getTo()==null)return;
        if(event.getFrom().getWorld()!=event.getTo().getWorld()) {command.cancelPendingHome(event.getPlayer(),"you moved");return;}
        if(event.getFrom().getBlockX()!=event.getTo().getBlockX()||event.getFrom().getBlockY()!=event.getTo().getBlockY()||event.getFrom().getBlockZ()!=event.getTo().getBlockZ())
            command.cancelPendingHome(event.getPlayer(),"you moved");
    }
}
