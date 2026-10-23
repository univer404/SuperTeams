package com.superteams.listener;

import com.superteams.storage.TeamStore;
import com.superteams.team.TeamData;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class TeamListener implements Listener {
    private final TeamStore store;
    public TeamListener(TeamStore store){this.store=store;}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void onDamage(EntityDamageByEntityEvent event){
        if(!(event.getEntity() instanceof Player victim))return;
        Player attacker=null;Entity damager=event.getDamager();
        if(damager instanceof Player p)attacker=p;
        else if(damager instanceof Projectile projectile&&projectile.getShooter() instanceof Player p)attacker=p;
        if(attacker==null)return;
        TeamData a=store.byMember(attacker.getUniqueId()),v=store.byMember(victim.getUniqueId());
        if(a!=null&&v!=null&&a.id().equals(v.id())&&!a.friendlyFire())event.setCancelled(true);
    }
}