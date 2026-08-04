package portalgun;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.component.CustomData;

/**
 * Custom item data components for 1.21+.
 * Portal gun state used to live in raw ItemStack NBT; that API is gone.
 */
public class PortalGunDataComponents {
    /**
     * Stores {@link portalgun.items.PortalGunItem.ItemInfo} as CustomData NBT.
     * Keeps the same tag layout as before so existing helper code can stay.
     */
    public static final DataComponentType<CustomData> PORTAL_GUN_DATA = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        PortalGunMod.id("portal_gun_data"),
        DataComponentType.<CustomData>builder()
            .persistent(CustomData.CODEC)
            .networkSynchronized(CustomData.STREAM_CODEC)
            .build()
    );

    public static void register() {
        // Force class loading so the static field registers the component.
    }
}
