package io.github.schntgaispock.gastronomicon.core.listeners;

import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import net.guizhanss.guizhanlib.slimefuncn.utils.NewBlockStorageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;

import io.github.schntgaispock.gastronomicon.Gastronomicon;
import io.github.schntgaispock.gastronomicon.core.slimefun.items.seeds.AbstractSeed;
import io.github.schntgaispock.gastronomicon.core.slimefun.items.seeds.DuplicatingSeed;
import io.github.schntgaispock.gastronomicon.core.slimefun.items.seeds.FruitingSeed;
import io.github.schntgaispock.gastronomicon.core.slimefun.items.seeds.VineSeed;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

public class SeedListener implements Listener {

    @EventHandler
    public void onCropGrow(@Nonnull BlockGrowEvent e) {
        switch (e.getNewState().getType()) {
            case SUGAR_CANE, CACTUS:
                assignGastroSeed(StorageCacheUtils.getSfItem(e.getBlock().getRelative(BlockFace.DOWN).getLocation()),
                    e.getNewState().getLocation());
                break;

            case PUMPKIN, MELON:
                // Have to schedule it later because there is no way to tell where the stem that
                // grew the plant is before its grown.
                Bukkit.getScheduler().runTaskLater(Gastronomicon.getInstance(), () -> {
                    for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
                        BlockFace.WEST }) {
                        final Block checking = e.getBlock().getRelative(face);
                        if (checking.getType() != Material.ATTACHED_MELON_STEM
                            && checking.getType() != Material.ATTACHED_PUMPKIN_STEM) {
                            continue;
                        }

                        final Directional stemData = (Directional) checking.getBlockData();

                        if (stemData.getFacing().getOppositeFace().equals(face)) {
                            assignGastroSeed(StorageCacheUtils.getSfItem(checking.getLocation()), e.getNewState().getLocation());
                            break;
                        }
                    }
                }, 1);
                break;

            default:
                break;
        }
    }

    @EventHandler
    public void onVineGrow(BlockSpreadEvent e) {
        switch (e.getNewState().getType()) {
            case VINE:
                assignGastroSeed(StorageCacheUtils.getSfItem(e.getSource().getLocation()),
                    e.getNewState().getLocation());
                break;

            default:
                break;
        }
    }

    @EventHandler
    public void onCropDestroy(BlockDestroyEvent e) {
        final Block b = e.getBlock();
        final AbstractSeed seed = getGastroSeed(b);

        if (seed != null) {
            e.setWillDrop(false);
            seed.getHarvestDrops(b.getState(), new ItemStack(Material.AIR), false).forEach(
                drop -> b.getWorld().dropItemNaturally(b.getLocation(), drop));
            Slimefun.getDatabaseManager().getBlockDataController().removeBlock(b.getLocation());
        }
    }

    private static final int STORAGE_LOAD_RETRY_TICKS = 2;
    private static final int STORAGE_LOAD_MAX_ATTEMPTS = 100;

    /**
     * Handles the break ourselves, at the earliest possible priority, instead
     * of relying on {@link AbstractSeed}'s Slimefun-native
     * {@code BlockBreakHandler}.
     * <p>
     * Two distinct problems were observed in the wild, both causing this
     * addon's crop to silently fail to drop and vanilla's own (much worse)
     * drop to happen instead:
     * <ol>
     * <li>Right after a server restart, Slimefun's block-data cache is
     * populated per-chunk asynchronously, and a cache-only lookup like
     * {@link StorageCacheUtils#getSfItem(Location)} can return null even
     * though the chunk is loaded and this really is one of our crops - the
     * cache just hasn't caught up yet.</li>
     * <li>Even once the cache is populated, something else touching the same
     * {@link BlockBreakEvent} between this listener (LOWEST) and Slimefun's
     * own native handling (HIGHEST) can turn the block to air before
     * Slimefun gets a chance to read its {@code Ageable} growth stage -
     * confirmed by logging: the block was still e.g. WHEAT here, but AIR by
     * the time Slimefun's handler ran later in the same tick.</li>
     * </ol>
     * Capturing the block's state here, at the earliest point any plugin can
     * react to the break, and doing the harvest ourselves sidesteps both.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCropBreakStorageRace(BlockBreakEvent e) {
        final Block b = e.getBlock();
        switch (b.getType()) {
            case WHEAT, POTATOES, CARROTS, BEETROOTS:
                break;
            default:
                return;
        }

        final Location loc = b.getLocation();
        final Player player = e.getPlayer();
        final ItemStack tool = player.getInventory().getItemInMainHand().clone();
        final SlimefunItem cached = StorageCacheUtils.getSfItem(loc);

        if (cached != null) {
            // Found immediately - handle it right now, using the block state
            // as it is at this earliest priority, before anything else can
            // touch it.
            e.setCancelled(true);
            if (cached instanceof final AbstractSeed seed) {
                seed.getHarvestDrops(b.getState(), tool, true)
                    .forEach(drop -> b.getWorld().dropItemNaturally(loc, drop));
                b.setType(Material.AIR);
                Slimefun.getDatabaseManager().getBlockDataController().removeBlock(loc);
            } else {
                b.breakNaturally(tool);
            }
            return;
        }

        e.setCancelled(true);
        final BukkitTask[] task = new BukkitTask[1];
        final int[] attemptsRemaining = { STORAGE_LOAD_MAX_ATTEMPTS };
        task[0] = Gastronomicon.scheduleSyncRepeatingTask(() -> {
            final SlimefunItem item = StorageCacheUtils.getSfItem(loc);
            if (item != null) {
                if (item instanceof final AbstractSeed seed) {
                    seed.getHarvestDrops(b.getState(), tool, true)
                        .forEach(drop -> b.getWorld().dropItemNaturally(loc, drop));
                    b.setType(Material.AIR);
                    Slimefun.getDatabaseManager().getBlockDataController().removeBlock(loc);
                } else {
                    b.breakNaturally(tool);
                }
                task[0].cancel();
                return;
            }

            if (--attemptsRemaining[0] <= 0) {
                b.breakNaturally(tool);
                task[0].cancel();
            }
        }, STORAGE_LOAD_RETRY_TICKS, STORAGE_LOAD_RETRY_TICKS);
    }

    @EventHandler
    public void onLiquidCropDestroy(BlockFromToEvent e) {
        final Block b = e.getToBlock();
        final AbstractSeed seed = getGastroSeed(b);

        if (seed != null) {
            e.setCancelled(true); // too bad there is no way to cancel drops!
            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1, 1);
            seed.getHarvestDrops(e.getBlock().getState(), new ItemStack(Material.AIR), false).forEach(
                drop -> e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop));
            b.setType(Material.AIR);
            Slimefun.getDatabaseManager().getBlockDataController().removeBlock(b.getLocation());
        }
    }

    @EventHandler
    public void onVillagerCropDestroy(EntityChangeBlockEvent e) {
        if (getGastroSeed(e.getBlock()) != null) {
            e.setCancelled(true);
        }
    }

    private void assignGastroSeed(SlimefunItem item, @Nonnull Location l) {
        if (item == null) {
            return;
        }

        if (item instanceof DuplicatingSeed || item instanceof VineSeed) {
            NewBlockStorageUtil.createBlock(l, item.getId());
        } else if (item instanceof final FruitingSeed fgs) {
            final SlimefunItem fruitingBody = fgs.getFruitingBody();
            if (fruitingBody == null) {
                Gastronomicon.log(Level.WARNING,
                    "assignGastroSeed at " + formatLocation(l) + " -> " + item.getId()
                        + " has a NULL fruiting body - see the warning logged when this seed was registered."
                        + " This growth tick will NOT produce a vegetable.");
                return;
            }
            NewBlockStorageUtil.createBlock(l, fruitingBody.getId());
        }
    }

    private static String formatLocation(@Nonnull Location l) {
        return l.getWorld().getName() + " " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    private AbstractSeed getGastroSeed(Block cropBlock) {
        if (cropBlock == null)
            return null;
        switch (cropBlock.getType()) {
            case WHEAT, POTATOES, CARROTS, BEETROOTS, PUMPKIN_STEM, ATTACHED_PUMPKIN_STEM, MELON_STEM, ATTACHED_MELON_STEM, SUGAR_CANE, CACTUS, VINE:
                break;
            default:
                return null;
        }

        final SlimefunItem item = StorageCacheUtils.getSfItem(cropBlock.getLocation());
        if (item == null) {
            return null;
        }

        if (item instanceof final AbstractSeed seed) {
            return seed;
        }

        return null;

    }

    public static void setup() {
        Bukkit.getPluginManager().registerEvents(new SeedListener(), Gastronomicon.getInstance());
    }
}
