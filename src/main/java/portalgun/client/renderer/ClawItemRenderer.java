package portalgun.client.renderer;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import portalgun.items.ClawItem;

public class ClawItemRenderer extends GeoItemRenderer<ClawItem> {
    public ClawItemRenderer() {
        super(
            new DefaultedItemGeoModel<ClawItem>(
                ResourceLocation.fromNamespaceAndPath("portalgun", "portalgun_claw")
            ).withAltTexture(ResourceLocation.fromNamespaceAndPath("portalgun", "portalgun"))
        );
    }
}
