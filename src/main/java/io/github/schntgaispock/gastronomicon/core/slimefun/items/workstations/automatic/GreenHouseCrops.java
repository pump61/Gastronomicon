package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.automatic;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import lombok.experimental.UtilityClass;

/**
 * Maps a Green House seed item to the fruit it produces. These seeds/saplings
 * (grape, tomato, banana, etc.) can no longer be planted in the world - they
 * are obtained only by breaking grass and are grown exclusively in a
 * {@link GreenHouse}.
 */
@UtilityClass
public class GreenHouseCrops {

    private static final Map<String, ItemStack> CROPS = new HashMap<>();

    public static void register(SlimefunItemStack seed, ItemStack fruit) {
        CROPS.put(seed.getItemId(), fruit);
    }

    @Nullable
    public static ItemStack getFruit(String seedId) {
        final ItemStack fruit = CROPS.get(seedId);
        return fruit == null ? null : fruit.clone();
    }

}
