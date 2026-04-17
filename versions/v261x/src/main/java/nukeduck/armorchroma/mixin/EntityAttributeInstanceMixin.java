package nukeduck.armorchroma.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import nukeduck.armorchroma.EntityAttributeInstanceAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Exposes the unclamped value of the attribute
 */
@Mixin(AttributeInstance.class)
public abstract class EntityAttributeInstanceMixin implements EntityAttributeInstanceAccess {

    @Shadow public abstract double getValue();

    @Unique private double unclampedValue;


    @Inject(method = "calculateValue",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;sanitizeValue(D)D"))
    private void onComputeValue(CallbackInfoReturnable<Double> info, @Local(ordinal = 1) double value) {
        unclampedValue = value;
    }

    @Override
    public double armorChroma_getUnclampedValue() {
        getValue(); // Computes the unclamped value again if needed
        return unclampedValue;
    }
}
