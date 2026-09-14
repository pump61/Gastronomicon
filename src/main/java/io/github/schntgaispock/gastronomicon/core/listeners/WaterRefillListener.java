package io.github.schntgaispock.gastronomicon.core.listeners;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import io.github.schntgaispock.gastronomicon.Gastronomicon;
import io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.WaterTank;
import io.github.schntgaispock.gastronomicon.util.ChunkPDC;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;

/**
 * Lets a player shift + right-click any {@link WaterTank} workstation with a
 * water bucket or water bottle to top up its internal water level.
 */
public class WaterRefillListener implements Listener {

    // because for some bs reason these workstations won't let us add a BlockUseHandler
    @EventHandler
    public void onRefill(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || !e.getPlayer().isSneaking())
            return;

        if (e.getItem() == null) {
            return;
        }

        final Block b = e.getClickedBlock();
        if (b == null)
            return;

        if (!Slimefun.getProtectionManager().hasPermission(e.getPlayer(), b, Interaction.INTERACT_BLOCK))
            return;

        final SlimefunItem sfItem = StorageCacheUtils.getSfItem(b.getLocation());
        if (sfItem == null || !(sfItem instanceof final WaterTank tank))
            return;

        final Integer water = ChunkPDC.getInt(b, tank.getWaterKey());
        if (water == null)
            return;

        final int refill;
        final Material ret;
        switch (e.getItem().getType()) {
            case WATER_BUCKET:
                refill = 1000;
                ret = Material.BUCKET;
                break;
            case POTION:
                if (((PotionMeta) e.getItem().getItemMeta()).getBasePotionData().getType() == PotionType.WATER) {
                    refill = 333;
                    ret = Material.GLASS_BOTTLE;
                    break;
                } else {
                    return;
                }
            default:
                return;
        }
        if (refill == 0)
            return;

        e.setCancelled(true);
        if (water == tank.getWaterCapacity()) {
            return;
        }

        ChunkPDC.set(b, tank.getWaterKey(), Math.min(water + refill, tank.getWaterCapacity()));
        b.getWorld().playSound(b.getLocation(), Sound.ITEM_BUCKET_FILL, SoundCategory.PLAYERS, 1f, 1f);

        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.getItem().setType(ret);
        }
    }

    public static void setup() {
        Bukkit.getPluginManager().registerEvents(new WaterRefillListener(), Gastronomicon.getInstance());
    }

}
