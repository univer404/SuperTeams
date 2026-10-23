package com.superteams.gui;

import com.superteams.SuperTeams;
import com.superteams.storage.TeamStore;
import com.superteams.team.*;
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
import org.bukkit.inventory.meta.SkullMeta;
import java.util.*;

public final class TeamGui implements Listener {
    private final SuperTeams plugin;
    private final TeamStore store;
    private static final String MAIN=ChatColor.DARK_AQUA+"SuperTeams";
    private static final String MEMBERS=ChatColor.DARK_AQUA+"Team Members";
    private static final String RIGHTS=ChatColor.DARK_AQUA+"Member Rights";
    private static final int[] MEMBER_SLOTS={10,11,12,13,14,15,16,19,20,21,22,23,24,25};
    public TeamGui(SuperTeams plugin,TeamStore store){this.plugin=plugin;this.store=store;}
    public void open(Player player){
        TeamData team=store.byMember(player.getUniqueId());
        if(team==null){message(player,"Create a team with /team create <name>.");return;}
        Inventory inv=Bukkit.createInventory(null,27,MAIN);
        inv.setItem(4,item(Material.NAME_TAG,"&b"+team.name(),List.of("&7Members: &f"+team.members().size(),"&7Owner: &f"+name(team,team.owner()),"&7Friendly fire: "+(team.friendlyFire()?"&aEnabled":"&cDisabled"))));
        inv.setItem(10,item(Material.PLAYER_HEAD,"&eMembers &7("+team.members().size()+")",List.of("&7View members and individual rights","&fClick to open")));
        inv.setItem(12,item(Material.WHITE_BED,"&aTeam Home",List.of("&7Teleport to your team home","&f/team home")));
        inv.setItem(14,item(Material.COMPASS,"&aSet Team Home",List.of("&7Set the shared team home","&f/team sethome")));
        inv.setItem(16,item(Material.IRON_SWORD,"&cFriendly Fire",List.of("&7Current: "+(team.friendlyFire()?"&aEnabled":"&cDisabled"),"&f/team pvp")));
        inv.setItem(20,item(Material.GOLD_INGOT,"&6Team Bank",List.of("&7Balance: &f"+String.format(Locale.US,"%.2f",team.bankBalance()),"&7Use /team bank balance|deposit|withdraw")));
        inv.setItem(22,item(Material.BARRIER,"&cClose",List.of("&7Close this menu")));
        player.openInventory(inv);
    }
    private String name(TeamData t,UUID id){MemberData m=t.member(id);return m==null?"Unknown":m.lastKnownName();}
    private boolean canManage(Player p,TeamData t){MemberData m=t.member(p.getUniqueId());return t.owner().equals(p.getUniqueId())||(m!=null&&m.has(TeamRight.MANAGE_MEMBERS));}
    private ItemStack item(Material type,String name,List<String> lore){
        ItemStack stack=new ItemStack(type);ItemMeta meta=stack.getItemMeta();
        if(meta!=null){meta.setDisplayName(color(name));meta.setLore(lore.stream().map(this::color).toList());stack.setItemMeta(meta);}
        return stack;
    }
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s);}
    private void message(Player p,String s){p.sendMessage(ChatColor.AQUA+"[SuperTeams] "+ChatColor.WHITE+s);}
    private void openMembers(Player p){
        TeamData t=store.byMember(p.getUniqueId());if(t==null){open(p);return;}
        Inventory inv=Bukkit.createInventory(null,54,MEMBERS);int i=0;
        for(MemberData m:t.members().values()){
            if(i>=MEMBER_SLOTS.length)break;
            ItemStack head=item(Material.PLAYER_HEAD,"&e"+m.lastKnownName(),List.of("&7"+(m.uuid().equals(t.owner())?"Owner":"Member"),"&7Rights: &f"+m.rights().size(),"&7Click to manage rights"));
            if(head.getItemMeta() instanceof SkullMeta meta){meta.setOwningPlayer(Bukkit.getOfflinePlayer(m.uuid()));head.setItemMeta(meta);}
            inv.setItem(MEMBER_SLOTS[i++],head);
        }
        inv.setItem(49,item(Material.ARROW,"&fBack",List.of("&7Return to main menu")));
        p.openInventory(inv);
    }
    private void openRights(Player p,UUID memberId){
        TeamData t=store.byMember(p.getUniqueId());if(t==null){open(p);return;}
        MemberData member=t.member(memberId);if(member==null){openMembers(p);return;}
        if(member.uuid().equals(t.owner())){message(p,"The owner's rights are always enabled and cannot be edited.");openMembers(p);return;}
        if(!canManage(p,t)){message(p,"You need MANAGE_MEMBERS to edit rights.");openMembers(p);return;}
        Inventory inv=Bukkit.createInventory(null,54,RIGHTS);
        inv.setItem(4,item(Material.PLAYER_HEAD,"&e"+member.lastKnownName(),List.of("&7Click a right to toggle it.")));
        if(inv.getItem(4).getItemMeta() instanceof SkullMeta meta){meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.uuid()));inv.getItem(4).setItemMeta(meta);}
        TeamRight[] rights=TeamRight.values();
        for(int i=0;i<rights.length&&i<45;i++){TeamRight r=rights[i];boolean enabled=member.has(r);inv.setItem(9+i,item(enabled?Material.LIME_DYE:Material.GRAY_DYE,(enabled?"&a":"&c")+r.name(),List.of("&7Status: "+(enabled?"&aGranted":"&cDenied"),"&7Click to toggle")));}
        inv.setItem(49,item(Material.ARROW,"&fBack to members",List.of("&7Return to member list")));
        p.openInventory(inv);
    }
    @EventHandler public void click(InventoryClickEvent event){
        String title=event.getView().getTitle();
        if(!MAIN.equals(title)&&!MEMBERS.equals(title)&&!RIGHTS.equals(title))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player p)||event.getClickedInventory()==null||event.getClickedInventory()!=event.getView().getTopInventory())return;
        int slot=event.getRawSlot();
        if(MAIN.equals(title)){
            switch(slot){case 10->openMembers(p);case 12->p.performCommand("team home");case 14->p.performCommand("team sethome");case 16->p.performCommand("team pvp");case 20->p.performCommand("team bank balance");case 22->p.closeInventory();default->{}}
            return;
        }
        TeamData t=store.byMember(p.getUniqueId());if(t==null){p.closeInventory();return;}
        if(MEMBERS.equals(title)){
            if(slot==49){open(p);return;}
            int idx=-1;for(int i=0;i<MEMBER_SLOTS.length;i++)if(MEMBER_SLOTS[i]==slot){idx=i;break;}
            if(idx<0)return;
            List<MemberData> members=new ArrayList<>(t.members().values());if(idx>=members.size())return;
            openRights(p,members.get(idx).uuid());return;
        }
        if(RIGHTS.equals(title)){
            if(slot==49){openMembers(p);return;}
            if(!canManage(p,t)){message(p,"You need MANAGE_MEMBERS to edit rights.");openMembers(p);return;}
            ItemStack clicked=event.getCurrentItem();if(clicked==null||clicked.getType()==Material.AIR)return;
            int index=slot-9;TeamRight[] rights=TeamRight.values();if(index<0||index>=rights.length)return;
            // Resolve target from the inventory's member head rather than trusting display text.
            ItemStack targetHead=event.getView().getTopInventory().getItem(4);if(targetHead==null||!targetHead.hasItemMeta())return;
            String targetName=ChatColor.stripColor(targetHead.getItemMeta().getDisplayName());MemberData target=t.members().values().stream().filter(m->m.lastKnownName().equals(targetName)).findFirst().orElse(null);
            if(target==null||target.uuid().equals(t.owner())){message(p,"Cannot edit this member.");openMembers(p);return;}
            TeamRight right=rights[index];
            if(right==TeamRight.MANAGE_MEMBERS&&!t.owner().equals(p.getUniqueId())){message(p,"Only the owner may delegate MANAGE_MEMBERS.");return;}
            if(target.has(right))target.rights().remove(right);else target.rights().add(right);
            store.save();openRights(p,target.uuid());
        }
    }
}
