package io.wispforest.tclayer.mixin;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.trinkets.api.TrinketConstants;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import dev.emi.trinkets.data.SlotLoader;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(SlotTypeLoader.class)
public abstract class SlotTypeLoaderMixin {

    @Unique private final ResourceLocation EMPTY_TEXTURE = new ResourceLocation(TrinketConstants.MOD_ID, "gui/slots/empty.png");

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;<init>()V", shift = At.Shift.AFTER, ordinal = 2))
    private void injectTrinketSpecificSlots(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci, @Local(name = "builders") HashMap<String, SlotTypeLoader.SlotBuilder> builders){
        for (var groupDataEntry : SlotLoader.INSTANCE.getSlots().entrySet()) {
            var groupData = groupDataEntry.getValue();
            var slots = groupData.slots;

            SlotTypeLoader.SlotBuilder builder;

            for (var entry : slots.entrySet()) {
                var accessoryType = WrappingTrinketsUtils.trinketsToAccessories_Slot(Optional.of(groupDataEntry.getKey()), entry.getKey());
                var slotData = entry.getValue();

                if (builders.containsKey(accessoryType)) {
                    builder = builders.get(accessoryType);

                    var slotsCurrentSize = builder.baseAmount;

                    if(slotsCurrentSize != null && slotData.amount > slotsCurrentSize) {
                        builder.amount(slotData.amount);
                    }
                } else {
                    builder = new SlotTypeLoader.SlotBuilder(accessoryType);

                    if (slotData.amount != -1) builder.amount(slotData.amount);

                    builder.order(slotData.order);

                    var icon = ResourceLocation.tryParse(slotData.icon);

                    if(!EMPTY_TEXTURE.equals(icon)) builder.icon(icon);

                    builder.dropRule(TrinketEnums.convert(TrinketEnums.DropRule.valueOf(slotData.dropRule)));

                    builder.alternativeTranslation("trinkets.slot." + groupDataEntry.getKey() + "." + entry.getKey());

                    builders.put(accessoryType, builder);
                }

                for (String validatorPredicate : slotData.validatorPredicates) {
                    var location = ResourceLocation.tryParse(validatorPredicate);

                    if(location == null) continue;

                    builder.validator(WrappingTrinketsUtils.trinketsToAccessories_Validators(location));
                }
            }
        }
    }
}
