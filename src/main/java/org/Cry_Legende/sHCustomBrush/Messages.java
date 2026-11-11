package org.Cry_Legende.sHCustomBrush;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/** Textes de messages.yml, avec les valeurs du jar pour toute clé absente du fichier du serveur. */
final class Messages {

    private final SHCustomBrush plugin;
    private final YamlConfiguration bundled;
    private volatile YamlConfiguration current;

    Messages(SHCustomBrush plugin) {
        this.plugin = plugin;
        this.bundled = new YamlConfiguration();
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                bundled.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING, "messages.yml du jar illisible : " + e.getMessage());
        }
        this.current = bundled;
    }

    /** Lit messages.yml sans rien appliquer ; le fichier est créé à partir du jar s'il manque. */
    YamlConfiguration read() throws IOException, InvalidConfigurationException {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file);
        yaml.setDefaults(bundled);
        return yaml;
    }

    void use(YamlConfiguration messages) {
        this.current = messages;
    }

    /** Texte avec couleurs et variables remplacées ; vide si le message est désactivé. */
    String get(String key, String... placeholders) {
        String text = current.getString(key);
        if (text == null) {
            return key;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace(placeholders[i], placeholders[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    void send(CommandSender to, String key, String... placeholders) {
        String text = get(key, placeholders);
        if (!text.isEmpty()) {
            to.sendMessage(text);
        }
    }
}
