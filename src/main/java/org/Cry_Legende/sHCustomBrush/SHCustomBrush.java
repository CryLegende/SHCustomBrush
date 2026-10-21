package org.Cry_Legende.sHCustomBrush;

import org.bukkit.Material;
import org.bukkit.block.BrushableBlock;
import org.bukkit.block.data.Brushable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SHCustomBrush extends JavaPlugin implements Listener, CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("shcustombrush").setExecutor(this);
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
        ConfigurationSection lootTable = getConfig().getConfigurationSection("LootTable");
        if (lootTable == null || lootTable.getKeys(false).isEmpty()) {
            return null;
        }

        List<String> keys = new ArrayList<>(lootTable.getKeys(false));
        double totalChance = keys.stream()
            .mapToDouble(key -> lootTable.getDouble(key + ".chance", 0))
            .sum();

        if (totalChance <= 0) {
            return null;
        }

        Random random = new Random();
        double randomChance = random.nextDouble() * totalChance;
        double accumulatedChance = 0;

        for (String key : keys) {
            double chance = lootTable.getDouble(key + ".chance", 0);
            accumulatedChance += chance;

            if (randomChance < accumulatedChance) {
                String serializedItem = lootTable.getString(key + ".serializedItem");
                if (serializedItem == null) {
                    continue;
                }

                YamlConfiguration config = new YamlConfiguration();
                try {
                    config.loadFromString(serializedItem);
                    ItemStack itemStack = config.getItemStack("item");
                    if (itemStack != null) {
                        return itemStack;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("shcustombrush.add")) {
            player.sendMessage("§8§l« §f§lSH §8§l» §fCommande inconnue, essayer autre chose.");
            return false;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType() == Material.AIR) {
                player.sendMessage("§cVous devez tenir un objet pour l'ajouter.");
                return false;
            }

            YamlConfiguration config = new YamlConfiguration();
            config.set("item", itemInHand);
            String serializedItem = config.saveToString();

            try {
                double chance = Double.parseDouble(args[2]);
                getConfig().set("LootTable." + args[1] + ".serializedItem", serializedItem);
                getConfig().set("LootTable." + args[1] + ".chance", chance);
                saveConfig();
                player.sendMessage("§aL'objet a été ajouté sous le nom: " + args[1] + " avec une chance de " + chance + "%");
                return true;
            } catch (NumberFormatException e) {
                player.sendMessage("§cLa chance doit être un nombre valide (avec jusqu'à 2 décimales).");
                return false;
            }
        }

        player.sendMessage("§cUsage: /shcustombrush add <nom_item> <chance>");
        return false;
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
