package io.github.schntgaispock.gastronomicon.core.slimefun.items.food;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * Toasted Bread restores 1 extra hunger point on top of the vanilla Bread it
 * is based on, the same way Slimefun's Apple Juice grants bonus hunger.
 */
public class ToastedBread extends SimpleSlimefunItem<ItemConsumptionHandler> {

    @ParametersAreNonnullByDefault
    public ToastedBread(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public ItemConsumptionHandler getItemHandler() {
        return (e, p, item) -> p.setFoodLevel(Math.min(p.getFoodLevel() + 1, 20));
    }
}
