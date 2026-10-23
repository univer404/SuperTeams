package com.superteams.gui;

import com.superteams.storage.TeamStore;
import com.superteams.team.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.*;

public final class TeamGui implements Listener {
    private final TeamStore store;
    public TeamGui(TeamStore store) { this.store = store; }

    private ItemStack button(Material type, String name, String... lore) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b" + name);
        meta.setLore(Arrays.stream(lore).map(s -> "§7" + s).toList());
        item.setItemMeta(meta);
        return item;
    }

    public void openMain(Player player) {
        TeamData team = store.byMember(player.getUniqueId());
        if (team == null) { player.sendMessage("§b[SuperTeams] §fCreate a team with /team create <name>."); return; }
        Inventory inv = Bukkit.createInventory(null, 27, "§8SuperTeams | " + team.name());
        inv.setItem(4, button(Material.NETHER_STAR, team.name(), "Owner: " + Bukkit.getOfflinePlayer(team.owner()).getName(), "Members: " + team.members().size()));
        inv.setItem(10, button(Material.PLAYER_HEAD, "Members", "Click to view members and rights."));
        inv.setItem(12, button(Material.WHITE_BANNER, "Team Home", team.homeSet() ? "Home configured" : "No home set", "Home commands are /team sethome and /team home."));
        inv.setItem(14, button(Material.PAPER, "Team Chat", "Use /team chat <message>."));
        inv.setItem(16, button(Material.IRON_SWORD, "Friendly Fire: " + (team.friendlyFire() ? "ON" : "OFF"), "Requires PVP right to toggle."));
        inv.setItem(22, button(Material.BARRIER, "Close"));
        player.openInventory(inv);
    }

    private boolean canManage(Player player, TeamData team) {
        MemberData member = team.member(player.getUniqueId());
        return team.owner().equals(player.getUniqueId()) || (member != null && member.has(TeamRight.MANAGE_MEMBERS));
    }

    public void openMembers(Player player) {
        TeamData team = store.byMember(player.getUniqueId());
        if (team == null) return;
        Inventory inv = Bukkit.createInventory(null, 54, "§8SuperTeams Members");
        int slot = 0;
        for (MemberData member : team.members().values()) {
            if (slot >= 45) break;
            ItemStack head = button(Material.PLAYER_HEAD, member.lastKnownName(), "Rights: " + member.rights(), "Click to manage rights.");
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.uuid()));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }
        inv.setItem(49, button(Material.ARROW, "Back"));
        player.openInventory(inv);
    }

    public void openRights(Player player, UUID targetId) {
        TeamData team = store.byMember(player.getUniqueId());
        if (team == null || !canManage(player, team)) return;
        MemberData target = team.member(targetId);
        if (target == null) return;
        Inventory inv = Bukkit.createInventory(null, 54, "§8Rights | " + target.uuid());
        int slot = 0;
        for (TeamRight right : TeamRight.values()) {
            boolean enabled = target.has(right);
            inv.setItem(slot++, button(enabled ? Material.LIME_DYE : Material.GRAY_DYE, right.name(), enabled ? "Enabled" : "Disabled", "Click to toggle."));
        }
        inv.setItem(49, button(Material.ARROW, "Back"));
        player.openInventory(inv);
    }

    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith("§8SuperTeams") && !title.startsWith("§8Rights |")) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        TeamData team = store.byMember(player.getUniqueId());
        if (team == null) return;
        int slot = event.getRawSlot();
        if (title.startsWith("§8SuperTeams |")) {
            if (slot == 10) openMembers(player);
            else if (slot == 22) player.closeInventory();
            else if (slot == 16) {
                MemberData actor = team.member(player.getUniqueId());
                if (team.owner().equals(player.getUniqueId()) || (actor != null && actor.has(TeamRight.PVP))) {
                    team.friendlyFire(!team.friendlyFire()); store.save(); openMain(player);
                } else player.sendMessage("§cYou need the PVP right.");
            }
        } else if (title.equals("§8SuperTeams Members")) {
            if (slot == 49) { openMain(player); return; }
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta() || !canManage(player, team)) return;
            String name = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
            team.members().values().stream().filter(m -> m.lastKnownName().equals(name) && !m.uuid().equals(team.owner()))
                .findFirst().ifPresent(m -> openRights(player, m.uuid()));
        } else if (title.startsWith("§8Rights |")) {
            if (slot == 49) { openMembers(player); return; }
            if (!canManage(player, team)) return;
            UUID targetId;
            try { targetId = UUID.fromString(title.substring("§8Rights |".length())); } catch (IllegalArgumentException ex) { return; }
            MemberData target = team.member(targetId);
            if (target == null || target.uuid().equals(team.owner())) return;
            TeamRight[] rights = TeamRight.values();
            if (slot < 0 || slot >= rights.length) return;
            MemberData actor = team.member(player.getUniqueId());
            if (!team.owner().equals(player.getUniqueId()) && (actor == null || !actor.has(TeamRight.MANAGE_MEMBERS))) return;
            TeamRight right = rights[slot];
            if (target.has(right)) target.rights().remove(right); else target.rights().add(right);
            store.save(); openRights(player, target.uuid());
        }
    }

    @EventHandler public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.startsWith("§8SuperTeams") || title.startsWith("§8Rights |")) event.setCancelled(true);
    }
}