package portalgun.client.renderer;

import net.minecraft.resources.Identifier;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import portalgun.items.ClawItem;

public class ClawItemRenderer extends GeoItemRenderer<ClawItem> {
    public ClawItemRenderer() {
        super(
            new DefaultedItemGeoModel<ClawItem>(
                Identifier.fromNamespaceAndPath("portalgun", "portalgun_claw")
            ).withAltTexture(Identifier.fromNamespaceAndPath("portalgun", "portalgun"))
        );
    }
}
