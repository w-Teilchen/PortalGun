package portalgun.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.my_util.IntBox;
import portalgun.PortalGunMod;
import portalgun.PortalGunRecord;
import portalgun.misc.BlockList;

import java.util.function.BiPredicate;

public class CustomPortal extends Portal {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // since 1.21.2 the registry key is needed when building the type
    public static EntityType<CustomPortal> entityType =
        Portal.createPortalEntityType(PortalGunMod.id("custom_portal"), CustomPortal::new);
    
    public PortalGunRecord.PortalDescriptor descriptor;
    
    public IntBox wallBox;
    public IntBox airBox;
    
    private BlockList allowedBlocks;
    
    public int thisSideUpdateCounter = 0;
    public int otherSideUpdateCounter = 0;
    
    public @Nullable Integer customColor;
    
    private BiPredicate<Level, BlockPos> wallPredicate;
    
    public CustomPortal(@NotNull EntityType<?> entityType, net.minecraft.world.level.Level world) {
        super(entityType, world);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        descriptor = PortalGunRecord.PortalDescriptor.fromTag(compoundTag.getCompoundOrEmpty("descriptor"));
        wallBox = IntBox.fromTag(compoundTag.getCompoundOrEmpty("wallBox"));
        airBox = IntBox.fromTag(compoundTag.getCompoundOrEmpty("airBox"));
        setAllowedBlocks(BlockList.fromTag(compoundTag.getListOrEmpty("allowedBlocks")));
        thisSideUpdateCounter = compoundTag.getIntOr("thisSideUpdateCounter", 0);
        otherSideUpdateCounter = compoundTag.getIntOr("otherSideUpdateCounter", 0);
        customColor = PortalGunMod.parseColorTag(compoundTag.get("customColor"));
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.put("descriptor", descriptor.toTag());
        compoundTag.put("wallBox", wallBox.toTag());
        compoundTag.put("airBox", airBox.toTag());
        compoundTag.put("allowedBlocks", allowedBlocks.toTag());
        compoundTag.putInt("thisSideUpdateCounter", thisSideUpdateCounter);
        compoundTag.putInt("otherSideUpdateCounter", otherSideUpdateCounter);
        if (customColor != null) {
            compoundTag.putInt("customColor", customColor);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide()) {
            updateState();
        }
    }
    
    // disable the interpolation between last tick pos and this tick pos
    // because the portal should change abruptly
    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        setOldPosAndRot();
    }
    
    public void setAllowedBlocks(BlockList allowedBlocks) {
        this.allowedBlocks = allowedBlocks;
        this.wallPredicate = allowedBlocks.getWallPredicate();
    }
    
    void updateState() {
        if (descriptor == null || wallBox == null) {
            LOGGER.error("Portal info abnormal {}", this);
            kill((ServerLevel) level());
            return;
        }
        
        PortalGunRecord record = PortalGunRecord.get();
        PortalGunRecord.PortalInfo thisSideInfo = record.data.get(descriptor);
        PortalGunRecord.PortalInfo otherSideInfo = record.data.get(descriptor.getTheOtherSide());
        if (thisSideInfo == null) {
            // info is missing
            playClosingSound();
            kill((ServerLevel) level());
            return;
        }
        if (thisSideUpdateCounter != thisSideInfo.updateCounter() || !thisSideInfo.portalId().equals(getUUID())) {
            // replaced by new portal
            kill((ServerLevel) level());
            return;
        }
        // check block status
        if (!checkBlockStatus()) {
            kill((ServerLevel) level());
            record.data.remove(descriptor);
            record.setDirty();
            playClosingSound();
            return;
        }
        if (otherSideInfo == null) {
            // other side is missing, make this side inactive
            if (otherSideUpdateCounter != -1) {
                otherSideUpdateCounter = -1;
                setTeleportable(false);
                setIsVisible(false);
                setDestination(getOriginPos().add(0, 10, 0));
                reloadAndSyncToClient();
            }
            return;
        }
        if (otherSideInfo.updateCounter() != otherSideUpdateCounter) {
            // other side is replaced by new portal, update linking
            if (!isVisible()) {
                level().playSound(
                    null,
                    getX(), getY(), getZ(),
                    PortalGunMod.PORTAL_OPEN_EVENT,
                    SoundSource.PLAYERS,
                    1.0F, 1.0F
                );
            }
            otherSideUpdateCounter = otherSideInfo.updateCounter();
            setTeleportable(true);
            setIsVisible(true);
            setDestination(otherSideInfo.portalPos());
            setDestinationDimension(otherSideInfo.portalDim());
            setOtherSideOrientation(otherSideInfo.portalOrientation());
            reloadAndSyncToClient();
            return;
        }
    }
    
    private boolean checkBlockStatus() {
        if (wallPredicate == null) {
            return true;
        }
        
        boolean wallIntact = wallBox.fastStream().allMatch(p -> wallPredicate.test(level(), p));
        if (!wallIntact) {
            return false;
        }
        
        return PortalGunMod.isAreaClear(level(), airBox);
    }
    
    private void playClosingSound() {
        level().playSound(
            null,
            getX(), getY(), getZ(),
            PortalGunMod.PORTAL_CLOSE_EVENT,
            SoundSource.PLAYERS,
            1.0F, 1.0F
        );
    }
    
    public int getColor() {
        if (customColor != null) {
            return customColor;
        }
        
        return this.descriptor.side().getColorInt();
    }
    
}
