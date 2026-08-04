package portalgun.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.core.CHelper;
import qouteall.q_misc_util.my_util.DQuaternion;
import portalgun.PortalGunMod;
import portalgun.client.PortalgunClient;
import portalgun.client.renderer.models.PortalOverlayModel;
import portalgun.entities.CustomPortal;

import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
import static portalgun.PortalGunMod.id;

public class CustomPortalEntityRenderer extends EntityRenderer<CustomPortal> {
    private final PortalOverlayModel model;
    
    public CustomPortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new PortalOverlayModel(context.bakeLayer(PortalgunClient.OVERLAY_MODEL_LAYER));
    }
    
    @Override
    public void render(
        @NotNull CustomPortal entity, float yaw, float tickDelta,
        @NotNull PoseStack matrices, @NotNull MultiBufferSource vertexConsumers, int light
    ) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        
        // don't render overlay from back side
        if (!entity.isInFrontOfPortal(CHelper.getCurrentCameraPos())) {
            return;
        }
        
        matrices.pushPose();
        // It's a small hack to get rid of Minecraft's lighting system and make it always bright.
        // MC use the normal with some global light direction to calculate the facing light (not block light).
        // It even affects emissive layer.
        // Change the normal matrix to make the facing light always brightest.
        matrices.last().normal().rotate(DQuaternion.rotationByDegrees(new Vec3(1, 0, 0), -90).toMcQuaternion());
        matrices.last().pose().rotate(entity.getOrientationRotation().toMcQuaternion());
        
        matrices.translate(0, 0, PortalGunMod.portalOverlayOffset);
        
        int color = entity.getColor();

        int r = (color & 0xFF0000) >> 16;
        int g = (color & 0xFF00) >> 8;
        int b = color & 0xFF;
        int packedColor = (0xFF << 24) | (r << 16) | (g << 8) | b;

        // 1.21 Model#renderToBuffer takes a packed ARGB int instead of float channels
        this.model.renderToBuffer(
            matrices,
            vertexConsumers.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(entity))),
            LightTexture.pack(15, 15),
            NO_OVERLAY,
            packedColor
        );
        matrices.popPose();
    }
    
    @Override
    public @NotNull ResourceLocation getTextureLocation(CustomPortal entity) {
        return entity.isVisible() ? OVERLAY_FRAME : OVERLAY_FILLED;
    }
    
    private static final ResourceLocation OVERLAY_FRAME =
        id("textures/entity/overlay_frame.png");
    private static final ResourceLocation OVERLAY_FILLED =
        id("textures/entity/overlay_filled.png");
    
}
