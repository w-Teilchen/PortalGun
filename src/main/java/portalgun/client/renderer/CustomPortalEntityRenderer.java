package portalgun.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.render.PortalEntityRenderer;
import qouteall.q_misc_util.my_util.DQuaternion;
import portalgun.PortalGunMod;
import portalgun.client.PortalgunClient;
import portalgun.entities.CustomPortal;

import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
import static portalgun.PortalGunMod.id;

/**
 * Extends Immersive Portals' renderer: on 26.3 the portal surface (the view through the portal)
 * is submitted by the portal's entity renderer, so it must still run before the overlay is drawn.
 */
public class CustomPortalEntityRenderer extends PortalEntityRenderer {
    private final ModelPart overlayBase;
    
    public CustomPortalEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        overlayBase = context.bakeLayer(PortalgunClient.OVERLAY_MODEL_LAYER).getChild("base");
    }
    
    @Override
    public void submit(
        PortalRenderState state, PoseStack matrices,
        SubmitNodeCollector submitNodeCollector, CameraRenderState camera
    ) {
        super.submit(state, matrices, submitNodeCollector, camera);
        
        if (!(state.portal instanceof CustomPortal entity)) {
            return;
        }
        
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
        int packedColor = 0xFF000000 | (color & 0xFFFFFF);
        
        submitNodeCollector.submitModelPart(
            overlayBase,
            matrices,
            RenderTypes.entityTranslucent(getTextureLocation(entity)),
            LightCoordsUtil.FULL_BRIGHT,
            NO_OVERLAY,
            null,
            packedColor
        );
        matrices.popPose();
    }
    
    private static Identifier getTextureLocation(CustomPortal entity) {
        return entity.isVisible() ? OVERLAY_FRAME : OVERLAY_FILLED;
    }
    
    private static final Identifier OVERLAY_FRAME =
        id("textures/entity/overlay_frame.png");
    private static final Identifier OVERLAY_FILLED =
        id("textures/entity/overlay_filled.png");
    
}
