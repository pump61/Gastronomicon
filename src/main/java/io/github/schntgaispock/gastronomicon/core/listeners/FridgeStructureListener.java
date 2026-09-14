package io.github.schntgaispock.gastronomicon.core.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;

import io.github.schntgaispock.gastronomicon.Gastronomicon;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * A Fridge is a Fridge Module with an Iron Block placed directly above it and an
 * Iron Door on one of its sides. The Fridge is opened by right-clicking its Iron
 * Door, not the module itself - clicking the module directly does nothing.
 */
public class FridgeStructureListener implements Listener {

    private static final String FRIDGE_MODULE_ID = "GN_FRIDGE_MODULE";

    private static final BlockFace[] SIDES = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Block b = e.getClickedBlock();
        if (b == null) {
            return;
        }

        if (b.getType() == Material.IRON_DOOR) {
            onDoorClick(e, b);
        } else if (isFridgeModule(b)) {
            // The module itself does nothing - you must open the Fridge via its door.
            e.setCancelled(true);
        }
    }

    private void onDoorClick(PlayerInteractEvent e, Block door) {
        final Block bottomHalf = door.getBlockData() instanceof final Bisected bisected
            && bisected.getHalf() == Bisected.Half.TOP ? door.getRelative(BlockFace.DOWN) : door;

        Block module = null;
        for (BlockFace side : SIDES) {
            final Block relative = bottomHalf.getRelative(side);
            if (isFridgeModule(relative)) {
                module = relative;
                break;
            }
        }

        if (module == null) {
            // Not a Fridge door - let vanilla door behaviour happen.
            return;
        }

        e.setCancelled(true);

        if (module.getRelative(BlockFace.UP).getType() != Material.IRON_BLOCK) {
            Gastronomicon.sendMessage(e.getPlayer(), "&cThis Fridge is missing its Iron Block!");
            return;
        }

        final BlockMenu menu = StorageCacheUtils.getMenu(module.getLocation());
        if (menu != null) {
            menu.open(e.getPlayer());
        }
    }

    private boolean isFridgeModule(Block b) {
        final SlimefunItem sfItem = StorageCacheUtils.getSfItem(b.getLocation());
        return sfItem != null && sfItem.getId().equals(FRIDGE_MODULE_ID);
    }

    public static void setup() {
        Bukkit.getPluginManager().registerEvents(new FridgeStructureListener(), Gastronomicon.getInstance());
    }

}
