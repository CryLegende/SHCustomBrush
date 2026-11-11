package org.Cry_Legende.sHCustomBrush;

import org.bukkit.Material;
import org.bukkit.block.BrushableBlock;
import org.bukkit.block.data.Brushable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class SHCustomBrush extends JavaPlugin implements Listener, TabExecutor {

    private static final String ADMIN = "shcustombrush.admin";
    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "list", "reload");

    /** Un objet de la table de loot, désérialisé une fois au chargement. */
    private record Loot(ItemStack item, double chance) {
    }

    private File configFile;
    // config.yml tel qu'il a été lu ; null tant qu'il n'a pas pu l'être, pour ne jamais l'écraser avec une table vide
    private YamlConfiguration settings;
    private volatile Map<String, Loot> table = Map.of();
    private final List<String> invalidEntries = new ArrayList<>();
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configFile = new File(getDataFolder(), "config.yml");
        messages = new Messages(this);
        for (String error : reloadAll(true)) {
            getLogger().severe(error.replace('\n', ' '));
        }
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("shcustombrush").setExecutor(this);
        getCommand("shcustombrush").setTabCompleter(this);
    }

    /**
     * Relit config.yml (la table de loot) et messages.yml. Les deux sont d'abord lus à part : si l'un est illisible,
     * rien n'est remplacé (sauf au démarrage, où il n'y a encore rien à conserver).
     *
     * @return une entrée « fichier\nerreur » par fichier illisible, vide si tout a été rechargé
     */
    private List<String> reloadAll(boolean startup) {
        List<String> errors = new ArrayList<>();
        YamlConfiguration newSettings = null;
        YamlConfiguration newMessages = null;
        try {
            newSettings = new YamlConfiguration();
            newSettings.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            newSettings = null;
            errors.add(describe("config.yml", e));
        }
        try {
            newMessages = messages.read();
        } catch (IOException | InvalidConfigurationException e) {
            errors.add(describe("messages.yml", e));
        }
        if (!errors.isEmpty() && !startup) {
            return errors;
        }
        if (newSettings != null) {
            settings = newSettings;
            loadTable(newSettings);
        }
        if (newMessages != null) {
            messages.use(newMessages);
        }
        return errors;
    }

    private static String describe(String file, Exception e) {
        String reason = String.valueOf(e.getMessage()).strip();
        int newline = reason.indexOf('\n');
        return file + "\n" + (newline < 0 ? reason : reason.substring(0, newline));
    }

    private void loadTable(YamlConfiguration yaml) {
        Map<String, Loot> loaded = new LinkedHashMap<>();
        invalidEntries.clear();
        ConfigurationSection section = yaml.getConfigurationSection("LootTable");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                Loot loot = parseLoot(section.getConfigurationSection(name));
                if (loot == null) {
                    getLogger().warning("Entrée de LootTable invalide : " + name);
                    invalidEntries.add(name);
                } else {
                    loaded.put(name, loot);
                }
            }
        }
        table = loaded;
    }

    private static Loot parseLoot(ConfigurationSection entry) {
        if (entry == null) {
            return null;
        }
        double chance = entry.getDouble("chance", 0);
        String serialized = entry.getString("serializedItem");
        if (serialized == null || !(chance > 0) || Double.isInfinite(chance)) {
            return null;
        }
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(serialized);
            ItemStack item = yaml.getItemStack("item");
            return item == null ? null : new Loot(item, chance);
        } catch (InvalidConfigurationException | RuntimeException e) {
            return null;
        }
    }

    @EventHandler
    public void onBrush(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        Material blockType = event.getClickedBlock().getType();
        if (blockType != Material.SUSPICIOUS_GRAVEL && blockType != Material.SUSPICIOUS_SAND) {
            return;
        }

        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.BRUSH) {
            return;
        }

        BrushableBlock block = (BrushableBlock) event.getClickedBlock().getState();
        Brushable brushable = (Brushable) block.getBlockData();

        if (block.getItem().getType() == Material.AIR) {
            ItemStack customItem = getRandomCustomItem();
            if (customItem != null) {
                block.setItem(customItem);
                block.update(true);
            }
        }

        if (brushable.getDusted() == brushable.getMaximumDusted()) {
            block.update(true);
        }
    }

    private ItemStack getRandomCustomItem() {
        Map<String, Loot> current = table;
        double total = current.values().stream().mapToDouble(Loot::chance).sum();
        if (total <= 0) {
            return null;
        }
        double roll = ThreadLocalRandom.current().nextDouble() * total;
        double accumulated = 0;
        for (Loot loot : current.values()) {
            accumulated += loot.chance();
            if (roll < accumulated) {
                return loot.item().clone();
            }
        }
        return null;
    }

    // ----------------------------------------------------------------- commande

    private static boolean can(CommandSender sender, String sub) {
        return sender.hasPermission(ADMIN + "." + sub) || sender.hasPermission(ADMIN);
    }

    private static List<String> allowed(CommandSender sender) {
        return SUBCOMMANDS.stream().filter(sub -> can(sender, sub)).toList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        List<String> allowed = allowed(sender);
        if (allowed.isEmpty()) {
            messages.send(sender, "no-permission");
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (!allowed.contains(sub)) {
            if (SUBCOMMANDS.contains(sub)) {
                messages.send(sender, "no-permission");
            } else {
                messages.send(sender, "usage", "%subcommands%", String.join("|", allowed));
            }
            return true;
        }
        switch (sub) {
            case "add" -> add(sender, args);
            case "remove" -> remove(sender, args);
            case "list" -> list(sender);
            default -> reload(sender);
        }
        return true;
    }

    private void add(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return;
        }
        if (args.length != 3) {
            messages.send(sender, "usage-add");
            return;
        }
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            messages.send(sender, "hold-item");
            return;
        }
        double chance;
        try {
            chance = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            chance = -1;
        }
        if (!(chance > 0) || Double.isInfinite(chance)) {
            messages.send(sender, "invalid-chance");
            return;
        }
        if (settings == null) {
            messages.send(sender, "config-unreadable");
            return;
        }
        YamlConfiguration serializer = new YamlConfiguration();
        serializer.set("item", itemInHand.clone());
        String serialized = serializer.saveToString();

        settings.set("LootTable." + args[1] + ".serializedItem", serialized);
        settings.set("LootTable." + args[1] + ".chance", chance);
        if (!save()) {
            messages.send(sender, "save-failed");
            return;
        }
        loadTable(settings);
        messages.send(sender, "loot-added", "%name%", args[1], "%chance%", String.valueOf(chance));
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length != 2) {
            messages.send(sender, "usage-remove");
            return;
        }
        if (settings == null) {
            messages.send(sender, "config-unreadable");
            return;
        }
        if (!settings.isConfigurationSection("LootTable." + args[1])) {
            messages.send(sender, "loot-not-found", "%name%", args[1]);
            return;
        }
        settings.set("LootTable." + args[1], null);
        if (!save()) {
            messages.send(sender, "save-failed");
            return;
        }
        loadTable(settings);
        messages.send(sender, "loot-removed", "%name%", args[1]);
    }

    private void list(CommandSender sender) {
        Map<String, Loot> current = table;
        if (current.isEmpty() && invalidEntries.isEmpty()) {
            messages.send(sender, "list-empty");
            return;
        }
        double total = current.values().stream().mapToDouble(Loot::chance).sum();
        messages.send(sender, "list-header", "%count%", String.valueOf(current.size()), "%total%", format(total));
        for (Map.Entry<String, Loot> entry : current.entrySet()) {
            Loot loot = entry.getValue();
            messages.send(sender, "list-entry", "%name%", entry.getKey(), "%chance%", format(loot.chance()),
                "%percent%", format(loot.chance() * 100 / total), "%item%", loot.item().getType().name().toLowerCase(Locale.ROOT));
        }
        if (!invalidEntries.isEmpty()) {
            messages.send(sender, "list-invalid", "%names%", String.join(", ", invalidEntries));
        }
    }

    private void reload(CommandSender sender) {
        List<String> errors = reloadAll(false);
        if (!errors.isEmpty()) {
            for (String error : errors) {
                int split = error.indexOf('\n');
                messages.send(sender, "reload-error", "%file%", error.substring(0, split), "%error%", error.substring(split + 1));
            }
            return;
        }
        messages.send(sender, "reload-ok", "%count%", String.valueOf(table.size()));
        if (!invalidEntries.isEmpty()) {
            messages.send(sender, "reload-invalid", "%names%", String.join(", ", invalidEntries));
        }
    }

    private boolean save() {
        try {
            settings.save(configFile);
            return true;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Écriture de config.yml impossible", e);
            return false;
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        List<String> allowed = allowed(sender);
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(allowed);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove") && allowed.contains("remove")) {
            suggestions.addAll(table.keySet());
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntityType() == EntityType.FALLING_BLOCK) {
            Material type = event.getBlock().getType();
            if (type == Material.SUSPICIOUS_GRAVEL || type == Material.SUSPICIOUS_SAND) {
                event.setCancelled(true);
            }
        }
    }
}
