package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesBasedSlot;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class AccessoriesInternalSlot extends AccessoriesBasedSlot {

    public final int menuIndex;

    public final AccessoriesContainer container;
    public final boolean isCosmetic;

    private Function<AccessoriesInternalSlot, Boolean> isActive = (slot) -> true;
    private Function<AccessoriesInternalSlot, Boolean> isAccessible = (slot) -> true;

    public AccessoriesInternalSlot(int menuIndex, AccessoriesContainer container, boolean isCosmetic, int slot, int x, int y) {
        super(container, isCosmetic ? container.getCosmeticAccessories() : container.getAccessories(), slot, x, y);

        this.menuIndex = menuIndex;

        this.isCosmetic = isCosmetic;
        this.container = container;
    }

    public AccessoriesInternalSlot isActive(Function<AccessoriesInternalSlot, Boolean> isActive){
        this.isActive = isActive;

        return this;
    }

    public AccessoriesInternalSlot isAccessible(Function<AccessoriesInternalSlot, Boolean> isAccessible){
        this.isAccessible = isAccessible;

        return this;
    }

    @Override
    public void set(ItemStack stack) {
        var prevStack = this.getItem();

        super.set(stack);

        // TODO: SHOULD THIS BE HERE?
        if(isCosmetic) {
            var reference = new SlotReference(container.getSlotName(), entity, getContainerSlot());

            AccessoriesAPI.getAccessory(prevStack)
                    .ifPresent(prevAccessory1 -> prevAccessory1.onUnequip(prevStack, reference));

            AccessoriesAPI.getAccessory(stack)
                    .ifPresent(accessory1 -> accessory1.onEquip(stack, reference));
        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.isAccessible.apply(this) && super.mayPlace(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return this.isAccessible.apply(this) && (isCosmetic || super.mayPickup(player));
    }

    @Override
    public boolean allowModification(Player player) {
        return this.isAccessible.apply(this);
    }

    @Override
    public boolean isActive() {
        return this.isActive.apply(this);
    }
}
