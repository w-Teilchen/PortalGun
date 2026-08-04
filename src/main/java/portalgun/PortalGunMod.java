package portalgun;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import portalgun.config.PortalGunConfig;
import portalgun.entities.CustomPortal;
import portalgun.items.ClawItem;
import portalgun.items.PortalGunItem;
import portalgun.misc.BlockList;
import portalgun.recipe.PortalGunRechargeRecipe;
import qouteall.q_misc_util.my_util.IntBox;

import javax.annotation.Nullable;
import java.util.List;

public class PortalGunMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "portalgun";
    public static final String MOD_NAME = "PortalGun Mod";

    public static final double portalOffset = 0.001;
    public static final double portalOverlayOffset = 0.001;

    public static final PortalGunItem PORTAL_GUN = new PortalGunItem(
        new Item.Properties().fireResistant().stacksTo(1).rarity(Rarity.EPIC)
    );
    public static final Item PORTAL_GUN_BODY = new Item(
        new Item.Properties().fireResistant().stacksTo(1).rarity(Rarity.RARE)
    );
    public static final ClawItem PORTAL_GUN_CLAW = new ClawItem(
        new Item.Properties().fireResistant().stacksTo(1).rarity(Rarity.RARE)
    );

    public static final ResourceLocation PORTAL1_SHOOT =
        ResourceLocation.fromNamespaceAndPath(MODID, "portal1_shoot");
    public static final ResourceLocation PORTAL2_SHOOT =
        ResourceLocation.fromNamespaceAndPath(MODID, "portal2_shoot");
    public static final ResourceLocation PORTAL_OPEN =
        ResourceLocation.fromNamespaceAndPath(MODID, "portal_open");
    public static final ResourceLocation PORTAL_CLOSE =
        ResourceLocation.fromNamespaceAndPath(MODID, "portal_close");

    public static final SoundEvent PORTAL1_SHOOT_EVENT =
        SoundEvent.createVariableRangeEvent(PORTAL1_SHOOT);
    public static final SoundEvent PORTAL2_SHOOT_EVENT =
        SoundEvent.createVariableRangeEvent(PORTAL2_SHOOT);
    public static final SoundEvent PORTAL_OPEN_EVENT =
        SoundEvent.createVariableRangeEvent(PORTAL_OPEN);
    public static final SoundEvent PORTAL_CLOSE_EVENT =
        SoundEvent.createVariableRangeEvent(PORTAL_CLOSE);

    public static final CreativeModeTab TAB =
        FabricItemGroup.builder()
            .icon(() -> new ItemStack(PortalGunMod.PORTAL_GUN))
            .title(Component.translatable("portalgun.item_group"))
            .displayItems((enabledFeatures, entries) -> {
                int maxEnergy = PortalGunConfig.get().maxEnergy;

                // avoid item duplication https://github.com/iPortalTeam/PortalGun/issues/10
                if (maxEnergy != 0) {
                    entries.accept(new PortalGunItem.ItemInfo(
                        BlockList.createDefault(), maxEnergy, maxEnergy,
                        null, null, false
                    ).toStack());
                }

                // infinite energy
                entries.accept(new PortalGunItem.ItemInfo(
                    BlockList.createDefault(), 0, 0
                ).toStack());

                // only works on quartz
                entries.accept(new PortalGunItem.ItemInfo(
                    new BlockList(List.of("minecraft:quartz_block")),
                    0, 0
                ).toStack());

                // custom colors + gravity transform
                entries.accept(new PortalGunItem.ItemInfo(
                    BlockList.createDefault(),
                    0, 0,
                    0x30E551, 0xE600C6, true
                ).toStack());

                entries.accept(PORTAL_GUN_CLAW);
                entries.accept(PORTAL_GUN_BODY);
            })
            .build();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static boolean isAreaClear(Level world, IntBox airBox1) {
        // allow non-solid decorative blocks in the portal opening
        return airBox1.fastStream().allMatch(
            p -> world.getBlockState(p).getCollisionShape(world, p).isEmpty()
        );
    }

    @Override
    public void onInitialize() {
        PortalGunDataComponents.register();

        Registry.register(BuiltInRegistries.ITEM, id("portal_gun"), PORTAL_GUN);
        Registry.register(BuiltInRegistries.ITEM, id("portalgun_body"), PORTAL_GUN_BODY);
        Registry.register(BuiltInRegistries.ITEM, id("portalgun_claw"), PORTAL_GUN_CLAW);

        Registry.register(
            BuiltInRegistries.ENTITY_TYPE, id("custom_portal"), CustomPortal.entityType
        );

        Registry.register(BuiltInRegistries.SOUND_EVENT, PORTAL1_SHOOT, PORTAL1_SHOOT_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, PORTAL2_SHOOT, PORTAL2_SHOOT_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, PORTAL_OPEN, PORTAL_OPEN_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, PORTAL_CLOSE, PORTAL_CLOSE_EVENT);

        PortalGunConfig.register();

        PortalGunRechargeRecipe.init();

        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            id("general"),
            TAB
        );
    }

    public static @Nullable Integer parseColorTag(@Nullable Tag tag) {
        if (tag == null) {
            return null;
        }

        if (tag instanceof StringTag stringTag) {
            String value = stringTag.getAsString();

            if (value.startsWith("#")) {
                String hex = value.substring(1);
                try {
                    return Integer.parseUnsignedInt(hex, 16);
                }
                catch (NumberFormatException e) {
                    return null;
                }
            }

            DyeColor dyeColor = DyeColor.byName(value, null);
            if (dyeColor != null) {
                return dyeColor.getTextColor();
            }

            return null;
        }

        if (tag instanceof NumericTag numericTag) {
            return numericTag.getAsInt();
        }

        return null;
    }
}
