package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.manual;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;

import io.github.schntgaispock.gastronomicon.Gastronomicon;
import io.github.schntgaispock.gastronomicon.core.Lang;
import io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.WaterTank;
import io.github.schntgaispock.gastronomicon.core.slimefun.recipes.GastroRecipeType;
import io.github.schntgaispock.gastronomicon.util.ChunkPDC;
import io.github.schntgaispock.gastronomicon.util.item.GastroKeys;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;

@Getter
public class Fermenter extends GastroWorkstation implements WaterTank {

    private static final int RECIPE_BOOK_SLOT = 52;
    private static final int WATER_LEVEL_SLOT = 44;

    private final int capacity;
    private final int mbPerCraft;

    public Fermenter(SlimefunItemStack item, ItemStack[] recipe, int capacity, int mbPerCraft) {
        super(item, recipe);

        this.capacity = capacity;
        this.mbPerCraft = mbPerCraft;
    }

    @Override
    public int getWaterCapacity() {
        return capacity;
    }

    @Override
    public NamespacedKey getWaterKey() {
        return GastroKeys.FERMENTER_WATER;
    }

    @Override
    protected void onBreak(BlockBreakEvent e, BlockMenu menu) {
        super.onBreak(e, menu);
        ChunkPDC.remove(e.getBlock(), GastroKeys.FERMENTER_WATER);
    }

    @Override
    protected void onPlace(BlockPlaceEvent e, Block b) {
        super.onPlace(e, b);
        ChunkPDC.set(b, GastroKeys.FERMENTER_WATER, 0);
    }

    @Override
    protected void setup(BlockMenuPreset preset) {
        super.setup(preset);
        addRecipeBookButton(preset, RECIPE_BOOK_SLOT, Lang.get("menu.recipe_book_title.fermenter"));
    }

    @Override
    protected void onNewInstance(BlockMenu menu, Block b) {
        super.onNewInstance(menu, b);

        menu.addMenuOpeningHandler(player -> updateWaterLevelDisplay(menu, b));
    }

    private void updateWaterLevelDisplay(BlockMenu menu, Block b) {
        final int water = ChunkPDC.getOrCreateDefault(b, GastroKeys.FERMENTER_WATER, 0);
        menu.replaceExistingItem(WATER_LEVEL_SLOT, buildWaterLevelItem(water, getCapacity()));
    }

    private static ItemStack buildWaterLevelItem(int water, int capacity) {
        return new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            "&b" + Lang.get("menu.water_level.name")
                .replace("{water}", String.valueOf(water))
                .replace("{capacity}", String.valueOf(capacity)),
            Lang.getList("menu.water_level.lore").toArray(new String[0]));
    }

    @Override
    public GastroRecipeType getGastroRecipeType() {
        return GastroRecipeType.FERMENTER;
    }

    @Override
    protected boolean canCraft(BlockMenu menu, Block b, Player p, boolean sendMessage) {
        final int water = ChunkPDC.getOrCreateDefault(b, GastroKeys.FERMENTER_WATER, 0);
        if (water < getMbPerCraft()) {
            Gastronomicon.sendMessage(p, "&e" + Lang.get("messages.not_enough_water"));
            return false;
        }

        return true;
    }

    @Override
    protected void onSuccessfulCraft(Block b) {
        final int water = ChunkPDC.getOrCreateDefault(b, GastroKeys.FERMENTER_WATER, 0);
        ChunkPDC.set(b, GastroKeys.FERMENTER_WATER, water - getMbPerCraft());

        final BlockMenu menu = StorageCacheUtils.getMenu(b.getLocation());
        if (menu != null) {
            updateWaterLevelDisplay(menu, b);
        }
    }

}
