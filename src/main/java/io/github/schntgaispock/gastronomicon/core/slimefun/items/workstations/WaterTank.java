package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations;

import org.bukkit.NamespacedKey;

/**
 * Marks a workstation as having an internal water tank that can be topped
 * up by shift + right-clicking it with a water bucket or water bottle - see
 * {@code WaterRefillListener}. First introduced by the Fermenter; anything
 * else that needs the same "fill me with water" mechanic implements this
 * instead of duplicating the fill logic.
 */
public interface WaterTank {

    /**
     * The maximum amount of water (in mB) this tank can hold.
     */
    int getWaterCapacity();

    /**
     * The per-block PDC key ({@link io.github.schntgaispock.gastronomicon.util.ChunkPDC})
     * this tank stores its current water level under.
     */
    NamespacedKey getWaterKey();
}
