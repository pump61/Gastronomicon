package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.automatic;

import java.util.Arrays;
import java.util.List;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;

import io.github.mooy1.infinitylib.core.AddonConfig;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import io.github.schntgaispock.gastronomicon.Gastronomicon;
import io.github.schntgaispock.gastronomicon.core.Lang;
import io.github.schntgaispock.gastronomicon.api.recipes.GastroRecipe;
import io.github.schntgaispock.gastronomicon.core.slimefun.GastroGroups;
import io.github.schntgaispock.gastronomicon.core.slimefun.GastroStacks;
import io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.WaterTank;
import io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.manual.GastroWorkstation;
import io.github.schntgaispock.gastronomicon.core.slimefun.recipes.GastroRecipeType;
import io.github.schntgaispock.gastronomicon.util.ChunkPDC;
import io.github.schntgaispock.gastronomicon.util.NumberUtil;
import io.github.schntgaispock.gastronomicon.util.item.GastroKeys;
import io.github.schntgaispock.gastronomicon.util.item.ItemUtil;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperation;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;

/**
 * The electric counterpart to the Culinary Workbench: same recipe grid,
 * container/tool slots and recipe book, but instead of an instant "Click to
 * Craft" button it has a Start/Stop toggle at the same slot - Start consumes
 * the ingredients and begins a timed brew (drawing power like an Electric
 * Kitchen), Stop cancels it early without refunding what was consumed.
 * <br>
 * <br>
 * No recipes are registered for {@link GastroRecipeType#COFFEE_MACHINE} yet,
 * so this is infrastructure waiting on actual coffee recipes to be added.
 */
@Getter
@SuppressWarnings("deprecation")
public class CoffeeMachine extends GastroWorkstation
    implements EnergyNetComponent, MachineProcessHolder<CraftingOperation>, WaterTank {

    private static final int RECIPE_BOOK_SLOT = 52;
    private static final int WATER_LEVEL_SLOT = 44;

    // GastroRecipe has no per-recipe brew time or water cost, so every recipe
    // currently takes the same fixed time and water. Revisit this if that
    // ever needs to vary.
    private static final int BREW_TICKS = 2 * 20;

    private final EnergyNetComponentType energyComponentType = EnergyNetComponentType.CONSUMER;
    private final MachineProcessor<CraftingOperation> machineProcessor = new MachineProcessor<>(this);
    private final int capacity;
    private final int energyConsumption;
    private final int waterCapacity;
    private final int mbPerBrew;

    public CoffeeMachine(SlimefunItemStack item, int capacity, int energyConsumption, int waterCapacity,
        int mbPerBrew, ItemStack[] recipe) {
        super(GastroGroups.ELECTRIC_MACHINES, item, recipe);

        this.capacity = capacity;
        this.energyConsumption = energyConsumption;
        this.waterCapacity = waterCapacity;
        this.mbPerBrew = mbPerBrew;
        // Reuses the "Click to Stop" icon as the progress bar base so its
        // name stays meaningful while its amount conveys brewing progress.
        machineProcessor.setProgressBar(GastroStacks.MENU_STOP_BUTTON);
    }

    @Override
    public NamespacedKey getWaterKey() {
        return GastroKeys.COFFEE_MACHINE_WATER;
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {
            @Override
            public void tick(Block b, SlimefunItem sf, Config data) {
                CoffeeMachine.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                // Must run on the main thread: tick() touches the menu's inventory.
                return true;
            }
        });
    }

    @Override
    protected void onPlace(BlockPlaceEvent e, Block b) {
        super.onPlace(e, b);
        ChunkPDC.set(b, getWaterKey(), 0);
    }

    @Override
    protected void setup(BlockMenuPreset preset) {
        super.setup(preset);
        preset.addItem(CRAFT_BUTTON_SLOT, GastroStacks.MENU_START_BUTTON, ChestMenuUtils.getEmptyClickHandler());
        addRecipeBookButton(preset, RECIPE_BOOK_SLOT, Lang.get("menu.recipe_book_title.coffee_machine"));
    }

    @Override
    public GastroRecipeType getGastroRecipeType() {
        return GastroRecipeType.COFFEE_MACHINE;
    }

    @Override
    protected boolean canCraft(BlockMenu menu, Block b, Player p, boolean sendMessage) {
        final int water = ChunkPDC.getOrCreateDefault(b, getWaterKey(), 0);
        if (water < mbPerBrew) {
            if (sendMessage) {
                Gastronomicon.sendMessage(p, "&e" + Lang.get("messages.not_enough_water"));
            }
            return false;
        }

        return true;
    }

    private void updateWaterLevelDisplay(BlockMenu menu, Block b) {
        final int water = ChunkPDC.getOrCreateDefault(b, getWaterKey(), 0);
        menu.replaceExistingItem(WATER_LEVEL_SLOT, new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            "&b" + Lang.get("menu.water_level.name")
                .replace("{water}", String.valueOf(water))
                .replace("{capacity}", String.valueOf(waterCapacity)),
            Lang.getList("menu.water_level.lore").toArray(new String[0])));
    }

    @Override
    protected void onNewInstance(BlockMenu menu, Block b) {
        // Deliberately not calling super.onNewInstance(): that wires up the
        // instant "Click to Craft" button, which this machine replaces with
        // the Start/Stop toggle below.
        menu.addMenuOpeningHandler(player -> updateWaterLevelDisplay(menu, b));

        menu.addMenuClickHandler(CRAFT_BUTTON_SLOT, (player, slot, item, action) -> {
            if (machineProcessor.getOperation(b) != null) {
                machineProcessor.endOperation(b);
                menu.replaceExistingItem(CRAFT_BUTTON_SLOT, GastroStacks.MENU_START_BUTTON);
                return false;
            }

            startBrewing(menu, b, player);
            return false;
        });
    }

    private void startBrewing(BlockMenu menu, Block b, Player player) {
        if (!canCraft(menu, b, player, true)) {
            return;
        }

        boolean hasRoom = false;
        for (final int s : getOutputSlots()) {
            final ItemStack stack = menu.getItemInSlot(s);
            if (stack == null || stack.getType() == Material.AIR) {
                hasRoom = true;
                break;
            }
        }
        if (!hasRoom) {
            return;
        }

        final ItemStack[] ingredients = Arrays.stream(getInputSlots()).mapToObj(s -> {
            final ItemStack i = menu.getItemInSlot(s);
            return i == null ? null : i.asOne();
        }).toArray(ItemStack[]::new);
        final List<ItemStack> containers = Arrays.stream(getContainerSlots()).mapToObj(s -> {
            final ItemStack i = menu.getItemInSlot(s);
            return i == null ? null : i.asOne();
        }).toList();
        final List<ItemStack> tools = Arrays.stream(getToolSlots()).mapToObj(s -> {
            final ItemStack i = menu.getItemInSlot(s);
            return i == null ? null : i.asOne();
        }).toList();

        final GastroRecipe recipe = findRecipe(ingredients, containers, tools, player, menu);
        if (recipe == null) {
            Gastronomicon.sendMessage(player, "&e" + Lang.get("messages.unknown_recipe"));
            return;
        }

        // Consume the ingredients and container up front, same as the Culinary
        // Workbench does on a successful craft. If the machine is stopped
        // early or broken mid-brew, these are not refunded.
        Arrays.stream(getInputSlots()).forEach(s -> {
            final ItemStack i = menu.getItemInSlot(s);
            if (i != null) {
                ItemUtil.consumeItem(i, 1, true).ifPresent(mat ->
                    player.getOpenInventory().getTopInventory().setItem(s, new ItemStack(mat)));
            }
        });
        for (final int containerSlot : getContainerSlots()) {
            final ItemStack i = menu.getItemInSlot(containerSlot);
            if (i != null && recipe.getInputs().getContainer().matches(i)) {
                i.subtract();
                break;
            }
        }

        final int water = ChunkPDC.getOrCreateDefault(b, getWaterKey(), 0);
        ChunkPDC.set(b, getWaterKey(), water - mbPerBrew);
        updateWaterLevelDisplay(menu, b);

        machineProcessor.startOperation(b, new CraftingOperation(ingredients, resolveOutputs(recipe, player), BREW_TICKS));
    }

    /**
     * Picks the actual result(s) of a recipe: for recipes with a "perfect"
     * variant (regular + perfect item, followed by any items to return),
     * rolls a chance based on the player's proficiency to pick the perfect
     * version instead - same system the Culinary Workbench's instant-craft
     * button uses. Returns exactly the items that should end up in the
     * output slots.
     */
    private ItemStack[] resolveOutputs(GastroRecipe recipe, Player player) {
        final ItemStack[] recipeOutputs = recipe.getOutputs();

        final ItemStack output;
        final ItemStack[] toReturn;
        if (recipeOutputs.length > 1 && recipeOutputs[0] instanceof final SlimefunItemStack sfItem) {
            final AddonConfig playerData = Gastronomicon.getInstance().getPlayerData();
            final String proficiencyPath = player.getUniqueId() + ".proficiencies." + sfItem.getItemId();
            final int proficiency = playerData.getInt(proficiencyPath, 0);

            playerData.set(proficiencyPath, proficiency + 1);

            final double perfectProbability = NumberUtil.clamp(NumberUtil.clamp(0, proficiency / 864, 0.25), 0, 1);

            output = recipeOutputs[NumberUtil.randomRound(perfectProbability)];
            toReturn = Arrays.copyOfRange(recipeOutputs, 2, recipeOutputs.length);
        } else {
            output = recipeOutputs[0];
            toReturn = Arrays.copyOfRange(recipeOutputs, 1, recipeOutputs.length);
        }

        final ItemStack[] results = new ItemStack[1 + toReturn.length];
        results[0] = output;
        System.arraycopy(toReturn, 0, results, 1, toReturn.length);
        return results;
    }

    @Override
    protected void onBreak(BlockBreakEvent e, BlockMenu menu) {
        super.onBreak(e, menu);
        machineProcessor.endOperation(menu.getLocation());
        ChunkPDC.remove(e.getBlock(), getWaterKey());
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

    private void pushOutput(BlockMenu menu, Block b, ItemStack item) {
        for (final int s : getOutputSlots()) {
            final ItemStack existing = menu.getItemInSlot(s);
            if (existing == null || existing.getType() == Material.AIR) {
                menu.replaceExistingItem(s, item);
                return;
            } else if (existing.isSimilar(item) && existing.getAmount() < existing.getMaxStackSize()) {
                existing.setAmount(existing.getAmount() + item.getAmount());
                return;
            }
        }

        b.getWorld().dropItemNaturally(b.getLocation(), item);
    }

    private void tick(Block b) {
        final CraftingOperation operation = machineProcessor.getOperation(b);
        if (operation == null) {
            return;
        }

        final BlockMenu menu = StorageCacheUtils.getMenu(b.getLocation());
        if (menu == null) {
            return;
        }

        if (!takeCharge(b.getLocation())) {
            menu.replaceExistingItem(CRAFT_BUTTON_SLOT, GastroStacks.MENU_NOT_ENOUGH_ENERGY);
            return;
        }

        if (!operation.isFinished()) {
            machineProcessor.updateProgressBar(menu, CRAFT_BUTTON_SLOT, operation);
            operation.addProgress(1);
            return;
        }

        for (final ItemStack output : operation.getResults()) {
            pushOutput(menu, b, output.clone());
        }

        b.getWorld().playSound(b.getLocation(), Sound.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1f, 1f);
        menu.replaceExistingItem(CRAFT_BUTTON_SLOT, GastroStacks.MENU_START_BUTTON);
        machineProcessor.endOperation(b);
    }
}
