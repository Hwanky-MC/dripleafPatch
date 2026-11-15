package com.hwanky.dripleafpatch;

import com.hwanky.dripleafpatch.command.PatchCommand;
import com.hwanky.dripleafpatch.gui.ControlPanelGUI;
import com.hwanky.dripleafpatch.listener.DupeFixListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DripleafPatchPlugin extends JavaPlugin {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> cachedMessages = new HashMap<>();

    private ControlPanelGUI controlPanelGUI;

    private boolean logAttempts;
    private boolean notifyAdmins;
    private boolean alertSoundEnabled;
    private Sound alertSound;
    private float alertSoundVolume;
    private float alertSoundPitch;

    private boolean guiSoundEnabled;
    private Sound guiToggleSound;
    private float guiSoundVolume;
    private float guiSoundPitch;

    private boolean dripleafFixEnabled;
    private boolean dripleafCleanupIllegalBlocks;
    private String dripleafCleanupMode;

    private boolean shulkerFixEnabled;
    private boolean shulkerBlockInventoryClick;
    private boolean shulkerBlockHopperTransfer;
    private boolean shulkerBlockDispenserDispense;
    private boolean shulkerSanitizeOnOpen;
    private boolean shulkerSanitizeOnPlace;
    private boolean shulkerSanitizeOnPickup;
    private String shulkerPurgeAction;

    private String messagePrefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();

        this.controlPanelGUI = new ControlPanelGUI(this);

        getServer().getPluginManager().registerEvents(new DupeFixListener(this), this);
        getServer().getPluginManager().registerEvents(controlPanelGUI, this);

        PluginCommand cmd = getCommand("dripleafpatch");
        if (cmd != null) {
            PatchCommand executor = new PatchCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("DripleafPatch v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("DripleafPatch disabled.");
    }

    public void loadConfiguration() {
        reloadConfig();

        this.logAttempts = getConfig().getBoolean("log-attempts", true);
        this.notifyAdmins = getConfig().getBoolean("notify-admins", true);

        this.alertSoundEnabled = getConfig().getBoolean("alert-sound.enabled", true);
        this.alertSound = parseSound(getConfig().getString("alert-sound.sound", "BLOCK_NOTE_BLOCK_BASS"), Sound.BLOCK_NOTE_BLOCK_BASS);
        this.alertSoundVolume = (float) getConfig().getDouble("alert-sound.volume", 1.0);
        this.alertSoundPitch = (float) getConfig().getDouble("alert-sound.pitch", 0.6);

        this.guiSoundEnabled = getConfig().getBoolean("gui-sound.enabled", true);
        this.guiToggleSound = parseSound(getConfig().getString("gui-sound.toggle-sound", "UI_BUTTON_CLICK"), Sound.UI_BUTTON_CLICK);
        this.guiSoundVolume = (float) getConfig().getDouble("gui-sound.volume", 0.8);
        this.guiSoundPitch = (float) getConfig().getDouble("gui-sound.pitch", 1.2);

        this.dripleafFixEnabled = getConfig().getBoolean("dripleaf-fix.enabled", true);
        this.dripleafCleanupIllegalBlocks = getConfig().getBoolean("dripleaf-fix.cleanup-illegal-blocks", true);
        this.dripleafCleanupMode = getConfig().getString("dripleaf-fix.cleanup-mode", "NATURAL").toUpperCase(Locale.ROOT);

        this.shulkerFixEnabled = getConfig().getBoolean("shulker-nesting-fix.enabled", true);
        this.shulkerBlockInventoryClick = getConfig().getBoolean("shulker-nesting-fix.block-inventory-click", true);
        this.shulkerBlockHopperTransfer = getConfig().getBoolean("shulker-nesting-fix.block-hopper-transfer", true);
        this.shulkerBlockDispenserDispense = getConfig().getBoolean("shulker-nesting-fix.block-dispenser-dispense", true);
        this.shulkerSanitizeOnOpen = getConfig().getBoolean("shulker-nesting-fix.sanitize-on-open", true);
        this.shulkerSanitizeOnPlace = getConfig().getBoolean("shulker-nesting-fix.sanitize-on-place", true);
        this.shulkerSanitizeOnPickup = getConfig().getBoolean("shulker-nesting-fix.sanitize-on-pickup", true);
        this.shulkerPurgeAction = getConfig().getString("shulker-nesting-fix.purge-action", "DROP").toUpperCase(Locale.ROOT);

        this.messagePrefix = getConfig().getString("messages.prefix", "<gradient:#00ff88:#00b4d8><b>DripleafPatch</b></gradient> <dark_gray>|</dark_gray> ");
        cachedMessages.clear();
        if (getConfig().isConfigurationSection("messages")) {
            for (String key : getConfig().getConfigurationSection("messages").getKeys(false)) {
                cachedMessages.put(key, getConfig().getString("messages." + key));
            }
        }
    }

    public void updateAndSaveSetting(String path, Object value) {
        getConfig().set(path, value);
        saveConfig();
        loadConfiguration();
    }

    private Sound parseSound(String name, Sound fallback) {
        if (name == null || name.equalsIgnoreCase("NONE")) {
            return null;
        }
        try {
            return Sound.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid sound in config: '" + name + "'. Using fallback sound.");
            return fallback;
        }
    }

    public void broadcastAlert(String messageKey, Map<String, String> placeholders) {
        String template = cachedMessages.get(messageKey);
        if (template == null || template.isEmpty()) {
            return;
        }

        Component component = formatMessage(template, placeholders);

        if (logAttempts) {
            getLogger().warning(PlainTextComponentSerializer.plainText().serialize(component));
        }

        if (notifyAdmins) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("dripleafpatch.notify")) {
                    player.sendMessage(component);
                    if (alertSoundEnabled && alertSound != null) {
                        player.playSound(player.getLocation(), alertSound, alertSoundVolume, alertSoundPitch);
                    }
                }
            }
        }
    }

    public Component getMessage(String key) {
        return formatMessage(cachedMessages.getOrDefault(key, ""), null);
    }

    public Component getMessage(String key, Map<String, String> placeholders) {
        return formatMessage(cachedMessages.getOrDefault(key, ""), placeholders);
    }

    public Component formatMessage(String template, Map<String, String> placeholders) {
        String processed = template.replace("<prefix>", messagePrefix);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                processed = processed.replace("<" + entry.getKey() + ">", entry.getValue());
            }
        }
        return miniMessage.deserialize(processed);
    }

    public void playGuiSound(Player player) {
        if (guiSoundEnabled && guiToggleSound != null) {
            player.playSound(player.getLocation(), guiToggleSound, guiSoundVolume, guiSoundPitch);
        }
    }

    public String formatLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return "unknown";
        }
        return loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public ControlPanelGUI getControlPanelGUI() {
        return controlPanelGUI;
    }

    public boolean isLogAttempts() { return logAttempts; }
    public boolean isNotifyAdmins() { return notifyAdmins; }
    public boolean isAlertSoundEnabled() { return alertSoundEnabled; }
    public boolean isDripleafFixEnabled() { return dripleafFixEnabled; }
    public boolean isDripleafCleanupIllegalBlocks() { return dripleafCleanupIllegalBlocks; }
    public String getDripleafCleanupMode() { return dripleafCleanupMode; }
    public boolean isShulkerFixEnabled() { return shulkerFixEnabled; }
    public boolean isShulkerBlockInventoryClick() { return shulkerBlockInventoryClick; }
    public boolean isShulkerBlockHopperTransfer() { return shulkerBlockHopperTransfer; }
    public boolean isShulkerBlockDispenserDispense() { return shulkerBlockDispenserDispense; }
    public boolean isShulkerSanitizeOnOpen() { return shulkerSanitizeOnOpen; }
    public boolean isShulkerSanitizeOnPlace() { return shulkerSanitizeOnPlace; }
    public boolean isShulkerSanitizeOnPickup() { return shulkerSanitizeOnPickup; }
    public String getShulkerPurgeAction() { return shulkerPurgeAction; }
}