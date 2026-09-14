package io.github.schntgaispock.gastronomicon.core.slimefun.items.workstations.manual;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.schntgaispock.gastronomicon.Gastronomicon;
import io.github.schntgaispock.gastronomicon.core.slimefun.recipes.GastroRecipeType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;

@Getter
public class Freezer extends GastroWorkstation implements EnergyNetComponent {

    private final int capacity;
    private final int energyPerUse;

    public Freezer(SlimefunItemStack item, ItemStack[] recipe, int capacity, int energyPerUse) {
        super(item, recipe);

        this.capacity = capacity;
        this.energyPerUse = energyPerUse;
    }

    private static final int RECIPE_BOOK_SLOT = 52;

    @Override
    protected void setup(BlockMenuPreset preset) {
        super.setup(preset);
        addRecipeBookButton(preset, RECIPE_BOOK_SLOT, "Freezer Recipes");
    }

    @Override
    public GastroRecipeType getGastroRecipeType() {
        return GastroRecipeType.FREEZER;
    }

    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    protected boolean canCraft(BlockMenu menu, Block b, Player p, boolean sendMessage) {
        final int charge = getCharge(b.getLocation());
        if (charge < getEnergyPerUse()) {
            Gastronomicon.sendMessage(p, "&eNot enough energy!");
            return false;
        }

        return true;
    }

    @Override
    protected void onSuccessfulCraft(Block b) {
        final int charge = getCharge(b.getLocation());
        setCharge(b.getLocation(), charge - getEnergyPerUse());
    }

}
