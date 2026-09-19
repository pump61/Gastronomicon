package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.automatic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import io.github.schntgaispock.gastronomicon.Gastronomicon;
import io.github.schntgaispock.gastronomicon.core.Lang;
import io.github.schntgaispock.gastronomicon.core.slimefun.GastroGroups;
import io.github.schntgaispock.gastronomicon.core.slimefun.GastroStacks;
import io.github.schntgaispock.gastronomicon.util.ChunkPDC;
import io.github.schntgaispock.gastronomicon.util.StringUtil;
import io.github.schntgaispock.gastronomicon.util.item.GastroKeys;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperation;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.AdvancedMenuClickHandler;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

/**
 * A 2-tall electric multiblock (this block + a Glass block directly above it)
 * that grows any {@link GreenHouseCrops}-registered seed/sapling - these can
 * no longer be planted in the world, so this is the only way to turn them
 * into fruit. Feed it a seed, soil and fertilizer; every cycle it consumes 1
 * fertilizer (the seed and soil are reusable) to produce that plant's fruit.
 */
@Getter
@SuppressWarnings("deprecation")
public class GreenHouse extends AContainer {

    // The 3 inputs sit in consecutive rows (1, 2, 3), each with a label glass
    // directly to its left. The status/timer slot sits in the middle row,
    // directly between the input column and the output block, so it's level
    // with the inputs instead of off to the side.
    public static final int SEED_LABEL = 9;
    public static final int SEED_SLOT = 10;
    public static final int SOIL_LABEL = 18;
    public static final int SOIL_SLOT = 19;
    public static final int STATUS_SLOT = 20;
    public static final int FERTILIZER_LABEL = 27;
    public static final int FERTILIZER_SLOT = 28;
    // Shifted one column right of the label/fertilizer column so the green
    // border doesn't crowd the orange progress indicator in STATUS_SLOT.
    public static final int[] OUTPUT_SLOTS = { 13, 14, 15, 22, 23, 24, 31, 32, 33 };
    public static final int[] OUTPUT_BORDER_SLOTS = { 3, 4, 5, 6, 7,
        12, 16,
        21, 25,
        30, 34,
        39, 40, 41, 42, 43 };
    public static final int START_SLOT = 52;
    public static final int STOP_SLOT = 53;
    public static final int GREEN_HOUSE_FERTILIZER_USES = 8;
    public static final int[] BACKGROUND_SLOTS = { 0, 1, 2, 8,
        11, 17,
        26,
        29, 35,
        36, 37, 38, 44,
        45, 46, 47, 48, 49, 50, 51 };

    private static final ItemStack STATUS_IDLE_ITEM = new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " ");

    private final EnergyNetComponentType energyComponentType = EnergyNetComponentType.CONSUMER;
    private final String machineIdentifier = "GN_GREEN_HOUSE";
    private final MachineProcessor<CraftingOperation> machineProcessor = new MachineProcessor<>(this);
    private final ItemStack progressBar = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);

    public GreenHouse(SlimefunItemStack item, int capacity, int energyConsumption, int speed, ItemStack[] recipe) {
        super(GastroGroups.ELECTRIC_MACHINES, item, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);

        setCapacity(capacity);
        setEnergyConsumption(energyConsumption);
        setProcessingSpeed(speed);
        machineProcessor.setProgressBar(progressBar);
        createPreset(this, this::constructMenu);
    }

    @Override
    public void preRegister() {
        super.preRegister();

        addItemHandler(new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                ChunkPDC.set(e.getBlock(), GastroKeys.GREEN_HOUSE_ENABLED, true);
            }
        });
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

                if (!hasGlassRoof(b)) {
                    Gastronomicon.sendMessage(p, "&c" + Lang.get("messages.greenhouse_missing_glass"));
                    return false;
                }

                return item.canUse(p, false)
                    && Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
            }
        };
    }

    @Override
    public int[] getInputSlots() {
        return new int[] { SEED_SLOT, SOIL_SLOT, FERTILIZER_SLOT };
    }

    @Override
    public int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }

    private static boolean hasGlassRoof(Block b) {
        return b.getRelative(BlockFace.UP).getType() == Material.GLASS;
    }

    private static boolean isEnabled(Block b) {
        final Boolean enabled = ChunkPDC.getBool(b, GastroKeys.GREEN_HOUSE_ENABLED);
        return enabled == null || enabled;
    }

    protected void constructMenu(BlockMenuPreset preset) {
        draw(preset, GastroStacks.MENU_BACKGROUND_ITEM, BACKGROUND_SLOTS);
        draw(preset, GastroStacks.MENU_GREEN_OUTPUT_BORDER, OUTPUT_BORDER_SLOTS);
        draw(preset, GastroStacks.MENU_SEED_INPUT, SEED_LABEL);
        draw(preset, GastroStacks.MENU_FERTILE_SOIL_INPUT, SOIL_LABEL);
        draw(preset, GastroStacks.MENU_FERTILIZER_INPUT, FERTILIZER_LABEL);
        draw(preset, STATUS_IDLE_ITEM, STATUS_SLOT);
        draw(preset, GastroStacks.MENU_STOP_BUTTON, STOP_SLOT);
        draw(preset, GastroStacks.MENU_START_BUTTON, START_SLOT);

        preset.addMenuClickHandler(STOP_SLOT, new AdvancedMenuClickHandler() {
            @Override
            public boolean onClick(Player p, int slot, ItemStack cursor, ClickAction action) {
                return false;
            }

            @Override
            public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor, ClickAction action) {
                final BlockMenu menu = (BlockMenu) e.getInventory().getHolder();
                ChunkPDC.set(menu.getLocation().getBlock(), GastroKeys.GREEN_HOUSE_ENABLED, false);
                return false;
            }
        });

        preset.addMenuClickHandler(START_SLOT, new AdvancedMenuClickHandler() {
            @Override
            public boolean onClick(Player p, int slot, ItemStack cursor, ClickAction action) {
                return false;
            }

            @Override
            public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor, ClickAction action) {
                final BlockMenu menu = (BlockMenu) e.getInventory().getHolder();
                ChunkPDC.set(menu.getLocation().getBlock(), GastroKeys.GREEN_HOUSE_ENABLED, true);
                return false;
            }
        });

        for (final int i : OUTPUT_SLOTS) {
            preset.addMenuClickHandler(i, new AdvancedMenuClickHandler() {
                @Override
                public boolean onClick(Player p, int slot, ItemStack cursor, ClickAction action) {
                    return false;
                }

                @Override
                public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor,
                    ClickAction action) {
                    return cursor == null || cursor.getType() == Material.AIR;
                }
            });
        }
    }

    private void draw(BlockMenuPreset preset, ItemStack item, int... slots) {
        for (int slot : slots) {
            preset.addItem(slot, item, ChestMenuUtils.getEmptyClickHandler());
        }
    }

    protected MachineRecipe findNextRecipe(BlockMenu menu) {
        final ItemStack seedStack = menu.getItemInSlot(SEED_SLOT);
        if (seedStack == null) {
            return null;
        }

        final SlimefunItem seedItem = SlimefunItem.getByItem(seedStack);
        if (seedItem == null) {
            return null;
        }

        final ItemStack fruit = GreenHouseCrops.getFruit(seedItem.getId());
        if (fruit == null) {
            return null;
        }

        final ItemStack soil = menu.getItemInSlot(SOIL_SLOT);
        if (soil == null || (soil.getType() != Material.DIRT && soil.getType() != Material.GRASS_BLOCK
            && soil.getType() != Material.FARMLAND)) {
            return null;
        }

        final ItemStack fertilizer = menu.getItemInSlot(FERTILIZER_SLOT);
        if (fertilizer == null) {
            return null;
        }

        final SlimefunItem infernalBonemeal = SlimefunItem.getById(SlimefunItems.INFERNAL_BONEMEAL.getItemId());
        final boolean isInfernal = infernalBonemeal != null && infernalBonemeal.isItem(fertilizer);

        if (fertilizer.getType() != Material.BONE_MEAL && !isInfernal && !isSlimefunFertilizer(fertilizer)) {
            return null;
        }

        if (isInfernal) {
            fruit.setAmount(fruit.getAmount() * 2);
        }

        boolean hasRoom = false;
        for (final int slot : OUTPUT_SLOTS) {
            final ItemStack existing = menu.getItemInSlot(slot);
            if (existing == null || existing.getType() == Material.AIR
                || (existing.isSimilar(fruit) && existing.getAmount() < existing.getMaxStackSize())) {
                hasRoom = true;
                break;
            }
        }
        if (!hasRoom) {
            return null;
        }

        return new MachineRecipe(300 / getSpeed(), new ItemStack[] { seedStack, soil, fertilizer },
            new ItemStack[] { fruit });
    }

    /**
     * Modified from AContainer#tick
     */
    @Override
    protected void tick(Block b) {
        final BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
        if (inv == null) {
            return;
        }

        if (!isEnabled(b)) {
            inv.replaceExistingItem(STATUS_SLOT, GastroStacks.MENU_GREENHOUSE_STOPPED);
            return;
        }

        if (!hasGlassRoof(b)) {
            inv.replaceExistingItem(STATUS_SLOT, GastroStacks.MENU_NO_GREENHOUSE_ROOF);
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

                    for (ItemStack output : currentOperation.getResults()) {
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
                consumeFertilizer(inv);

                currentOperation = new CraftingOperation(next);
                getMachineProcessor().startOperation(b, currentOperation);
                getMachineProcessor().updateProgressBar(inv, STATUS_SLOT, currentOperation);
            }
        }
    }

    // Regular/Infernal Bonemeal are consumed whole every cycle, like before.
    // A Slimefun Fertilizer is far more potent - it's only consumed once its
    // uses (tracked on the ItemStack itself) run out, lasting several cycles.
    private void consumeFertilizer(BlockMenu menu) {
        final ItemStack fertilizer = menu.getItemInSlot(FERTILIZER_SLOT);
        if (fertilizer == null) {
            return;
        }

        if (!isSlimefunFertilizer(fertilizer)) {
            menu.consumeItem(FERTILIZER_SLOT);
            return;
        }

        final int usesLeft = getFertilizerUses(fertilizer) - 1;
        if (usesLeft > 0) {
            final ItemStack updated = fertilizer.clone();
            setFertilizerUses(updated, usesLeft);
            menu.replaceExistingItem(FERTILIZER_SLOT, updated);
            return;
        }

        menu.consumeItem(FERTILIZER_SLOT);

        // If there's another one behind it in the same stack, it's a fresh
        // unit - reset its displayed uses back to full.
        final ItemStack remaining = menu.getItemInSlot(FERTILIZER_SLOT);
        if (remaining != null && isSlimefunFertilizer(remaining)) {
            final ItemStack refreshed = remaining.clone();
            setFertilizerUses(refreshed, GREEN_HOUSE_FERTILIZER_USES);
            menu.replaceExistingItem(FERTILIZER_SLOT, refreshed);
        }
    }

    // Every "can of fertilizer" Slimefun offers - the generic one plus every
    // crop-specific variant - all work the same way in the Green House.
    private static final SlimefunItemStack[] SLIMEFUN_FERTILIZERS = {
        SlimefunItems.FERTILIZER,
        SlimefunItems.WHEAT_FERTILIZER,
        SlimefunItems.CARROT_FERTILIZER,
        SlimefunItems.POTATO_FERTILIZER,
        SlimefunItems.SEEDS_FERTILIZER,
        SlimefunItems.BEETROOT_FERTILIZER,
        SlimefunItems.MELON_FERTILIZER,
        SlimefunItems.APPLE_FERTILIZER,
        SlimefunItems.SWEET_BERRIES_FERTILIZER,
        SlimefunItems.KELP_FERTILIZER,
        SlimefunItems.COCOA_FERTILIZER,
        SlimefunItems.SEAGRASS_FERTILIZER,
    };

    private static boolean isSlimefunFertilizer(ItemStack item) {
        for (final SlimefunItemStack stack : SLIMEFUN_FERTILIZERS) {
            final SlimefunItem sfItem = SlimefunItem.getById(stack.getItemId());
            if (sfItem != null && sfItem.isItem(item)) {
                return true;
            }
        }
        return false;
    }

    private static int getFertilizerUses(ItemStack item) {
        final Integer uses = item.getItemMeta().getPersistentDataContainer()
            .get(GastroKeys.GREEN_HOUSE_FERTILIZER_USES, PersistentDataType.INTEGER);
        return uses == null ? GREEN_HOUSE_FERTILIZER_USES : uses;
    }

    private static void setFertilizerUses(ItemStack item, int uses) {
        final ItemMeta meta = item.getItemMeta();
        final boolean hadUsesTag = meta.getPersistentDataContainer()
            .has(GastroKeys.GREEN_HOUSE_FERTILIZER_USES, PersistentDataType.INTEGER);
        meta.getPersistentDataContainer().set(GastroKeys.GREEN_HOUSE_FERTILIZER_USES, PersistentDataType.INTEGER,
            uses);

        final List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (hadUsesTag && !lore.isEmpty()) {
            lore.remove(lore.size() - 1);
        }
        lore.add(StringUtil.formatColors(Lang.get("menu.green_house_fertilizer_uses")
            .replace("{uses}", String.valueOf(uses))
            .replace("{max}", String.valueOf(GREEN_HOUSE_FERTILIZER_USES))));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

}
