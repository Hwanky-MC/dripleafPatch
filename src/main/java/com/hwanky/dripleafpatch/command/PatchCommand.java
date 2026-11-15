package com.hwanky.dripleafpatch.command;

import com.hwanky.dripleafpatch.DripleafPatchPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PatchCommand implements CommandExecutor, TabCompleter {

    private final DripleafPatchPlugin plugin;

    private static final Map<String, String> TOGGLE_PATHS = Map.ofEntries(
            Map.entry("dripleaf", "dripleaf-fix.enabled"),
            Map.entry("dripleaf-cleanup", "dripleaf-fix.cleanup-illegal-blocks"),
            Map.entry("shulker", "shulker-nesting-fix.enabled"),
            Map.entry("shulker-click", "shulker-nesting-fix.block-inventory-click"),
            Map.entry("shulker-hopper", "shulker-nesting-fix.block-hopper-transfer"),
            Map.entry("shulker-dispenser", "shulker-nesting-fix.block-dispenser-dispense"),
            Map.entry("sanitize-open", "shulker-nesting-fix.sanitize-on-open"),
            Map.entry("sanitize-place", "shulker-nesting-fix.sanitize-on-place"),
            Map.entry("sanitize-pickup", "shulker-nesting-fix.sanitize-on-pickup"),
            Map.entry("alerts", "notify-admins"),
            Map.entry("alert-sound", "alert-sound.enabled"),
            Map.entry("log-attempts", "log-attempts")
    );

    public PatchCommand(DripleafPatchPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dripleafpatch.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "panel", "gui", "menu" -> {
                if (!sender.hasPermission("dripleafpatch.panel")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getMessage("player-only"));
                    return true;
                }
                plugin.getControlPanelGUI().open(player);
            }
            case "toggle" -> {
                if (!sender.hasPermission("dripleafpatch.toggle")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                handleToggle(sender, label, args);
            }
            case "set" -> {
                if (!sender.hasPermission("dripleafpatch.set")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                handleSet(sender, label, args);
            }
            case "status" -> {
                if (!sender.hasPermission("dripleafpatch.status")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                sendStatus(sender);
            }
            case "reload" -> {
                if (!sender.hasPermission("dripleafpatch.reload")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                long start = System.currentTimeMillis();
                plugin.loadConfiguration();
                long elapsed = System.currentTimeMillis() - start;
                sender.sendMessage(plugin.getMessage("reload-success", Map.of("time", String.valueOf(elapsed))));
            }
            default -> sendHelp(sender, label);
        }

        return true;
    }

    private void handleToggle(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("invalid-argument", Map.of("usage", "/" + label + " toggle <module> [on|off]")));
            return;
        }

        String moduleKey = args[1].toLowerCase(Locale.ROOT);
        String configPath = TOGGLE_PATHS.get(moduleKey);
        if (configPath == null) {
            sender.sendMessage(plugin.getMessage("invalid-argument", Map.of("usage", "/" + label + " toggle <module> [on|off]")));
            return;
        }

        boolean newState;
        if (args.length >= 3) {
            String val = args[2].toLowerCase(Locale.ROOT);
            if (val.equals("on") || val.equals("true") || val.equals("enable")) {
                newState = true;
            } else if (val.equals("off") || val.equals("false") || val.equals("disable")) {
                newState = false;
            } else {
                sender.sendMessage(plugin.getMessage("invalid-argument", Map.of("usage", "/" + label + " toggle " + moduleKey + " [on|off]")));
                return;
            }
        } else {
            newState = !plugin.getConfig().getBoolean(configPath, true);
        }

        plugin.updateAndSaveSetting(configPath, newState);
        String statusStr = newState ? "<green>ENABLED</green>" : "<red>DISABLED</red>";
        sender.sendMessage(plugin.getMessage("module-toggled", Map.of("module", moduleKey, "status", statusStr)));
    }

    private void handleSet(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessage("invalid-argument", Map.of("usage", "/" + label + " set <cleanup-mode|purge-action> <value>")));
            return;
        }

        String setting = args[1].toLowerCase(Locale.ROOT);
        String value = args[2].toUpperCase(Locale.ROOT);

        if (setting.equals("cleanup-mode")) {
            if (!value.equals("NATURAL") && !value.equals("DESTROY")) {
                sender.sendMessage(plugin.getMessage("invalid-argument", Map.of("usage", "/" + label + " set cleanup-mode <NATURAL|DESTROY>")));
                return;
            }
            plugin.updateAndSaveSetting("dripleaf-fix.cleanup-mode", value);
            sender.sendMessage(plugin.getMessage("setting-updated", Map.of("setting", "cleanup-mode", "value", value)));
        } else if (setting.equals("purge-action")) {
            if (!value.equals("DROP") && !value.equals("DESTROY")) {
                sender.sendMessage(plugin.getMessage("invalid-argument", Map.of("usage", "/" + label + " set purge-action <DROP|DESTROY>")));
                return;
            }
            plugin.updateAndSaveSetting("shulker-nesting-fix.purge-action", value);
            sender.sendMessage(plugin.getMessage("setting-updated", Map.of("setting", "purge-action", "value", value)));
        } else {
            sender.sendMessage(plugin.getMessage("invalid-argument", Map.of("usage", "/" + label + " set <cleanup-mode|purge-action> <value>")));
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(plugin.getMiniMessage().deserialize("<gradient:#00ff88:#00b4d8><b>DripleafPatch v" + plugin.getDescription().getVersion() + "</b></gradient> <dark_gray>-</dark_gray> <gray>by hwanky</gray>"));
        sender.sendMessage(plugin.getMiniMessage().deserialize("<yellow>/" + label + " panel <gray>- Open interactive module control GUI"));
        sender.sendMessage(plugin.getMiniMessage().deserialize("<yellow>/" + label + " toggle <module> [on|off] <gray>- Toggle a protection module"));
        sender.sendMessage(plugin.getMiniMessage().deserialize("<yellow>/" + label + " set <setting> <value> <gray>- Change module behavior mode"));
        sender.sendMessage(plugin.getMiniMessage().deserialize("<yellow>/" + label + " status <gray>- View status of all modules"));
        sender.sendMessage(plugin.getMiniMessage().deserialize("<yellow>/" + label + " reload <gray>- Reload config.yml from disk"));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(plugin.getMiniMessage().deserialize("<gradient:#00ff88:#00b4d8><b>DripleafPatch Status</b></gradient>"));
        sender.sendMessage(formatStatusLine("Dripleaf Dupe Fix", plugin.isDripleafFixEnabled()));
        sender.sendMessage(formatStatusLine("Dripleaf Cleanup (" + plugin.getDripleafCleanupMode() + ")", plugin.isDripleafCleanupIllegalBlocks()));
        sender.sendMessage(formatStatusLine("Shulker Master Guard", plugin.isShulkerFixEnabled()));
        sender.sendMessage(formatStatusLine("  Click & Swap Block", plugin.isShulkerBlockInventoryClick()));
        sender.sendMessage(formatStatusLine("  Hopper Transfer Block", plugin.isShulkerBlockHopperTransfer()));
        sender.sendMessage(formatStatusLine("  Dispenser Shoot Block", plugin.isShulkerBlockDispenserDispense()));
        sender.sendMessage(formatStatusLine("  Sanitizers (" + plugin.getShulkerPurgeAction() + ")", plugin.isShulkerSanitizeOnOpen() || plugin.isShulkerSanitizeOnPlace() || plugin.isShulkerSanitizeOnPickup()));
        sender.sendMessage(formatStatusLine("Staff Alerts", plugin.isNotifyAdmins()));
    }

    private net.kyori.adventure.text.Component formatStatusLine(String label, boolean enabled) {
        String badge = enabled ? "<green>ON</green>" : "<red>OFF</red>";
        return plugin.getMiniMessage().deserialize("<gray> - " + label + ": " + badge);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("dripleafpatch.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterPrefix(List.of("panel", "toggle", "set", "status", "reload", "help"), args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (sub.equals("toggle")) {
                return filterPrefix(new ArrayList<>(TOGGLE_PATHS.keySet()), args[1]);
            }
            if (sub.equals("set")) {
                return filterPrefix(List.of("cleanup-mode", "purge-action"), args[1]);
            }
        }

        if (args.length == 3) {
            if (sub.equals("toggle")) {
                return filterPrefix(List.of("on", "off"), args[2]);
            }
            if (sub.equals("set")) {
                String setting = args[1].toLowerCase(Locale.ROOT);
                if (setting.equals("cleanup-mode")) {
                    return filterPrefix(List.of("NATURAL", "DESTROY"), args[2]);
                }
                if (setting.equals("purge-action")) {
                    return filterPrefix(List.of("DROP", "DESTROY"), args[2]);
                }
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}