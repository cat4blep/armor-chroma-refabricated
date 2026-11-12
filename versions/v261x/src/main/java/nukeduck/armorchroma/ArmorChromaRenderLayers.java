package nukeduck.armorchroma;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import static nukeduck.armorchroma.ArmorChroma.TEXTURE_SIZE;

public class ArmorChromaRenderLayers {

    private static final BlendFunction MASKED_ICON_BLEND_FUNCTION = new BlendFunction(SourceFactor.DST_COLOR, DestFactor.ZERO);

    /**
     * Based on {@link RenderPipelines#GUI_TEXTURED_OVERLAY}.
     */
    private static final RenderPipeline MASKED_ICON_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(ArmorChroma.MODID, "pipeline/masked_icon"))
                    .withColorTargetState(new ColorTargetState(MASKED_ICON_BLEND_FUNCTION))
                    .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false))
                    .build());

    public static void blitIcon(GuiGraphicsExtractor context, Identifier texture, int x, int y,
                                int u, int v, int width, int height, int color, boolean masked) {
        RenderPipeline pipeline = masked ? MASKED_ICON_PIPELINE : RenderPipelines.GUI_TEXTURED;
        context.blit(pipeline, texture, x, y, u, v, width, height, TEXTURE_SIZE, TEXTURE_SIZE, color);
    }
}
