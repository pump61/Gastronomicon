package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.automatic;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.schntgaispock.gastronomicon.core.slimefun.GastroGroups;
import io.github.schntgaispock.gastronomicon.core.slimefun.GastroStacks;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperation;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

/**
 * An electric machine (same power tier as {@link ElectricKitchen} I) that's
 * fully automatic - no "Start" button, it just processes on its own as long
 * as it's powered. Feed it Dirt, Bone Meal and any hoe (the hoe is a tool,
 * not an ingredient - it wears down exactly like using one by hand, and is
 * never fully consumed by the recipe itself); every 5 seconds it turns one
 * Dirt + one Bone Meal into a Farmland block.
 */
@Getter
@SuppressWarnings("deprecation")
public class SoilFabricator extends AContainer implements EnergyNetComponent {

    // Two inputs stacked in a column, each with a label glass to its left,
    // and the tool slot below them - same visual language as GreenHouse.
    public static final int DIRT_LABEL = 9;
    public static final int DIRT_SLOT = 10;
    public static final int BONEMEAL_LABEL = 18;
    public static final int BONEMEAL_SLOT = 19;
    public static final int STATUS_SLOT = 20;
    public static final int HOE_LABEL = 27;
    public static final int HOE_SLOT = 28;
    // Shifted one column right of the label/tool column so the green
    // border doesn't crowd the orange progress indicator in STATUS_SLOT.
    public static final int[] OUTPUT_SLOTS = { 13, 14, 15, 22, 23, 24, 31, 32, 33 };
    public static final int[] OUTPUT_BORDER_SLOTS = { 3, 4, 5, 6, 7,
        12, 16,
        21, 25,
        30, 34,
        39, 40, 41, 42, 43 };
    public static final int[] BACKGROUND_SLOTS = { 0, 1, 2, 8,
        11, 17,
        26,
        29, 35,
        36, 37, 38, 44,
        45, 46, 47, 48, 49, 50, 51, 52, 53 };

    // Slimefun's own machine ticker calls tick() roughly once per real
    // second (not once per raw 1/20s game tick), so this is ~5 seconds -
    // fixed, this machine has no "speed" upgrade path.
    private static final int CRAFT_TIME_TICKS = 5;

    private static final ItemStack STATUS_IDLE_ITEM = new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " ");

    private final EnergyNetComponentType energyComponentType = EnergyNetComponentType.CONSUMER;
    private final String machineIdentifier = "GN_SOIL_FABRICATOR";
    private final MachineProcessor<CraftingOperation> machineProcessor = new MachineProcessor<>(this);
    private final ItemStack progressBar = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);

    // Same power tier as Electric Kitchen I.
    public SoilFabricator(SlimefunItemStack item, ItemStack[] recipe) {
        super(GastroGroups.ELECTRIC_MACHINES, item, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);

        setCapacity(256);
        setEnergyConsumption(16);
        setProcessingSpeed(1);
        machineProcessor.setProgressBar(progressBar);
        createPreset(this, this::constructMenu);
    }

    @Override
    public void createPreset(SlimefunItem item, String title, Consumer<BlockMenuPreset> setup) {
        new BlockMenuPreset(item.getId(), title) {

            @Override
            public void init() {
                setup.accept(this);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return getInputSlots();
                } else {
                    return getOutputSlots();
                }
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                if (p.hasPermission("slimefun.inventory.bypass")) {
                    return true;
                }

                return item.canUse(p, false)
                    && Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
            }
        };
    }

    @Override
    public int[] getInputSlots() {
        return new int[] { DIRT_SLOT, BONEMEAL_SLOT, HOE_SLOT };
    }

    @Override
    public int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }

    protected void constructMenu(BlockMenuPreset preset) {
        draw(preset, GastroStacks.MENU_BACKGROUND_ITEM, BACKGROUND_SLOTS);
        draw(preset, GastroStacks.MENU_GREEN_OUTPUT_BORDER, OUTPUT_BORDER_SLOTS);
        draw(preset, GastroStacks.MENU_SOIL_INPUT, DIRT_LABEL);
        draw(preset, GastroStacks.MENU_BONEMEAL_INPUT, BONEMEAL_LABEL);
        draw(preset, GastroStacks.MENU_TOOL_BORDER, HOE_LABEL);
        draw(preset, STATUS_IDLE_ITEM, STATUS_SLOT);
    }

    private void draw(BlockMenuPreset preset, ItemStack item, int... slots) {
        for (final int slot : slots) {
            preset.addItem(slot, item, (p, s, i, action) -> false);
        }
    }

    private static boolean isHoe(ItemStack item) {
        return item != null && item.getType().name().endsWith("_HOE");
    }

    protected MachineRecipe findNextRecipe(BlockMenu menu) {
        final ItemStack dirt = menu.getItemInSlot(DIRT_SLOT);
        if (dirt == null || dirt.getType() != Material.DIRT) {
            return null;
        }

        final ItemStack bonemeal = menu.getItemInSlot(BONEMEAL_SLOT);
        if (bonemeal == null || bonemeal.getType() != Material.BONE_MEAL) {
            return null;
        }

        if (!isHoe(menu.getItemInSlot(HOE_SLOT))) {
            return null;
        }

        final ItemStack farmland = new ItemStack(Material.FARMLAND);
        boolean hasRoom = false;
        for (final int slot : OUTPUT_SLOTS) {
            final ItemStack existing = menu.getItemInSlot(slot);
            if (existing == null || existing.getType() == Material.AIR
                || (existing.isSimilar(farmland) && existing.getAmount() < existing.getMaxStackSize())) {
                hasRoom = true;
                break;
            }
        }
        if (!hasRoom) {
            return null;
        }

        return new MachineRecipe(CRAFT_TIME_TICKS, new ItemStack[] { dirt, bonemeal },
            new ItemStack[] { farmland });
    }

    // Wears the hoe down exactly like normal use would (respecting
    // Unbreaking's chance to skip damage) instead of consuming it outright.
    private void damageTool(BlockMenu menu) {
        final ItemStack tool = menu.getItemInSlot(HOE_SLOT);
        if (tool == null) {
            return;
        }

        final ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof final Damageable damageable)) {
            return;
        }

        final int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreakingLevel > 0 && ThreadLocalRandom.current().nextInt(unbreakingLevel + 1) != 0) {
            return;
        }

        final int newDamage = damageable.getDamage() + 1;
        if (newDamage >= tool.getType().getMaxDurability()) {
            menu.consumeItem(HOE_SLOT);
            return;
        }

        damageable.setDamage(newDamage);
        tool.setItemMeta((ItemMeta) damageable);
        menu.replaceExistingItem(HOE_SLOT, tool);
    }

    @Override
    protected void tick(Block b) {
        final BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
        if (inv == null) {
            return;
        }

        CraftingOperation currentOperation = getMachineProcessor().getOperation(b);

        if (currentOperation != null) {
            if (takeCharge(b.getLocation())) {
                if (!currentOperation.isFinished()) {
                    getMachineProcessor().updateProgressBar(inv, STATUS_SLOT, currentOperation);
                    currentOperation.addProgress(1);
                } else {
                    inv.replaceExistingItem(STATUS_SLOT, STATUS_IDLE_ITEM);

                    for (final ItemStack output : currentOperation.getResults()) {
                        inv.pushItem(output.clone(), OUTPUT_SLOTS);
                    }

                    getMachineProcessor().endOperation(b);
                }
            } else {
                inv.replaceExistingItem(STATUS_SLOT, GastroStacks.MENU_NOT_ENOUGH_ENERGY);
            }
        } else {
            final MachineRecipe next = findNextRecipe(inv);

            if (next != null) {
                inv.consumeItem(DIRT_SLOT);
                inv.consumeItem(BONEMEAL_SLOT);
                damageTool(inv);

                currentOperation = new CraftingOperation(next);
                getMachineProcessor().startOperation(b, currentOperation);
                getMachineProcessor().updateProgressBar(inv, STATUS_SLOT, currentOperation);
            }
        }
    }

}
