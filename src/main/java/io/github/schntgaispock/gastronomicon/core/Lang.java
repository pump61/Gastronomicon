package io.github.schntgaispock.gastronomicon.core;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nonnull;

import org.bukkit.configuration.file.YamlConfiguration;

import io.github.schntgaispock.gastronomicon.Gastronomicon;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import lombok.experimental.UtilityClass;

/**
 * Loads translatable item/message text from {@code lang/<locale>.yml} files,
 * so the plugin can be translated without touching any Java code.
 * <br>
 * <br>
 * The active locale comes from {@code options.language} in config.yml. When
 * that is left as {@code auto} (the default), it instead mirrors Slimefun's
 * own configured default language, so Gastronomicon matches the rest of
 * Slimefun's translation without a separate setting on most servers. A
 * locale's file is looked up first in the plugin's data folder
 * ({@code lang/<locale>.yml}, for community-supplied translations that don't
 * need a rebuild of the plugin) and falls back to a bundled copy of the same
 * name in the jar's resources, if one exists. Any key missing from the
 * active locale falls back to {@code en_US}, which is always bundled, so a
 * translation only has to cover what it actually translates.
 */
@UtilityClass
public class Lang {

    private static final String DEFAULT_LOCALE = "en_US";
    private static final String AUTO_LOCALE = "auto";

    private static YamlConfiguration fallback;
    private static YamlConfiguration active;

    public static void setup() {
        fallback = loadBundled(DEFAULT_LOCALE);

        String locale = Gastronomicon.getInstance().getConfig().getString("options.language", AUTO_LOCALE);
        if (locale.equalsIgnoreCase(AUTO_LOCALE)) {
            locale = resolveSlimefunLocale();
        }

        active = locale.equals(DEFAULT_LOCALE) ? fallback : loadLocale(locale);

        if (active == null) {
            Gastronomicon.warn("Could not find a language file for locale \"" + locale + "\", falling back to " + DEFAULT_LOCALE + ".");
            active = fallback;
        }
    }

    /**
     * Mirrors Slimefun's own server-wide default language (the language
     * option in the guide is per-player, but this default is what
     * determines every player's language unless they've changed it
     * themselves), so servers don't need a separate language setting just
     * for Gastronomicon.
     */
    @Nonnull
    private static String resolveSlimefunLocale() {
        try {
            return Slimefun.getLocalization().getDefaultLanguage().getId();
        } catch (Exception e) {
            return DEFAULT_LOCALE;
        }
    }

    /**
     * Gets a translated string for the given key, e.g. {@code "items.GN_TOASTER.name"}.
     * Falls back to en_US if the key is missing from the active locale, and
     * to the key itself (so a missing translation is obvious in-game rather
     * than silently blank) if it is missing from en_US too.
     */
    @Nonnull
    public static String get(@Nonnull String path) {
        String value = active.getString(path);
        if (value == null) {
            value = fallback.getString(path);
        }
        return value == null ? path : value;
    }

    /**
     * Gets a translated list of strings for the given key, e.g. a lore list.
     * Falls back to en_US if the key is missing or empty in the active locale.
     */
    @Nonnull
    public static List<String> getList(@Nonnull String path) {
        final List<String> value = active.getStringList(path);
        return value.isEmpty() ? fallback.getStringList(path) : value;
    }

    @Nonnull
    private static YamlConfiguration loadBundled(String locale) {
        final InputStream stream = Gastronomicon.getInstance().getResource("lang/" + locale + ".yml");
        if (stream == null) {
            throw new IllegalStateException("Missing bundled language file lang/" + locale + ".yml!");
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private static YamlConfiguration loadLocale(String locale) {
        final Gastronomicon plugin = Gastronomicon.getInstance();
        final String path = "lang/" + locale + ".yml";
        final File externalFile = new File(plugin.getDataFolder(), path);

        // Seed the data folder with a bundled copy (if there is one) the first
        // time this locale is used, so there's something for a translator to
        // find and edit. This is skipped entirely for locales nobody bundled -
        // those are expected to be dropped into the data folder by hand.
        if (!externalFile.exists() && plugin.getResource(path) != null) {
            plugin.saveResource(path, false);
        }

        if (externalFile.exists()) {
            return YamlConfiguration.loadConfiguration(externalFile);
        }

        final InputStream bundled = plugin.getResource(path);
        return bundled == null ? null : YamlConfiguration.loadConfiguration(new InputStreamReader(bundled, StandardCharsets.UTF_8));
    }
}
