package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.manual;

import java.util.ArrayList;
import java.util.List;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.schntgaispock.gastronomicon.api.recipes.GastroRecipe;
import io.github.schntgaispock.gastronomicon.api.recipes.MultiStoveRecipe;
import io.github.schntgaispock.gastronomicon.api.recipes.RecipeRegistry;
import io.github.schntgaispock.gastronomicon.api.recipes.components.RecipeComponent;
import io.github.schntgaispock.gastronomicon.core.Lang;
import io.github.schntgaispock.gastronomicon.core.slimefun.GastroStacks;
import io.github.schntgaispock.gastronomicon.core.slimefun.recipes.GastroRecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import lombok.experimental.UtilityClass;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * A simple recipe book, like vanilla's furnace/smoker: lists every recipe
 * known for a {@link GastroRecipeType}, and clicking one shows the
 * ingredients needed to make it. Purely for browsing - it does not move any
 * items for the player.
 */
@UtilityClass
public class RecipeBookMenu {

    private static final int[] LIST_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int PREV_PAGE_SLOT = 45;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int RETURN_TO_MACHINE_SLOT = 47;

    // Mirrors the slot layout of a GastroWorkstation's own crafting UI (same
    // ingredient/container/output/tool borders) so the recipe book looks like
    // a read-only version of the machine the player already knows.
    private static final int[] DETAIL_INGREDIENT_SLOTS = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };
    private static final int[] DETAIL_INGREDIENT_BORDER_SLOTS = { 9, 18, 27 };
    private static final int DETAIL_CONTAINER_SLOT = 15;
    private static final int DETAIL_CONTAINER_BORDER_SLOT = 14;
    private static final int DETAIL_TEMPERATURE_SLOT = 24;
    private static final int DETAIL_OUTPUT_SLOT = 33;
    private static final int DETAIL_OUTPUT_BORDER_SLOT = 32;
    private static final int[] DETAIL_TOOL_SLOTS = { 46, 47, 48, 49, 50, 51 };
    private static final int DETAIL_TOOL_BORDER_SLOT = 45;
    private static final int DETAIL_BACK_SLOT = 52;
    private static final int DETAIL_RETURN_TO_MACHINE_SLOT = 53;

    /**
     * Opens the recipe book for the given machine type.
     *
     * @param p
     *            The player to show it to
     * @param type
     *            Which machine's recipes to list
     * @param title
     *            The inventory title
     * @param machineLocation
     *            The location of the machine block that was open before this,
     *            so a "Return to Machine" button can take the player back to it
     */
    public static void open(Player p, GastroRecipeType type, String title, Location machineLocation) {
        openList(p, type, title, machineLocation, 0);
    }

    private static void openList(Player p, GastroRecipeType type, String title, Location machineLocation, int page) {
        final List<GastroRecipe> recipes = new ArrayList<>(RecipeRegistry.getRecipes(type));
        recipes.sort((a, b) -> a.getOutputs()[0].getType().compareTo(b.getOutputs()[0].getType()));

        final int perPage = LIST_SLOTS.length;
        final int pageCount = Math.max(1, (int) Math.ceil(recipes.size() / (double) perPage));
        final int page_ = Math.max(0, Math.min(page, pageCount - 1));

        final ChestMenu menu = new ChestMenu(title, 54);
        menu.setEmptySlotsClickable(false);
        menu.setPlayerInventoryClickable(true);

        for (int slot = 0; slot < 54; slot++) {
            menu.addItem(slot, GastroStacks.MENU_BACKGROUND_ITEM, (pl, s, item, action) -> false);
        }

        final int start = page_ * perPage;
        for (int i = 0; i < LIST_SLOTS.length; i++) {
            final int recipeIndex = start + i;
            if (recipeIndex >= recipes.size()) {
                break;
            }

            final GastroRecipe recipe = recipes.get(recipeIndex);
            menu.addItem(LIST_SLOTS[i], recipe.getOutputs()[0].clone(), (pl, slot, item, action) -> {
                openDetail(pl, type, title, machineLocation, page_, recipe);
                return false;
            });
        }

        if (page_ > 0) {
            menu.addItem(PREV_PAGE_SLOT, new CustomItemStack(Material.ARROW, "&e" + Lang.get("menu.previous_page")),
                (pl, slot, item, action) -> {
                    openList(pl, type, title, machineLocation, page_ - 1);
                    return false;
                });
        }

        menu.addItem(PAGE_INFO_SLOT,
            new CustomItemStack(Material.BOOK, "&e" + Lang.get("menu.page_info")
                .replace("{page}", String.valueOf(page_ + 1))
                .replace("{total}", String.valueOf(pageCount))),
            (pl, slot, item, action) -> false);

        if (page_ < pageCount - 1) {
            menu.addItem(NEXT_PAGE_SLOT, new CustomItemStack(Material.ARROW, "&e" + Lang.get("menu.next_page")),
                (pl, slot, item, action) -> {
                    openList(pl, type, title, machineLocation, page_ + 1);
                    return false;
                });
        }

        addReturnButton(menu, RETURN_TO_MACHINE_SLOT, machineLocation);

        menu.open(p);
    }

    private static void openDetail(Player p, GastroRecipeType type, String title, Location machineLocation, int page,
        GastroRecipe recipe) {
        final ChestMenu menu = new ChestMenu(title, 54);
        menu.setEmptySlotsClickable(false);
        menu.setPlayerInventoryClickable(true);

        for (int slot = 0; slot < 54; slot++) {
            menu.addItem(slot, GastroStacks.MENU_BACKGROUND_ITEM, (pl, s, item, action) -> false);
        }

        for (final int slot : DETAIL_INGREDIENT_BORDER_SLOTS) {
            menu.addItem(slot, GastroStacks.MENU_INGREDIENT_BORDER, (pl, s, item, action) -> false);
        }
        menu.addItem(DETAIL_CONTAINER_BORDER_SLOT, GastroStacks.MENU_CONTAINER_BORDER, (pl, s, item, action) -> false);
        menu.addItem(DETAIL_OUTPUT_BORDER_SLOT, GastroStacks.MENU_OUTPUT_BORDER, (pl, s, item, action) -> false);
        menu.addItem(DETAIL_TOOL_BORDER_SLOT, GastroStacks.MENU_TOOL_BORDER, (pl, s, item, action) -> false);

        final ItemStack[] ingredients = recipe.getInputs().getDisplayIngredients();
        for (int i = 0; i < DETAIL_INGREDIENT_SLOTS.length && i < ingredients.length; i++) {
            if (ingredients[i] != null && ingredients[i].getType() != Material.AIR) {
                menu.addItem(DETAIL_INGREDIENT_SLOTS[i], ingredients[i], (pl, slot, item, action) -> false);
            }
        }

        final RecipeComponent<?> container = recipe.getInputs().getContainer();
        if (container != null && container != RecipeComponent.EMPTY) {
            menu.addItem(DETAIL_CONTAINER_SLOT, container.getDisplayItem(), (pl, slot, item, action) -> false);
        }

        if (recipe instanceof final MultiStoveRecipe msRecipe) {
            menu.addItem(DETAIL_TEMPERATURE_SLOT, msRecipe.getTemperature().getItem().clone(),
                (pl, slot, item, action) -> false);
        }

        int toolSlot = 0;
        for (final ItemStack tool : recipe.getTools()) {
            if (toolSlot >= DETAIL_TOOL_SLOTS.length) {
                break;
            }
            menu.addItem(DETAIL_TOOL_SLOTS[toolSlot++], tool.clone(), (pl, slot, item, action) -> false);
        }

        menu.addItem(DETAIL_OUTPUT_SLOT, recipe.getOutputs()[0].clone(), (pl, slot, item, action) -> false);

        menu.addItem(DETAIL_BACK_SLOT, new CustomItemStack(Material.BARRIER, "&c" + Lang.get("menu.back")),
            (pl, slot, item, action) -> {
                openList(pl, type, title, machineLocation, page);
                return false;
            });

        addReturnButton(menu, DETAIL_RETURN_TO_MACHINE_SLOT, machineLocation);

        menu.open(p);
    }

    private static void addReturnButton(ChestMenu menu, int slot, Location machineLocation) {
        menu.addItem(slot, new CustomItemStack(Material.LIME_STAINED_GLASS_PANE, "&a" + Lang.get("menu.return_to_machine")),
            (pl, s, item, action) -> {
                final BlockMenu machineMenu = StorageCacheUtils.getMenu(machineLocation);
                if (machineMenu != null) {
                    machineMenu.open(pl);
                }
                return false;
            });
    }

}
