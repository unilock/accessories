package io.wispforest.accessories.criteria;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.data.SlotGroupLoader;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class AccessoryChangedCriterion extends SimpleCriterionTrigger<AccessoryChangedCriterion.Conditions> {

    public void trigger(ServerPlayer player, ItemStack accessory, SlotReference reference, Boolean cosmetic) {
        this.trigger(player, conditions -> {
            return conditions.itemPredicates().map(predicates -> predicates.stream().allMatch(predicate -> predicate.matches(accessory))).orElse(true)
                    && conditions.groups().flatMap(groups -> SlotGroupLoader.INSTANCE.findGroup(false, reference.slotName()).map(group -> groups.stream().noneMatch(s -> s.equals(group.name())))).orElse(true)
                    && conditions.slots().map(slots -> slots.stream().noneMatch(reference.slotName()::equals)).orElse(true)
                    && conditions.indices().map(indices -> indices.stream().noneMatch(index -> index == reference.slot())).orElse(true)
                    && conditions.cosmetic().map(isCosmetic -> isCosmetic && cosmetic).orElse(true);
        });
    }

    @Override
    public @NotNull Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public record Conditions(
            Optional<ContextAwarePredicate> player,
            Optional<List<ItemPredicate>> itemPredicates,
            Optional<List<String>> groups,
            Optional<List<String>> slots,
            Optional<List<Integer>> indices,
            Optional<Boolean> cosmetic
    ) implements SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(Conditions::player),
                ExtraCodecs.strictOptionalField(ItemPredicate.CODEC.listOf(), "items").forGetter(Conditions::itemPredicates),
                ExtraCodecs.strictOptionalField(Codec.STRING.listOf(), "groups").forGetter(Conditions::groups),
                ExtraCodecs.strictOptionalField(Codec.STRING.listOf(), "slots").forGetter(Conditions::slots),
                ExtraCodecs.strictOptionalField(Codec.INT.listOf(), "indices").forGetter(Conditions::indices),
                ExtraCodecs.strictOptionalField(Codec.BOOL, "cosmetic").forGetter(Conditions::cosmetic)
                ).apply(instance, Conditions::new));
    }
}