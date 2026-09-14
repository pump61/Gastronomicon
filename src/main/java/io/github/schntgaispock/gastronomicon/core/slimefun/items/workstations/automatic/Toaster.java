package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.automatic;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import io.github.schntgaispock.gastronomicon.core.slimefun.GastroGroups;
import io.github.schntgaispock.gastronomicon.core.slimefun.GastroStacks;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperation;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;

/**
 * A simple electric appliance with no inventory of its own - right-click it
 * while holding Sliced Bread (from the Cutting Board) to feed it, wait for it
 * to toast, then collect the Toasted Bread it drops next to itself.
 */
@Getter
@SuppressWarnings("deprecation")
public class Toaster extends SlimefunItem implements EnergyNetComponent, MachineProcessHolder<CraftingOperation> {

    private static final int TOAST_TICKS = 2 * 20;
    private static final int PARTICLE_PERIOD = 5;
    private static final int SOUND_PERIOD = 40;

    private final EnergyNetComponentType energyComponentType = EnergyNetComponentType.CONSUMER;
    private final MachineProcessor<CraftingOperation> machineProcessor = new MachineProcessor<>(this);
    private final int capacity;
    private final int energyConsumption;

    public Toaster(SlimefunItemStack item, int capacity, int energyConsumption, ItemStack[] recipe) {
        super(GastroGroups.ELECTRIC_MACHINES, item, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);

        this.capacity = capacity;
        this.energyConsumption = energyConsumption;
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {
            @Override
            public void tick(Block b, SlimefunItem sf, Config data) {
                Toaster.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                // Must run on the main thread: tick() plays sounds, spawns
                // particles and drops items, none of which are thread-safe.
                return true;
            }
        });

        addItemHandler((BlockUseHandler) e -> {
            if (e.getClickedBlock().isEmpty() || !canUse(e.getPlayer(), true)) {
                return;
            }

            final ItemStack inHand = e.getItem();
            final SlimefunItem heldItem = SlimefunItem.getByItem(inHand);
            if (heldItem == null || !heldItem.getId().equals(GastroStacks.SLICED_BREAD.getItemId())) {
                return;
            }

            final Block b = e.getClickedBlock().get();
            if (machineProcessor.getOperation(b) != null) {
                return;
            }

            e.cancel();

            final CraftingOperation operation = new CraftingOperation(
                new ItemStack[] { GastroStacks.SLICED_BREAD.clone() },
                new ItemStack[] { GastroStacks.TOASTED_BREAD.clone() },
                TOAST_TICKS);

            if (machineProcessor.startOperation(b, operation)) {
                inHand.subtract(1);
                b.getWorld().playSound(b.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 1f, 1f);
            }
        });

        addItemHandler(new SimpleBlockBreakHandler() {
            @Override
            public void onBlockBreak(Block b) {
                machineProcessor.endOperation(b);
            }
        });
    }

    private boolean takeCharge(Location l) {
        if (isChargeable()) {
            final long charge = getChargeLong(l);
            if (charge < getEnergyConsumption()) {
                return false;
            }

            setCharge(l, charge - getEnergyConsumption());
            return true;
        }

        return true;
    }

    private void tick(Block b) {
        final CraftingOperation operation = machineProcessor.getOperation(b);
        if (operation == null) {
            return;
        }

        // Best-effort energy draw - toasting always takes the same 5 seconds
        // even if the power network can't keep up with the full draw every tick.
        takeCharge(b.getLocation());

        if (!operation.isFinished()) {
            operation.addProgress(1);

            final int progress = operation.getProgress();
            if (progress % PARTICLE_PERIOD == 0) {
                b.getWorld().spawnParticle(
                    Particle.SMOKE,
                    b.getLocation().add(0.5, 1.1, 0.5),
                    3, 0.15, 0.05, 0.15, 0.01);
            }
            if (progress % SOUND_PERIOD == 0) {
                b.getWorld().playSound(b.getLocation(), Sound.BLOCK_SMOKER_SMOKE, SoundCategory.BLOCKS, 0.6f, 1f);
            }

            return;
        }

        b.getWorld().playSound(b.getLocation(), Sound.BLOCK_TRIPWIRE_ATTACH, SoundCategory.BLOCKS, 1f, 2f);

        for (ItemStack result : operation.getResults()) {
            // dropItem (not dropItemNaturally) so it pops straight up above
            // the block instead of scattering to a random side.
            final Item dropped = b.getWorld().dropItem(b.getLocation().add(0.5, 1, 0.5), result.clone());
            dropped.setVelocity(new Vector(0, 0.2, 0));
        }

        machineProcessor.endOperation(b);
    }
}
