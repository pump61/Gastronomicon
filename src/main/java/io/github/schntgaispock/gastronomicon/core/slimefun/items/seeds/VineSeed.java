package io.github.schntgaispock.gastronomicon.core.slimefun.items.seeds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import lombok.Getter;

public class VineSeed extends AbstractSeed {

    private final @Getter List<ItemStack> grownCrops;

    public VineSeed(SlimefunItemStack item, ItemStack[] gatherSources, ItemStack... crops) {
        super(item, gatherSources);

        grownCrops = new ArrayList<>(Arrays.asList(crops));
    }

    @Override
    public List<ItemStack> getHarvestDrops(BlockState e, ItemStack item, boolean brokenByPlayer) {
        final List<ItemStack> clones = new ArrayList<>(grownCrops.size() + 1);
        for (ItemStack drop : grownCrops) {
            clones.add(drop.clone());
        }

        clones.add(getItem().clone());

        return clones;
    }

    @Override
    public boolean isMature(BlockState b) {
        return true;
    }
    
}
