package portalgun.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import portalgun.PortalGunMod;
import portalgun.client.renderer.CustomPortalEntityRenderer;
import portalgun.client.renderer.models.PortalOverlayModel;
import portalgun.entities.CustomPortal;

import static portalgun.PortalGunMod.id;

@Environment(EnvType.CLIENT)
public class PortalgunClient implements ClientModInitializer {
    public static final ModelLayerLocation OVERLAY_MODEL_LAYER =
        new ModelLayerLocation(id("portal_overlay"), "main");
    
    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(id("portalgun"));
        KeyMapping clearPortals = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.portalgun.clearportals", InputConstants.Type.KEYBOARD, InputConstants.KEY_R, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (clearPortals.consumeClick()) {
                /**{@link portalgun.misc.RemoteCallables#onClientClearPortalGun}*/
                McRemoteProcedureCall.tellServerToInvoke(
                    "portalgun.misc.RemoteCallables.onClientClearPortalGun"
                );
            }
        });
        
        ModelLayerRegistry.registerModelLayer(
            OVERLAY_MODEL_LAYER, PortalOverlayModel::getTexturedModelData
        );
        EntityRendererRegistry.register(CustomPortal.entityType, (EntityRendererProvider) CustomPortalEntityRenderer::new);
        
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            ItemStack mainHandItem = player.getMainHandItem();
            
            if (mainHandItem.getItem() != PortalGunMod.PORTAL_GUN) {
                return false;
            }
            
            ItemCooldowns cooldowns = player.getCooldowns();
            float cooldownPercent = cooldowns.getCooldownPercent(mainHandItem, 0);
            
            if (cooldownPercent < 0.001) {
                /**{@link portalgun.misc.RemoteCallables#onClientLeftClickPortalGun(ServerPlayer)}*/
                McRemoteProcedureCall.tellServerToInvoke(
                    "portalgun.misc.RemoteCallables.onClientLeftClickPortalGun"
                );
            }
            
            return true;
        });
    }
}
