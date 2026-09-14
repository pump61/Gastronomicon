package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.manual;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import io.github.schntgaispock.gastronomicon.core.slimefun.GastroGroups;
import io.github.schntgaispock.gastronomicon.core.slimefun.GastroStacks;
import io.github.schntgaispock.gastronomicon.util.ChunkPDC;
import io.github.schntgaispock.gastronomicon.util.item.GastroKeys;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * A block that, once filled with water, acts as an infinite water source.
 * Breaking it always returns it to the empty state.
 */
public class WaterSink extends SimpleSlimefunItem<BlockUseHandler> {

    public WaterSink(SlimefunItemStack item, ItemStack[] recipe) {
        super(GastroGroups.BASIC_MACHINES, item, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);
    }

    @Override
    public void preRegister() {
        super.preRegister();

        addItemHandler(new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(org.bukkit.event.block.BlockPlaceEvent e) {
                ChunkPDC.set(e.getBlock(), GastroKeys.WATER_SINK_FILLED, false);
            }
        });

        addItemHandler(new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
                ChunkPDC.remove(e.getBlock(), GastroKeys.WATER_SINK_FILLED);
            }
        });
    }

    @Override
    public BlockUseHandler getItemHandler() {
        return event -> {
            final Player p = event.getPlayer();
            if (event.getClickedBlock().isEmpty()) {
                return;
            }
            final Block b = event.getClickedBlock().get();

            final ItemStack inHand = event.getItem();
            if (inHand == null) {
                return;
            }

            final Boolean filled = ChunkPDC.getBool(b, GastroKeys.WATER_SINK_FILLED);
            if (filled == null) {
                return;
            }

            switch (inHand.getType()) {
                case BUCKET -> {
                    if (!filled) {
                        return;
                    }
                    event.cancel();
                    if (p.getGameMode() != GameMode.CREATIVE) {
                        inHand.setType(Material.WATER_BUCKET);
                    }
                    b.getWorld().playSound(b.getLocation(), Sound.ITEM_BUCKET_FILL, SoundCategory.PLAYERS, 1f, 1f);
                }
                case GLASS_BOTTLE -> {
                    if (!filled) {
                        return;
                    }
                    event.cancel();
                    if (p.getGameMode() != GameMode.CREATIVE) {
                        inHand.subtract(1);
                        p.getInventory().addItem(GastroStacks.WATER_BOTTLE.clone());
                    }
                    b.getWorld().playSound(b.getLocation(), Sound.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 1f, 1f);
                }
                case WATER_BUCKET -> {
                    event.cancel();
                    ChunkPDC.set(b, GastroKeys.WATER_SINK_FILLED, true);
                    if (p.getGameMode() != GameMode.CREATIVE) {
                        inHand.setType(Material.BUCKET);
                    }
                    b.getWorld().playSound(b.getLocation(), Sound.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 1f, 1f);
                }
                case POTION -> {
                    if (!(inHand.getItemMeta() instanceof final PotionMeta meta)
                        || meta.getBasePotionData().getType() != PotionType.WATER) {
                        return;
                    }
                    event.cancel();
                    ChunkPDC.set(b, GastroKeys.WATER_SINK_FILLED, true);
                    if (p.getGameMode() != GameMode.CREATIVE) {
                        inHand.setType(Material.GLASS_BOTTLE);
                    }
                    b.getWorld().playSound(b.getLocation(), Sound.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 1f, 1f);
                }
                default -> {
                }
            }
        };
    }

}
