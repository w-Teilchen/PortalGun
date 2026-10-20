package portalgun.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import portalgun.PortalGunMod;
import portalgun.items.PortalGunItem;

public class PortalGunRechargeRecipe extends CustomRecipe {
    // stateless special recipe, like vanilla's RepairItemRecipe since 26.x
    public static final PortalGunRechargeRecipe INSTANCE = new PortalGunRechargeRecipe();
    public static final MapCodec<PortalGunRechargeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, PortalGunRechargeRecipe> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<PortalGunRechargeRecipe> SERIALIZER =
        new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public static void init() {
        Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.parse("portalgun:portal_gun_recharge"),
            SERIALIZER
        );
    }

    /**
     * Portal gun + nether star restores remaining energy to max.
     * MC's recipe book cannot pick stacks with custom data, so keep it special.
     */
    @Override
    public boolean matches(CraftingInput input, Level level) {
        int portalGunCount = 0;
        int netherStarCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() == PortalGunMod.PORTAL_GUN) {
                portalGunCount++;
            }
            if (stack.getItem() == Items.NETHER_STAR) {
                netherStarCount++;
            }
        }

        return portalGunCount == 1 && netherStarCount == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack portalGun = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() == PortalGunMod.PORTAL_GUN) {
                portalGun = stack;
                break;
            }
        }

        if (portalGun.isEmpty()) {
            return ItemStack.EMPTY;
        }

        PortalGunItem.ItemInfo itemInfo = PortalGunItem.readInfo(portalGun);
        itemInfo.remainingEnergy = itemInfo.maxEnergy;
        return itemInfo.toStack();
    }

    @Override
    public RecipeSerializer<PortalGunRechargeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
