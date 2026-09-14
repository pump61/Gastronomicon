package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.manual;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.schntgaispock.gastronomicon.core.slimefun.recipes.GastroRecipeType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;

public class CulinaryWorkbench extends GastroWorkstation {

    public CulinaryWorkbench(SlimefunItemStack item, ItemStack[] recipe) {
        super(item, recipe);
    }

    private static final int RECIPE_BOOK_SLOT = 52;

    @Override
    protected void setup(BlockMenuPreset preset) {
        super.setup(preset);
        addRecipeBookButton(preset, RECIPE_BOOK_SLOT, "Culinary Workbench Recipes");
    }

    @Override
    public GastroRecipeType getGastroRecipeType() {
        return GastroRecipeType.CULINARY_WORKBENCH;
    }

    @Override
    protected boolean canCraft(BlockMenu menu, Block b, Player p, boolean sendMessage) {
        return true;
    }
    
}
