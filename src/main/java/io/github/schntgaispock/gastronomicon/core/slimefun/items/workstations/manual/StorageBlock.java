package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.manual;

import org.bukkit.inventory.ItemStack;

import io.github.mooy1.infinitylib.machines.MenuBlock;
import io.github.schntgaispock.gastronomicon.core.slimefun.GastroGroups;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;

/**
 * A plain storage container with no processing logic - every slot is free-form storage.
 */
public class StorageBlock extends MenuBlock {

    private final int[] storageSlots;

    public StorageBlock(SlimefunItemStack item, ItemStack[] recipe, int rows) {
        this(GastroGroups.BASIC_MACHINES, item, recipe, rows);
    }

    public StorageBlock(ItemGroup itemGroup, SlimefunItemStack item, ItemStack[] recipe, int rows) {
        super(itemGroup, item, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);

        storageSlots = new int[rows * 9];
        for (int i = 0; i < storageSlots.length; i++) {
            storageSlots[i] = i;
        }
    }

    @Override
    protected void setup(BlockMenuPreset preset) {
        preset.setSize(storageSlots.length);
    }

    @Override
    protected int[] getInputSlots() {
        return storageSlots;
    }

    @Override
    protected int[] getOutputSlots() {
        return storageSlots;
    }

}
