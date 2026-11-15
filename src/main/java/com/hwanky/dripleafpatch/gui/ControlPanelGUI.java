package com.hwanky.dripleafpatch.gui;

import com.hwanky.dripleafpatch.DripleafPatchPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ControlPanelGUI implements Listener {

    private final DripleafPatchPlugin plugin;

    public ControlPanelGUI(DripleafPatchPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        PanelHolder holder = new PanelHolder();
        Inventory inv = Bukkit.createInventory(holder, 36, plugin.getMiniMessage().deserialize("<dark_gray>DripleafPatch Control Panel"));
        holder.setInventory(inv);

        populateItems(inv);
        player.openInventory(inv);
    }

    private void populateItems(Inventory inv) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, "<dark_gray> ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(10, createToggleItem(
                Material.SMALL_DRIPLEAF,
                "Dripleaf Dupe Fix",
                plugin.isDripleafFixEnabled(),
                List.of("Blocks bone meal on floating", "top-half Small Dripleaf blocks.")
        ));

        inv.setItem(11, createModeItem(
                Material.SHEARS,
                "Dripleaf Cleanup Mode",
                plugin.isDripleafCleanupIllegalBlocks() ? plugin.getDripleafCleanupMode() : "OFF",
                List.of("Handles floating dripleaf blocks.", "Click to cycle: NATURAL -> DESTROY -> OFF")
        ));

        inv.setItem(12, createToggleItem(
                Material.SHULKER_BOX,
                "Shulker Nesting Master",
                plugin.isShulkerFixEnabled(),
                List.of("Master toggle for all Shulker", "anti-nesting protection layers.")
        ));

        inv.setItem(13, createToggleItem(
                Material.CHEST,
                "Block Inventory Nesting",
                plugin.isShulkerBlockInventoryClick(),
                List.of("Blocks clicking, shift-clicking,", "hotbar & offhand swaps into Shulkers.")
        ));

        inv.setItem(14, createToggleItem(
                Material.HOPPER,
                "Block Hopper Transfer",
                plugin.isShulkerBlockHopperTransfer(),
                List.of("Prevents Hoppers and Droppers", "from pushing Shulkers into Shulkers.")
        ));

        inv.setItem(15, createToggleItem(
                Material.DISPENSER,
                "Block Dispenser Nesting",
                plugin.isShulkerBlockDispenserDispense(),
                List.of("Prevents Dispensers from shooting", "Shulkers into adjacent containers.")
        ));

        inv.setItem(16, createModeItem(
                Material.ANVIL,
                "Sanitizer Purge Action",
                plugin.getShulkerPurgeAction(),
                List.of("Action when nested Shulker is found.", "Click to cycle: DROP <-> DESTROY")
        ));

        inv.setItem(19, createToggleItem(
                Material.ENDER_CHEST,
                "Sanitize on Open",
                plugin.isShulkerSanitizeOnOpen(),
                List.of("Cleans nested Shulkers when", "a container is opened.")
        ));

        inv.setItem(20, createToggleItem(
                Material.PISTON,
                "Sanitize on Place",
                plugin.isShulkerSanitizeOnPlace(),
                List.of("Blocks and cleans corrupted", "Shulkers when placed down.")
        ));

        inv.setItem(21, createToggleItem(
                Material.ITEM_FRAME,
                "Sanitize on Pickup",
                plugin.isShulkerSanitizeOnPickup(),
                List.of("Inspects and cleans Shulkers", "picked up from the ground.")
        ));

        inv.setItem(23, createToggleItem(
                Material.BELL,
                "Staff Alert Broadcast",
                plugin.isNotifyAdmins(),
                List.of("Sends real-time chat alerts", "to staff with notify permission.")
        ));

        inv.setItem(24, createToggleItem(
                Material.NOTE_BLOCK,
                "Alert Sound Effect",
                plugin.isAlertSoundEnabled(),
                List.of("Plays warning sound to staff", "when an exploit is intercepted.")
        ));

        inv.setItem(25, createItem(
                Material.CLOCK,
                "<aqua>Reload Configuration",
                List.of("<gray>Click to reload config.yml", "<gray>and refresh active settings.")
        ));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPanelClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PanelHolder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("dripleafpatch.panel")) {
            player.closeInventory();
            player.sendMessage(plugin.getMessage("no-permission"));
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        boolean updated = true;
        switch (slot) {
            case 10 -> plugin.updateAndSaveSetting("dripleaf-fix.enabled", !plugin.isDripleafFixEnabled());
            case 11 -> cycleDripleafCleanup();
            case 12 -> plugin.updateAndSaveSetting("shulker-nesting-fix.enabled", !plugin.isShulkerFixEnabled());
            case 13 -> plugin.updateAndSaveSetting("shulker-nesting-fix.block-inventory-click", !plugin.isShulkerBlockInventoryClick());
            case 14 -> plugin.updateAndSaveSetting("shulker-nesting-fix.block-hopper-transfer", !plugin.isShulkerBlockHopperTransfer());
            case 15 -> plugin.updateAndSaveSetting("shulker-nesting-fix.block-dispenser-dispense", !plugin.isShulkerBlockDispenserDispense());
            case 16 -> {
                String next = "DROP".equals(plugin.getShulkerPurgeAction()) ? "DESTROY" : "DROP";
                plugin.updateAndSaveSetting("shulker-nesting-fix.purge-action", next);
            }
            case 19 -> plugin.updateAndSaveSetting("shulker-nesting-fix.sanitize-on-open", !plugin.isShulkerSanitizeOnOpen());
            case 20 -> plugin.updateAndSaveSetting("shulker-nesting-fix.sanitize-on-place", !plugin.isShulkerSanitizeOnPlace());
            case 21 -> plugin.updateAndSaveSetting("shulker-nesting-fix.sanitize-on-pickup", !plugin.isShulkerSanitizeOnPickup());
            case 23 -> plugin.updateAndSaveSetting("notify-admins", !plugin.isNotifyAdmins());
            case 24 -> plugin.updateAndSaveSetting("alert-sound.enabled", !plugin.isAlertSoundEnabled());
            case 25 -> {
                long start = System.currentTimeMillis();
                plugin.loadConfiguration();
                long elapsed = System.currentTimeMillis() - start;
                player.sendMessage(plugin.getMessage("reload-success", Map.of("time", String.valueOf(elapsed))));
            }
            default -> updated = false;
        }

        if (updated) {
            plugin.playGuiSound(player);
            populateItems(event.getInventory());
        }
    }

    private void cycleDripleafCleanup() {
        if (!plugin.isDripleafCleanupIllegalBlocks()) {
            plugin.getConfig().set("dripleaf-fix.cleanup-illegal-blocks", true);
            plugin.updateAndSaveSetting("dripleaf-fix.cleanup-mode", "NATURAL");
        } else if ("NATURAL".equals(plugin.getDripleafCleanupMode())) {
            plugin.updateAndSaveSetting("dripleaf-fix.cleanup-mode", "DESTROY");
        } else {
            plugin.updateAndSaveSetting("dripleaf-fix.cleanup-illegal-blocks", false);
        }
    }

    private ItemStack createToggleItem(Material material, String name, boolean state, List<String> desc) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String line : desc) {
            lore.add("<gray>" + line);
        }
        lore.add("");
        lore.add("<gray>Status: " + (state ? "<green><b>ENABLED</b>" : "<red><b>DISABLED</b>"));
        lore.add("<yellow>Click to toggle");

        String prefixColor = state ? "<green>" : "<red>";
        return createItem(material, prefixColor + name, lore);
    }

    private ItemStack createModeItem(Material material, String name, String mode, List<String> desc) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String line : desc) {
            lore.add("<gray>" + line);
        }
        lore.add("");
        lore.add("<gray>Current Mode: <aqua><b>" + mode + "</b>");
        lore.add("<yellow>Click to switch mode");

        return createItem(material, "<aqua>" + name, lore);
    }

    private ItemStack createItem(Material material, String displayName, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMiniMessage().deserialize(displayName).decoration(TextDecoration.ITALIC, false));
            if (!loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>(loreLines.size());
                for (String line : loreLines) {
                    lore.add(plugin.getMiniMessage().deserialize(line).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static final class PanelHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }
}