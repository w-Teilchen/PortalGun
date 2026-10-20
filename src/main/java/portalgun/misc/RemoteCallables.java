package portalgun.misc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import portalgun.PortalGunMod;
import portalgun.PortalGunRecord;

public class RemoteCallables {
    public static void onClientLeftClickPortalGun(
        ServerPlayer player
    ) {
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (itemInHand.getItem() == PortalGunMod.PORTAL_GUN) {
            ItemCooldowns cooldowns = player.getCooldowns();
            float cooldownPercent = cooldowns.getCooldownPercent(itemInHand, 0);
            
            if (cooldownPercent < 0.001) {
                PortalGunMod.PORTAL_GUN.onAttack(player, player.level(), InteractionHand.MAIN_HAND);
            }
            else {
                PortalGunMod.LOGGER.warn("Received portal gun interaction packet while on cooldown {}", player);
            }
        }
        else {
            PortalGunMod.LOGGER.error("Invalid left click packet {}", player);
        }
    }
    
    public static void onClientClearPortalGun(
        ServerPlayer player
    ) {
        PortalGunRecord record = PortalGunRecord.get();
        PortalGunRecord.PortalDescriptor orangeDescriptor =
            new PortalGunRecord.PortalDescriptor(
                player.getUUID(),
                PortalGunRecord.PortalGunKind._2x1,
                PortalGunRecord.PortalGunSide.orange
            );
        PortalGunRecord.PortalDescriptor blueDescriptor =
            new PortalGunRecord.PortalDescriptor(
                player.getUUID(),
                PortalGunRecord.PortalGunKind._2x1,
                PortalGunRecord.PortalGunSide.blue
            );
        record.data.remove(orangeDescriptor);
        record.data.remove(blueDescriptor);
        record.setDirty();
    }
}
