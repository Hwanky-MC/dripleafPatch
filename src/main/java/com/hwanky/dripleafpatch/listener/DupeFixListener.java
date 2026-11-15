package com.hwanky.dripleafpatch.listener;

import com.hwanky.dripleafpatch.DripleafPatchPlugin;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.Map;

public final class DupeFixListener implements Listener {

    private final DripleafPatchPlugin plugin;

    public DupeFixListener(DripleafPatchPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteractDripleaf(PlayerInteractEvent event) {
        if (!plugin.isDripleafFixEnabled() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BONE_MEAL) {
            return;
        }

        Block targetDripleaf = null;
        if (clicked.getType() == Material.SMALL_DRIPLEAF) {
            targetDripleaf = clicked;
        } else {
            Block above = clicked.getRelative(BlockFace.UP);
            if (above.getType() == Material.SMALL_DRIPLEAF) {
                targetDripleaf = above;
            }
        }

        if (targetDripleaf != null && isIllegalSmallDripleaf(targetDripleaf)) {
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);

            cleanupIllegalDripleaf(targetDripleaf);

            plugin.broadcastAlert("dripleaf-dupe-prevented", Map.of(
                    "player", event.getPlayer().getName(),
                    "location", plugin.formatLocation(targetDripleaf.getLocation())
            ));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockFertilizeDripleaf(BlockFertilizeEvent event) {
        if (!plugin.isDripleafFixEnabled()) {
            return;
        }

        Block block = event.getBlock();
        if (block.getType() == Material.SMALL_DRIPLEAF && isIllegalSmallDripleaf(block)) {
            event.setCancelled(true);
            cleanupIllegalDripleaf(block);

            if (event.getPlayer() != null) {
                plugin.broadcastAlert("dripleaf-dupe-prevented", Map.of(
                        "player", event.getPlayer().getName(),
                        "location", plugin.formatLocation(block.getLocation())
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onStructureGrow(StructureGrowEvent event) {
        if (!plugin.isDripleafFixEnabled()) {
            return;
        }

        Block base = event.getLocation().getBlock();
        if (base.getType() == Material.SMALL_DRIPLEAF && isIllegalSmallDripleaf(base)) {
            event.setCancelled(true);
            cleanupIllegalDripleaf(base);
        }
    }

    private boolean isIllegalSmallDripleaf(Block block) {
        if (block.getType() != Material.SMALL_DRIPLEAF) {
            return false;
        }

        if (block.getBlockData() instanceof Bisected bisected) {
            if (bisected.getHalf() == Bisected.Half.TOP) {
                Block below = block.getRelative(BlockFace.DOWN);
                if (below.getType() != Material.SMALL_DRIPLEAF) {
                    return true;
                }
                return below.getBlockData() instanceof Bisected belowBisected
                        && belowBisected.getHalf() != Bisected.Half.BOTTOM;
            } else {
                Block above = block.getRelative(BlockFace.UP);
                if (above.getType() != Material.SMALL_DRIPLEAF) {
                    return true;
                }
                return above.getBlockData() instanceof Bisected aboveBisected
                        && aboveBisected.getHalf() != Bisected.Half.TOP;
            }
        }
        return false;
    }

    private void cleanupIllegalDripleaf(Block block) {
        if (!plugin.isDripleafCleanupIllegalBlocks()) {
            return;
        }
        if ("DESTROY".equals(plugin.getDripleafCleanupMode())) {
            block.setType(Material.AIR, false);
        } else {
            block.breakNaturally();
        }
    }

    private boolean isShulkerBox(Material material) {
        return material != null && Tag.SHULKER_BOXES.isTagged(material);
    }

    private boolean isShulkerBox(ItemStack item) {
        return item != null && isShulkerBox(item.getType());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryClickShulker(InventoryClickEvent event) {
        if (!plugin.isShulkerFixEnabled() || !plugin.isShulkerBlockInventoryClick()) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (top.getType() != InventoryType.SHULKER_BOX) {
            return;
        }

        int rawSlot = event.getRawSlot();
        int topSize = top.getSize();
        boolean insideShulker = rawSlot >= 0 && rawSlot < topSize;

        if (insideShulker && isShulkerBox(event.getCursor())) {
            cancelShulkerAction(event, "Cursor-Placement");
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && rawSlot >= topSize && isShulkerBox(event.getCurrentItem())) {
            cancelShulkerAction(event, "Shift-Click");
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY && insideShulker) {
            int btn = event.getHotbarButton();
            if (btn >= 0 && btn < 9 && isShulkerBox(event.getWhoClicked().getInventory().getItem(btn))) {
                cancelShulkerAction(event, "Hotbar-Swap");
                return;
            }
        }

        if (event.getClick() == ClickType.SWAP_OFFHAND && insideShulker && isShulkerBox(event.getWhoClicked().getInventory().getItemInOffHand())) {
            cancelShulkerAction(event, "Offhand-Swap");
            return;
        }

        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR && isShulkerBox(event.getCursor())) {
            cancelShulkerAction(event, "Collect-To-Cursor");
        }
    }

    private void cancelShulkerAction(InventoryClickEvent event, String method) {
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        if (event.getWhoClicked() instanceof Player player) {
            player.updateInventory();
            plugin.broadcastAlert("shulker-nesting-prevented", Map.of(
                    "player", player.getName(),
                    "method", method,
                    "location", plugin.formatLocation(player.getLocation())
            ));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryDragShulker(InventoryDragEvent event) {
        if (!plugin.isShulkerFixEnabled() || !plugin.isShulkerBlockInventoryClick()) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (top.getType() != InventoryType.SHULKER_BOX || !isShulkerBox(event.getOldCursor())) {
            return;
        }

        int topSize = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                event.setResult(Event.Result.DENY);
                if (event.getWhoClicked() instanceof Player player) {
                    player.updateInventory();
                }
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!plugin.isShulkerFixEnabled() || !plugin.isShulkerBlockHopperTransfer()) {
            return;
        }

        if (event.getDestination().getType() == InventoryType.SHULKER_BOX && isShulkerBox(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (!plugin.isShulkerFixEnabled() || !plugin.isShulkerBlockDispenserDispense() || !isShulkerBox(event.getItem())) {
            return;
        }

        Block block = event.getBlock();
        if (block.getBlockData() instanceof Directional directional) {
            Material targetType = block.getRelative(directional.getFacing()).getType();
            if (isShulkerBox(targetType) || targetType == Material.BARREL) {
                event.setCancelled(true);
                plugin.broadcastAlert("dispenser-nesting-prevented", Map.of(
                        "location", plugin.formatLocation(block.getLocation())
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!plugin.isShulkerFixEnabled() || !plugin.isShulkerSanitizeOnOpen()) {
            return;
        }

        Inventory inv = event.getInventory();
        if (inv.getType() != InventoryType.SHULKER_BOX) {
            return;
        }

        Player player = (Player) event.getPlayer();
        if (sanitizeInventory(inv, player)) {
            plugin.broadcastAlert("shulker-purged", Map.of(
                    "player", player.getName(),
                    "method", plugin.getShulkerPurgeAction(),
                    "location", plugin.formatLocation(player.getLocation())
            ));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.isShulkerFixEnabled() || !plugin.isShulkerSanitizeOnPlace()) {
            return;
        }

        Block placed = event.getBlockPlaced();
        if (!isShulkerBox(placed.getType())) {
            return;
        }

        ItemStack item = event.getItemInHand();
        if (item.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof ShulkerBox boxState) {
            Player player = event.getPlayer();
            if (sanitizeInventory(boxState.getInventory(), player)) {
                event.setCancelled(true);
                meta.setBlockState(boxState);
                item.setItemMeta(meta);

                plugin.broadcastAlert("shulker-purged", Map.of(
                        "player", player.getName(),
                        "method", "Block-Place",
                        "location", plugin.formatLocation(placed.getLocation())
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!plugin.isShulkerFixEnabled() || !plugin.isShulkerSanitizeOnPickup()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getItem().getItemStack();
        if (isShulkerBox(item) && item.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof ShulkerBox boxState) {
            if (sanitizeInventory(boxState.getInventory(), player)) {
                meta.setBlockState(boxState);
                item.setItemMeta(meta);
                event.getItem().setItemStack(item);

                plugin.broadcastAlert("shulker-purged", Map.of(
                        "player", player.getName(),
                        "method", "Ground-Pickup",
                        "location", plugin.formatLocation(player.getLocation())
                ));
            }
        }
    }

    private boolean sanitizeInventory(Inventory inventory, Player player) {
        boolean modified = false;
        ItemStack[] contents = inventory.getContents();
        boolean dropMode = "DROP".equals(plugin.getShulkerPurgeAction());

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isShulkerBox(item)) {
                modified = true;
                if (dropMode && player != null && player.isValid()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                }
                inventory.setItem(i, null);
            }
        }
        return modified;
    }
}