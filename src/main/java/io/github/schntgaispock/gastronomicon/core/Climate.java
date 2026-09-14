package io.github.schntgaispock.gastronomicon.core;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Climate {
    DRY(new CustomItemStack(Material.SAND, "&eDry")),
    TEMPERATE(new CustomItemStack(Material.GRASS_BLOCK, "&eTemperate")),
    COLD(new CustomItemStack(Material.ICE, "&eCold")),
    SNOWY(new CustomItemStack(Material.SNOW, "&eSnowy")),
    NETHER(new CustomItemStack(Material.CRIMSON_NYLIUM, "&eNether")),
    END(new CustomItemStack(Material.END_STONE, "&eEnd"));

    private final @Getter ItemStack displayItem;

    // Biome stopped being a plain enum (it's now a registry-backed OldEnum), so it can
    // no longer be used as a switch selector with unqualified case labels.
    private static final Set<Biome> DRY_BIOMES = Set.of(
        Biome.BADLANDS, Biome.WOODED_BADLANDS, Biome.ERODED_BADLANDS, Biome.DESERT,
        Biome.SAVANNA, Biome.WINDSWEPT_SAVANNA, Biome.SAVANNA_PLATEAU);
    private static final Set<Biome> COLD_BIOMES = Set.of(
        Biome.DEEP_FROZEN_OCEAN, Biome.OLD_GROWTH_PINE_TAIGA, Biome.TAIGA,
        Biome.OLD_GROWTH_SPRUCE_TAIGA, Biome.WINDSWEPT_HILLS, Biome.WINDSWEPT_FOREST,
        Biome.WINDSWEPT_GRAVELLY_HILLS, Biome.STONY_SHORE);
    private static final Set<Biome> SNOWY_BIOMES = Set.of(
        Biome.SNOWY_BEACH, Biome.SNOWY_PLAINS, Biome.ICE_SPIKES, Biome.FROZEN_RIVER,
        Biome.FROZEN_OCEAN, Biome.GROVE, Biome.SNOWY_SLOPES, Biome.SNOWY_TAIGA,
        Biome.JAGGED_PEAKS, Biome.FROZEN_PEAKS);
    private static final Set<Biome> NETHER_BIOMES = Set.of(
        Biome.NETHER_WASTES, Biome.CRIMSON_FOREST, Biome.WARPED_FOREST,
        Biome.SOUL_SAND_VALLEY, Biome.BASALT_DELTAS);
    private static final Set<Biome> END_BIOMES = Set.of(
        Biome.THE_END, Biome.SMALL_END_ISLANDS, Biome.END_BARRENS, Biome.END_MIDLANDS,
        Biome.END_HIGHLANDS, Biome.THE_VOID);

    public static Climate of(Biome b) {
        if (DRY_BIOMES.contains(b)) return Climate.DRY;
        if (COLD_BIOMES.contains(b)) return Climate.COLD;
        if (SNOWY_BIOMES.contains(b)) return Climate.SNOWY;
        if (NETHER_BIOMES.contains(b)) return Climate.NETHER;
        if (END_BIOMES.contains(b)) return Climate.END;
        return Climate.TEMPERATE;
    }

    public static Climate of(Block b) {
        return of(b.getBiome());
    }

    public static Climate of(Location l) {
        return of(l.getBlock().getBiome());
    }
}