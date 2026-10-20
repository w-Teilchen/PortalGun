package portalgun.client.renderer.models;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The portal overlay mesh. Since 1.21.2 entity models are drawn from render states,
 * so the renderer submits the baked "base" part directly instead of going through an EntityModel.
 */
public class PortalOverlayModel {
    public static MeshDefinition getModelData() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
        
        PartDefinition base = modelPartData.addOrReplaceChild(
            "base", CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 16, 0.0F, 0.0F, 0.0F, 0.0F)
        );
        
        PartDefinition cube_r1 = base.addOrReplaceChild(
            "cube_r1",
            CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(
                    -8.0F, -16.0F, 0.0F, 16.0F, 32.0F, 0.0F,
                    new CubeDeformation(0.0F)
                ).mirror(true),
            PartPose.offsetAndRotation(0.0F, -16.0F, 0.0F, 0.0F, (float) Math.PI, 0.0F)
        );
        return modelData;
    }
    
    public static LayerDefinition getTexturedModelData() {
        return LayerDefinition.create(getModelData(), 32, 64);
    }
}
