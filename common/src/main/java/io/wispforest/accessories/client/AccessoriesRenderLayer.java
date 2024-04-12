package io.wispforest.accessories.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.util.Calendar;


/**
 * Render layer used to render equipped Accessories for a given {@link LivingEntity}.
 * Such is only applied to {@link LivingEntityRenderer} that have a model that
 * extends {@link HumanoidModel}
 */
public class AccessoriesRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final PostEffectBuffer BUFFER = new PostEffectBuffer();

    public AccessoriesRenderLayer(RenderLayerParent<T, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        //if(true) return;

        var capability = AccessoriesCapability.get(entity);

        if (capability == null) return;

        var calendar = Calendar.getInstance();

        float scale = (float) (1 + (0.5 * (0.75 + (Math.sin(System.currentTimeMillis() / 250d)))));

        for (var entry : capability.getContainers().entrySet()) {
            var container = entry.getValue();

            var accessories = container.getAccessories();
            var cosmetics = container.getCosmeticAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var stack = accessories.getItem(i);
                var cosmeticStack = cosmetics.getItem(i);

                if (!cosmeticStack.isEmpty()) stack = cosmeticStack;

                var renderingLines = AccessoriesScreen.IS_RENDERING_PLAYER && Accessories.getConfig().clientData.showLineRendering;

                if(stack.isEmpty()) {
                    if(!renderingLines) AccessoriesScreen.NOT_VERY_NICE_POSITIONS.remove(container.getSlotName() + i);

                    continue;
                }

                var renderer = AccessoriesRendererRegistry.getRender(stack);

                if(renderer == null || !renderer.shouldRender(container.shouldRender(i))) {
                    if(!renderingLines) AccessoriesScreen.NOT_VERY_NICE_POSITIONS.remove(container.getSlotName() + i);

                    continue;
                }

                poseStack.pushPose();

                var mpoatv = new MPOATVConstructingVertexConsumer();

                MultiBufferSource innerBufferSource = renderType -> {
                    return renderingLines ?
                            VertexMultiConsumer.create(multiBufferSource.getBuffer(renderType), mpoatv) :
                            multiBufferSource.getBuffer(renderType);
                };

                renderer.render(stack, new SlotReference(container.getSlotName(), entity, i), poseStack, getParentModel(), innerBufferSource, light, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);

                if(renderingLines) {
                    float[] colorValues = null;

                    if (AccessoriesScreen.HOVERED_SLOT_TYPE != null && AccessoriesScreen.HOVERED_SLOT_TYPE.equals(container.getSlotName() + i)) {
                        if (calendar.get(Calendar.MONTH) + 1 == 5 && calendar.get(Calendar.DATE) == 16) {
                            var hue = (float) ((System.currentTimeMillis() / 20d % 360d) / 360d);

                            var color = new Color(Mth.hsvToRgb(hue, 1, 1));

                            colorValues = new float[]{color.getRed() / 128f, color.getGreen() / 128f, color.getBlue() / 128f, 1};
                        } else {
                            colorValues = new float[]{scale, scale, scale, 1};
                        }
                    }

                    if(multiBufferSource instanceof MultiBufferSource.BufferSource bufferSource) {
                        if (colorValues != null) {
                            BUFFER.beginWrite(true, GL30.GL_DEPTH_BUFFER_BIT);
                            bufferSource.endBatch();
                            BUFFER.endWrite();

                            BUFFER.draw(colorValues);

                            var frameBuffer = BUFFER.buffer();

                            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, frameBuffer.frameBufferId);
                            GL30.glBlitFramebuffer(0, 0, frameBuffer.width, frameBuffer.height, 0, 0, frameBuffer.width, frameBuffer.height, GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST);
                            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                        } else {
                            bufferSource.endBatch();
                        }
                    }

                    if (AccessoriesScreen.IS_RENDERING_PLAYER) {
                        AccessoriesScreen.NOT_VERY_NICE_POSITIONS.put(container.getSlotName() + i, mpoatv.meanPos());
                    } else {
                        AccessoriesScreen.NOT_VERY_NICE_POSITIONS.remove(container.getSlotName() + i);
                    }
                }

                poseStack.popPose();
            }
        }
    }
}