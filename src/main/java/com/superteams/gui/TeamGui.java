package com.superteams.gui;

import com.superteams.SuperTeams;
import com.superteams.storage.TeamStore;
import com.superteams.team.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.*;

public final class TeamGui implements Listener {
    private final SuperTeams plugin;
    private final TeamStore store;

    public TeamGui(SuperTeams plugin, TeamStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    private YamlConfiguration gui() {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui.yml"));
    }

    private String title(YamlConfiguration cfg, String path, String fallback) {
        return color(cfg.getString(path + ".title", fallback));
    }

    private int size(YamlConfiguration cfg, String path, int fallback) {
        int value = cfg.getInt(path + ".size", fallback);
        return value >= 9 && value <= 54 && value % 9 == 0 ? value : fallback;
    }

    private int slot(YamlConfiguration cfg, String path, int fallback, int size) {
        int value = cfg.getInt(path + ".slot", fallback);
        return value >= 0 && value < size ? value : fallback;
    }

    private Material material(YamlConfiguration cfg, String path, Material fallback) {
        Material value = Material.matchMaterial(cfg.getString(path, fallback.name()));
        return value == null || !value.isItem() ? fallback : value;
    }

    private ItemStack item(YamlConfiguration cfg, String path, Material fallbackMaterial,
                           String fallbackName, List<String> fallbackLore, Map<String, String> replacements) {
        Material type = material(cfg, path + ".material", fallbackMaterial);
        ItemStack stack = new ItemStack(type);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        String name = cfg.getString(path + ".name", fallbackName);
        for (var replacement : replacements.entrySet()) name = name.replace(replacement.getKey(), replacement.getValue());
        meta.setDisplayName(color(name));
        List<String> sourceLore = cfg.contains(path + ".lore") ? cfg.getStringList(path + ".lore") : fallbackLore;
        List<String> lore = new ArrayList<>();
        for (String line : sourceLore) {
            for (var replacement : replacements.entrySet()) line = line.replace(replacement.getKey(), replacement.getValue());
            lore.add(color(line));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack item(YamlConfiguration cfg, String path, Material fallbackMaterial,
                           String fallbackName, List<String> fallbackLore) {
        return item(cfg, path, fallbackMaterial, fallbackName, fallbackLore, Map.of());
    }

    public void open(Player player) {
        TeamData team = store.byMember(player.getUniqueId());
        if (team == null) { message(player, "Create a team with /team create <name>."); return; }
        YamlConfiguration cfg = gui();
        int size = size(cfg, "main", 27);
        Inventory inv = Bukkit.createInventory(null, size, title(cfg, "main", "&3SuperTeams"));
        String pvp = team.friendlyFire() ? "&aEnabled" : "&cDisabled";
        Map<String, String> values = Map.of("%team%", team.name(), "%members%", String.valueOf(team.members().size()),
                "%owner%", name(team, team.owner()), "%pvp%", color(pvp),
                "%balance%", String.format(Locale.US, "%.2f", team.bankBalance()));
        inv.setItem(slot(cfg, "main.items.overview", 4, size),
                item(cfg, "main.items.overview", Material.NAME_TAG, "&b%team%",
                        List.of("&7Members: &f%members%", "&7Owner: &f%owner%", "&7Friendly fire: %pvp%"), values));
        inv.setItem(slot(cfg, "main.items.members", 10, size),
                item(cfg, "main.items.members", Material.PLAYER_HEAD, "&eMembers &7(%members%)",
                        List.of("&7View members and individual rights", "&fClick to open"), values));
        inv.setItem(slot(cfg, "main.items.home", 12, size), item(cfg, "main.items.home", Material.WHITE_BED, "&aTeam Home", List.of("&7Teleport to your team home", "&f/team home")));
        inv.setItem(slot(cfg, "main.items.set-home", 14, size), item(cfg, "main.items.set-home", Material.COMPASS, "&aSet Team Home", List.of("&7Set the shared team home", "&f/team sethome")));
        inv.setItem(slot(cfg, "main.items.friendly-fire", 16, size),
                item(cfg, "main.items.friendly-fire", Material.IRON_SWORD, "&cFriendly Fire", List.of("&7Current: %pvp%", "&f/team pvp"), values));
        inv.setItem(slot(cfg, "main.items.bank", 20, size), item(cfg, "main.items.bank", Material.GOLD_INGOT, "&6Team Bank", List.of("&7Balance: &f%balance%", "&7Use /team bank balance|deposit|withdraw"), values));
        inv.setItem(slot(cfg, "main.items.close", 22, size), item(cfg, "main.items.close", Material.BARRIER, "&cClose", List.of("&7Close this menu")));
        inv.setItem(slot(cfg, "main.items.ender-chest", 24, size), item(cfg, "main.items.ender-chest", Material.ENDER_CHEST, "&5Team Ender Chest", List.of("&7Shared storage for your team", "&7Permission: ENDERCHEST_USE", "&f/team enderchest")));
        player.openInventory(inv);
    }

    public void openEnderChest(Player player, TeamData team) {
        TeamData current = store.byMember(player.getUniqueId());
        if (current == null || !current.id().equals(team.id())) { message(player, "You are not a member of that team."); return; }
        MemberData member = team.member(player.getUniqueId());
        if (!team.owner().equals(player.getUniqueId()) && (member == null || !member.has(TeamRight.ENDERCHEST_USE))) {
            message(player, "You need ENDERCHEST_USE to access team storage."); return;
        }
        YamlConfiguration cfg = gui();
        int size = Math.max(team.enderChestContents().length, size(cfg, "ender-chest", 27));
        if (size < 27) size = 27;
        if (size > 54) size = 54;
        TeamChestHolder holder = new TeamChestHolder(team.id());
        String chestTitle = cfg.getString("ender-chest.title", "&5%team% Ender Chest").replace("%team%", team.name());
        Inventory inventory = Bukkit.createInventory(holder, size, color(chestTitle));
        holder.inventory(inventory);
        ItemStack[] contents = team.enderChestContents();
        inventory.setContents(Arrays.copyOf(contents, size));
        player.openInventory(inventory);
    }

    private static final class TeamChestHolder implements InventoryHolder {
        private final UUID teamId;
        private Inventory inventory;
        private TeamChestHolder(UUID teamId) { this.teamId = teamId; }
        public UUID teamId() { return teamId; }
        private void inventory(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private String name(TeamData team, UUID id) {
        MemberData member = team.member(id);
        return member == null ? "Unknown" : member.lastKnownName();
    }
    private boolean canManage(Player player, TeamData team) {
        MemberData member = team.member(player.getUniqueId());
        return team.owner().equals(player.getUniqueId()) || (member != null && member.has(TeamRight.MANAGE_MEMBERS));
    }
    private String color(String value) { return ChatColor.translateAlternateColorCodes('&', value); }
    private void message(Player player, String value) { player.sendMessage(ChatColor.AQUA + "[SuperTeams] " + ChatColor.WHITE + value); }

    private List<Integer> memberSlots(YamlConfiguration cfg) {
        List<Integer> slots = cfg.getIntegerList("members.slots");
        if (slots.isEmpty()) slots = List.of(10,11,12,13,14,15,16,19,20,21,22,23,24,25);
        return slots;
    }

    private void openMembers(Player player) {
        TeamData team = store.byMember(player.getUniqueId());
        if (team == null) { open(player); return; }
        YamlConfiguration cfg = gui();
        int size = size(cfg, "members", 54);
        Inventory inv = Bukkit.createInventory(null, size, title(cfg, "members", "&3Team Members"));
        List<Integer> slots = memberSlots(cfg);
        int index = 0;
        for (MemberData member : team.members().values()) {
            if (index >= slots.size()) break;
            int targetSlot = slots.get(index++);
            if (targetSlot < 0 || targetSlot >= size) continue;
            Map<String, String> values = Map.of("%player%", member.lastKnownName(),
                    "%role%", member.uuid().equals(team.owner()) ? "Owner" : "Member",
                    "%rights%", String.valueOf(member.rights().size()));
            ItemStack head = item(cfg, "members.item", Material.PLAYER_HEAD, "&e%player%",
                    List.of("&7%role%", "&7Rights: &f%rights%", "&7Click to manage rights"), values);
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.uuid())); head.setItemMeta(meta);
            }
            inv.setItem(targetSlot, head);
        }
        int back = slot(cfg, "members.back", 49, size);
        inv.setItem(back, item(cfg, "members.back", Material.ARROW, "&fBack", List.of("&7Return to main menu")));
        player.openInventory(inv);
    }

    private void openRights(Player player, UUID memberId) {
        TeamData team = store.byMember(player.getUniqueId());
        if (team == null) { open(player); return; }
        MemberData member = team.member(memberId);
        if (member == null) { openMembers(player); return; }
        if (member.uuid().equals(team.owner())) { message(player, "The owner's rights are always enabled and cannot be edited."); openMembers(player); return; }
        if (!canManage(player, team)) { message(player, "You need MANAGE_MEMBERS to edit rights."); openMembers(player); return; }
        YamlConfiguration cfg = gui();
        int size = size(cfg, "rights", 54);
        Inventory inv = Bukkit.createInventory(null, size, title(cfg, "rights", "&3Member Rights"));
        int memberSlot = cfg.getInt("rights.member-slot", 4);
        if (memberSlot < 0 || memberSlot >= size) memberSlot = 4;
        ItemStack head = item(cfg, "rights.member", Material.PLAYER_HEAD, "&e%player%", List.of("&7Click a right to toggle it."), Map.of("%player%", member.lastKnownName()));
        if (head.getItemMeta() instanceof SkullMeta meta) { meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.uuid())); head.setItemMeta(meta); }
        inv.setItem(memberSlot, head);
        List<Integer> slots = cfg.getIntegerList("rights.slots");
        if (slots.isEmpty()) { slots = new ArrayList<>(); for (int i=9;i<Math.min(54, size);i++) slots.add(i); }
        TeamRight[] rights = TeamRight.values();
        for (int i=0; i<rights.length && i<slots.size(); i++) {
            int targetSlot = slots.get(i);
            if (targetSlot < 0 || targetSlot >= size) continue;
            TeamRight right = rights[i];
            boolean enabled = member.has(right);
            Material fallback = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
            Material type = material(cfg, "rights.item." + (enabled ? "enabled-material" : "disabled-material"), fallback);
            String name = cfg.getString("rights.item." + (enabled ? "enabled-name" : "disabled-name"), (enabled ? "&a" : "&c") + "%right%").replace("%right%", right.name());
            String status = enabled ? "&aGranted" : "&cDenied";
            ItemStack icon = item(cfg, "rights.item", type, name, List.of("&7Status: %status%", "&7Click to toggle"), Map.of("%right%", right.name(), "%status%", color(status)));
            inv.setItem(targetSlot, icon);
        }
        inv.setItem(slot(cfg, "rights.back", 49, size), item(cfg, "rights.back", Material.ARROW, "&fBack to members", List.of("&7Return to member list")));
        player.openInventory(inv);
    }

    @EventHandler public void closeChest(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TeamChestHolder holder)) return;
        TeamData team = store.byId(holder.teamId());
        if (team == null) return;
        team.enderChestContents(event.getInventory().getContents());
        store.save();
    }

    @EventHandler public void click(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TeamChestHolder) return;
        YamlConfiguration cfg = gui();
        String title = event.getView().getTitle();
        String mainTitle = title(cfg, "main", "&3SuperTeams");
        String membersTitle = title(cfg, "members", "&3Team Members");
        String rightsTitle = title(cfg, "rights", "&3Member Rights");
        if (!mainTitle.equals(title) && !membersTitle.equals(title) && !rightsTitle.equals(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;
        int clickedSlot = event.getRawSlot();
        if (mainTitle.equals(title)) {
            if (clickedSlot == slot(cfg, "main.items.members", 10, event.getView().getTopInventory().getSize())) openMembers(player);
            else if (clickedSlot == slot(cfg, "main.items.home", 12, event.getView().getTopInventory().getSize())) player.performCommand("team home");
            else if (clickedSlot == slot(cfg, "main.items.set-home", 14, event.getView().getTopInventory().getSize())) player.performCommand("team sethome");
            else if (clickedSlot == slot(cfg, "main.items.friendly-fire", 16, event.getView().getTopInventory().getSize())) player.performCommand("team pvp");
            else if (clickedSlot == slot(cfg, "main.items.bank", 20, event.getView().getTopInventory().getSize())) player.performCommand("team bank balance");
            else if (clickedSlot == slot(cfg, "main.items.close", 22, event.getView().getTopInventory().getSize())) player.closeInventory();
            else if (clickedSlot == slot(cfg, "main.items.ender-chest", 24, event.getView().getTopInventory().getSize())) player.performCommand("team enderchest");
            return;
        }
        TeamData team = store.byMember(player.getUniqueId());
        if (team == null) { player.closeInventory(); return; }
        if (membersTitle.equals(title)) {
            if (clickedSlot == slot(cfg, "members.back", 49, event.getView().getTopInventory().getSize())) { open(player); return; }
            int index = memberSlots(cfg).indexOf(clickedSlot);
            if (index < 0) return;
            List<MemberData> members = new ArrayList<>(team.members().values());
            if (index >= members.size()) return;
            openRights(player, members.get(index).uuid());
            return;
        }
        if (rightsTitle.equals(title)) {
            if (clickedSlot == slot(cfg, "rights.back", 49, event.getView().getTopInventory().getSize())) { openMembers(player); return; }
            if (!canManage(player, team)) { message(player, "You need MANAGE_MEMBERS to edit rights."); openMembers(player); return; }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            List<Integer> slots = cfg.getIntegerList("rights.slots");
            if (slots.isEmpty()) { slots = new ArrayList<>(); for (int i=9;i<Math.min(54, event.getView().getTopInventory().getSize());i++) slots.add(i); }
            int index = slots.indexOf(clickedSlot);
            TeamRight[] rights = TeamRight.values();
            if (index < 0 || index >= rights.length) return;
            int configuredMemberSlot = cfg.getInt("rights.member-slot", 4);
            if (configuredMemberSlot < 0 || configuredMemberSlot >= event.getView().getTopInventory().getSize()) configuredMemberSlot = 4;
            ItemStack targetHead = event.getView().getTopInventory().getItem(configuredMemberSlot);
            if (targetHead == null || !targetHead.hasItemMeta()) return;
            String targetName = ChatColor.stripColor(targetHead.getItemMeta().getDisplayName());
            MemberData target = team.members().values().stream().filter(member -> member.lastKnownName().equals(targetName)).findFirst().orElse(null);
            if (target == null || target.uuid().equals(team.owner())) { message(player, "Cannot edit this member."); openMembers(player); return; }
            TeamRight right = rights[index];
            if (right == TeamRight.MANAGE_MEMBERS && !team.owner().equals(player.getUniqueId())) { message(player, "Only the owner may delegate MANAGE_MEMBERS."); return; }
            if (target.has(right)) target.rights().remove(right); else target.rights().add(right);
            store.save();
            openRights(player, target.uuid());
        }
    }
}
