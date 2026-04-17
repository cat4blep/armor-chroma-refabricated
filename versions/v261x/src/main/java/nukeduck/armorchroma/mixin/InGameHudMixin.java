package nukeduck.armorchroma.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import nukeduck.armorchroma.ArmorChroma;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla armor rendering with the mod's
 */
@Mixin(Gui.class)
public abstract class InGameHudMixin {

    /**
     * Mojmap name: LINE_HEIGHT
     */
    @Shadow @Final private static int LINE_HEIGHT;

    /**
     * Replaces the vanilla armor bar
     */
    @Inject(method = "extractArmor", at = @At("HEAD"), cancellable = true)
    private static void onBeforeRenderArmor(GuiGraphicsExtractor context, Player player, int top, int heartRows, int heartRowsSpacing, int left, CallbackInfo info) {
        if (ArmorChroma.config.isEnabled()) {
            info.cancel();
            top -= (heartRows - 1) * heartRowsSpacing + LINE_HEIGHT;
            ArmorChroma.GUI.draw(context, left, top);
        }
    }

}
