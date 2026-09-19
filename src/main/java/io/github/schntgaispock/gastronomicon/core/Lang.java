package io.github.schntgaispock.gastronomicon.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.bukkit.configuration.ConfigurationSection;
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

    /**
     * Tier glyphs from the server's shared ItemsAdder rarity font (see
     * {@code contents/abtall/configs/rarity.yml}) - same convention every
     * other addon on this server uses for the last lore line of an item.
     */
    private static final Map<String, String> TIER_GLYPHS = Map.of(
        "COMMON", "&f𳭙",
        "RARE", "&f𳭚",
        "EPIC", "&f𳭉",
        "LEGENDARY", "&f𳭋");

    private static YamlConfiguration fallback;
    private static YamlConfiguration active;
    private static final Map<String, String> ITEM_RARITY = new HashMap<>();

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

        loadRarity();
    }

    private static void loadRarity() {
        ITEM_RARITY.clear();
        final InputStream stream = Gastronomicon.getInstance().getResource("rarity.yml");
        if (stream == null) {
            return;
        }
        final YamlConfiguration rarity = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        for (final String id : rarity.getKeys(false)) {
            ITEM_RARITY.put(id, rarity.getString(id));
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

    private static final java.util.regex.Pattern ITEM_LORE_KEY = java.util.regex.Pattern.compile("^items\\.(GN_[A-Z0-9_]+)\\.lore$");

    /**
     * Gets a translated list of strings for the given key, e.g. a lore list.
     * Falls back to en_US if the key is missing or empty in the active locale.
     * <br>
     * <br>
     * When {@code path} is an item's lore key ({@code items.<ID>.lore}), the
     * item's tier glyph (from {@code rarity.yml}, matching every other addon
     * on this server's rarity convention) is appended as the final line, so
     * every item shows its tier regardless of whether it has flavor text.
     */
    @Nonnull
    public static List<String> getList(@Nonnull String path) {
        final List<String> value = active.getStringList(path);
        final List<String> result = new ArrayList<>(value.isEmpty() ? fallback.getStringList(path) : value);

        final java.util.regex.Matcher matcher = ITEM_LORE_KEY.matcher(path);
        if (matcher.matches()) {
            final String tier = ITEM_RARITY.get(matcher.group(1));
            final String glyph = tier == null ? null : TIER_GLYPHS.get(tier);
            if (glyph != null) {
                if (!result.isEmpty()) {
                    result.add("");
                }
                result.add(glyph);
            }
        }

        return result;
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
        final boolean hasBundled = plugin.getResource(path) != null;

        // Seed the data folder with a bundled copy (if there is one) the first
        // time this locale is used, so there's something for a translator to
        // find and edit. This is skipped entirely for locales nobody bundled -
        // those are expected to be dropped into the data folder by hand.
        if (!externalFile.exists()) {
            if (!hasBundled) {
                return null;
            }
            plugin.saveResource(path, false);
        }

        final YamlConfiguration external = YamlConfiguration.loadConfiguration(externalFile);

        // A translator's copy on disk is meant to be hand-edited, so it's
        // never overwritten wholesale - but any key that a newer build adds
        // and the on-disk copy doesn't have yet (translated or not) is merged
        // in, so new content shows up (falling back to en_US via get()/
        // getList() until translated) without deleting the file by hand.
        if (hasBundled) {
            try (InputStream bundledStream = plugin.getResource(path)) {
                final YamlConfiguration bundled = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(bundledStream, StandardCharsets.UTF_8));
                if (mergeMissingKeys(bundled, external)) {
                    external.save(externalFile);
                }
            } catch (IOException e) {
                Gastronomicon.warn("Could not update language file " + path + ": " + e.getMessage());
            }
        }

        return external;
    }

    /**
     * Copies every key present in {@code source} but missing from
     * {@code target} into {@code target}, recursing into nested sections.
     * Existing keys in {@code target} - including ones a translator has
     * edited - are never touched.
     *
     * @return whether anything was added
     */
    private static boolean mergeMissingKeys(ConfigurationSection source, ConfigurationSection target) {
        boolean changed = false;
        for (final String key : source.getKeys(false)) {
            if (source.isConfigurationSection(key)) {
                ConfigurationSection targetSection = target.getConfigurationSection(key);
                if (targetSection == null) {
                    targetSection = target.createSection(key);
                }
                changed |= mergeMissingKeys(source.getConfigurationSection(key), targetSection);
            } else if (!target.contains(key)) {
                target.set(key, source.get(key));
                changed = true;
            }
        }
        return changed;
    }
}
