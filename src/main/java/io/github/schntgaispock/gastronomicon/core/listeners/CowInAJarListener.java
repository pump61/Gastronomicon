package io.github.schntgaispock.gastronomicon.core.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.entity.Cow;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import io.github.schntgaispock.gastronomicon.Gastronomicon;
import io.github.schntgaispock.gastronomicon.core.slimefun.GastroStacks;

/**
 * Dropping an anvil on an (adult) cow turns it into a {@link
 * io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.manual.CowInAJar},
 * exactly like Cooking for Blockheads.
 */
public class CowInAJarListener implements Listener {

    public static void setup() {
        Bukkit.getPluginManager().registerEvents(new CowInAJarListener(), Gastronomicon.getInstance());
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnvilDropOnCow(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof FallingBlock fallingBlock)) {
            return;
        }

        if (!Tag.ANVIL.isTagged(fallingBlock.getBlockData().getMaterial())) {
            return;
        }

        if (!(e.getEntity() instanceof Cow cow) || !cow.isAdult()) {
            return;
        }

        if (cow.getHealth() > e.getFinalDamage()) {
            return;
        }

        e.setCancelled(true);

        final Location loc = cow.getLocation();
        cow.remove();
        loc.getWorld().dropItemNaturally(loc, GastroStacks.COW_IN_A_JAR.clone());
        loc.getWorld().playSound(loc, Sound.ENTITY_COW_DEATH, SoundCategory.NEUTRAL, 1f, 1f);
    }

}
