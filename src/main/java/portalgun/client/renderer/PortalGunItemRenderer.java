package portalgun.client.renderer;

import net.minecraft.resources.Identifier;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import portalgun.items.PortalGunItem;

public class PortalGunItemRenderer extends GeoItemRenderer<PortalGunItem> {
    public PortalGunItemRenderer() {
        super(new DefaultedItemGeoModel<>(
            Identifier.fromNamespaceAndPath("portalgun", "portalgun")
        ));
    }
}
