package com.superteams.gui;

import com.superteams.SuperTeams;
import com.superteams.storage.TeamStore;
import com.superteams.team.TeamData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public final class TeamGui implements Listener {
    private final SuperTeams plugin;
    private final TeamStore store;
    private static final String TITLE=ChatColor.DARK_AQUA+"SuperTeams";
    public TeamGui(SuperTeams plugin,TeamStore store){this.plugin=plugin;this.store=store;}
    public void open(Player player){
        TeamData team=store.byMember(player.getUniqueId());
        if(team==null){player.sendMessage(ChatColor.AQUA+"[SuperTeams] "+ChatColor.WHITE+"Create a team with /team create <name>.");return;}
        Inventory inv=Bukkit.createInventory(null,27,TITLE);
        inv.setItem(4,item(Material.NAME_TAG,"&b"+team.name(),List.of("&7Members: &f"+team.members().size(),"&7Owner: &f"+team.members().get(team.owner()).lastKnownName(),"&7Friendly fire: "+(team.friendlyFire()?"&aEnabled":"&cDisabled"))));
        inv.setItem(10,item(Material.PLAYER_HEAD,"&eMembers",List.of("&7View member names and rights","&f/team")));
        inv.setItem(12,item(Material.WHITE_BED,"&aTeam Home",List.of("&7Teleport to your team home","&f/team home")));
        inv.setItem(14,item(Material.COMPASS,"&aSet Team Home",List.of("&7Set the shared team home","&f/team sethome")));
        inv.setItem(16,item(Material.IRON_SWORD,"&cToggle Friendly Fire",List.of("&7Current: "+(team.friendlyFire()?"&aEnabled":"&cDisabled"),"&f/team pvp")));
        inv.setItem(22,item(Material.BARRIER,"&cClose",List.of("&7Close this menu")));
        player.openInventory(inv);
    }
    private ItemStack item(Material type,String name,List<String> lore){
        ItemStack stack=new ItemStack(type);ItemMeta meta=stack.getItemMeta();
        if(meta!=null){meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',name));meta.setLore(lore.stream().map(x->ChatColor.translateAlternateColorCodes('&',x)).toList());stack.setItemMeta(meta);}
        return stack;
    }
    @EventHandler public void click(InventoryClickEvent event){
        if(!TITLE.equals(event.getView().getTitle()))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player)||event.getClickedInventory()==null||event.getClickedInventory()!=event.getView().getTopInventory())return;
        switch(event.getRawSlot()){
            case 12->player.performCommand("team home");
            case 14->player.performCommand("team sethome");
            case 16->player.performCommand("team pvp");
            case 22->player.closeInventory();
            default->{}
        }
    }
}