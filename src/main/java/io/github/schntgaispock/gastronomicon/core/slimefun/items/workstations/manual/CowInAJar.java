package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.manual;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.schntgaispock.gastronomicon.core.slimefun.GastroGroups;
import io.github.schntgaispock.gastronomicon.core.slimefun.recipes.GastroRecipeType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * A never-ending source of milk, like Cooking for Blockheads' "Cow in a Jar" -
 * right-click it with a bucket for a free milk bucket, no filling required.
 * Not craftable normally; obtained by dropping an anvil on a cow, see
 * {@link io.github.schntgaispock.gastronomicon.core.listeners.CowInAJarListener}.
 */
public class CowInAJar extends SimpleSlimefunItem<BlockUseHandler> {

    public CowInAJar(SlimefunItemStack item, ItemStack[] recipe) {
        super(GastroGroups.BASIC_MACHINES, item, GastroRecipeType.KILL, recipe);
    }

    @Override
    public BlockUseHandler getItemHandler() {
        return event -> {
            final Player p = event.getPlayer();
            if (event.getClickedBlock().isEmpty()) {
                return;
            }

            final ItemStack inHand = event.getItem();
            if (inHand == null || inHand.getType() != Material.BUCKET) {
                return;
            }

            final Block b = event.getClickedBlock().get();
            event.cancel();

            if (p.getGameMode() != GameMode.CREATIVE) {
                inHand.setType(Material.MILK_BUCKET);
            }
            b.getWorld().playSound(b.getLocation(), Sound.ITEM_BUCKET_FILL, SoundCategory.PLAYERS, 1f, 1f);
        };
    }

}
