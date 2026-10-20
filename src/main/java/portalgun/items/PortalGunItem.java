package portalgun.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import portalgun.config.PortalGunConfig;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.PortalUtils;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.AARotation;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.LimitedLogger;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import portalgun.PortalGunDataComponents;
import portalgun.PortalGunMod;
import portalgun.PortalGunRecord;
import portalgun.client.renderer.PortalGunItemRenderer;
import portalgun.entities.CustomPortal;
import portalgun.misc.BlockList;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class PortalGunItem extends Item implements GeoItem {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final LimitedLogger LIMITED_LOGGER = new LimitedLogger(100);
    public static final int COOLDOWN_TICKS = 4;
    public static final double SIZE_MULTIPLIER = 31.0 / 32.0;
    
    public final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    
    private static final RawAnimation SHOOT_ANIM = RawAnimation.begin().thenPlay("portal_shoot");

    public PortalGunItem(Properties settings) {
        super(settings);

        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }
    
    // example commands (1.21 data component):
    // /give @p portalgun:portal_gun[portalgun:portal_gun_data={allowedBlocks:["#minecraft:ice","minecraft:stone"]}]
    // /give @p portalgun:portal_gun[portalgun:portal_gun_data={side1Color:"#32a852",side2Color:"purple"}]
    // /give @p portalgun:portal_gun[portalgun:portal_gun_data={maxEnergy:0}]
    public static final class ItemInfo {
        public @NotNull BlockList allowedBlocks;
        
        /**
         * 0 for infinite energy
         */
        public int maxEnergy;
        public int remainingEnergy;
        
        public @Nullable Integer side1Color;
        public @Nullable Integer side2Color;
        
        public boolean transformGravity = false;
        
        public ItemInfo(
            @NotNull BlockList allowedBlocks,
            int maxEnergy, // 0 for infinite energy
            int remainingEnergy
        ) {
            this.allowedBlocks = allowedBlocks;
            this.maxEnergy = maxEnergy;
            this.remainingEnergy = remainingEnergy;
        }
        
        public ItemInfo(
            @NotNull BlockList allowedBlocks, int maxEnergy, int remainingEnergy,
            @Nullable Integer side1Color, @Nullable Integer side2Color,
            boolean transformGravity
        ) {
            this.allowedBlocks = allowedBlocks;
            this.maxEnergy = maxEnergy;
            this.remainingEnergy = remainingEnergy;
            this.side1Color = side1Color;
            this.side2Color = side2Color;
            this.transformGravity = transformGravity;
        }
        
        public static ItemInfo fromTag(CompoundTag tag) {
            BlockList allowedBlocks =
                BlockList.fromTag(tag.getListOrEmpty("allowedBlocks"));
            int maxEnergy = tag.getIntOr("maxEnergy", PortalGunConfig.get().maxEnergy);
            
            int remainingEnergy = tag.getIntOr("remainingEnergy", PortalGunConfig.get().maxEnergy);
            
            @Nullable Integer side1Color = PortalGunMod.parseColorTag(tag.get("side1Color"));
            @Nullable Integer side2Color = PortalGunMod.parseColorTag(tag.get("side2Color"));
            
            boolean transformGravity = tag.getBooleanOr("transformGravity", false);
            
            return new ItemInfo(
                allowedBlocks, maxEnergy, remainingEnergy, side1Color, side2Color,
                transformGravity
            );
        }
        
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put("allowedBlocks", allowedBlocks.toTag());
            tag.putInt("maxEnergy", maxEnergy);
            tag.putInt("remainingEnergy", remainingEnergy);
            if (side1Color != null) {
                tag.putInt("side1Color", side1Color);
            }
            if (side2Color != null) {
                tag.putInt("side2Color", side2Color);
            }
            tag.putBoolean("transformGravity", transformGravity);
            return tag;
        }
        
        public ItemStack toStack() {
            ItemStack stack = new ItemStack(PortalGunMod.PORTAL_GUN);
            writeInfo(stack, this);
            return stack;
        }
        
        public boolean limitsEnergy() {
            return this.maxEnergy != 0;
        }
        
        public @Nullable Integer getCustomColor(PortalGunRecord.PortalGunSide side) {
            return switch (side) {
                case blue -> side1Color;
                case orange -> side2Color;
            };
        }
        
        public int getColor(PortalGunRecord.PortalGunSide side) {
            Integer customColor = getCustomColor(side);
            if (customColor != null) {
                return customColor;
            }
            
            return side.getColorInt();
        }
        
        @Override
        public String toString() {
            return "ItemInfo{" +
                "allowedBlocks=" + allowedBlocks +
                ", maxEnergy=" + maxEnergy +
                ", remainingEnergy=" + remainingEnergy +
                ", side1Color=" + side1Color +
                ", side2Color=" + side2Color +
                '}';
        }
    }
    
    /**
     * Read portal gun state from the custom data component.
     * Falls back to config defaults when the component is missing.
     */
    public static ItemInfo readInfo(ItemStack stack) {
        CustomData customData = stack.get(PortalGunDataComponents.PORTAL_GUN_DATA);
        if (customData == null || customData.isEmpty()) {
            int maxEnergy = PortalGunConfig.get().maxEnergy;
            return new ItemInfo(BlockList.createDefault(), maxEnergy, maxEnergy);
        }
        return ItemInfo.fromTag(customData.copyTag());
    }

    /**
     * Write portal gun state into the custom data component.
     */
    public static void writeInfo(ItemStack stack, ItemInfo itemInfo) {
        stack.set(PortalGunDataComponents.PORTAL_GUN_DATA, CustomData.of(itemInfo.toTag()));
    }

    // Utilise our own render hook to define our custom renderer
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PortalGunItemRenderer renderer;

            @Override
            public PortalGunItemRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new PortalGunItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    // Register our animation controllers
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
            new AnimationController<PortalGunItem>("portalGunController", 1, state -> PlayState.CONTINUE)
                .triggerableAnim("shoot_anim", SHOOT_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    public boolean canDestroyBlock(ItemStack itemStack, BlockState state, Level world, BlockPos pos, LivingEntity user) {
        return false;
    }
    
    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.FAIL;
        }
        
        boolean success = interact(
            (ServerPlayer) player, hand,
            PortalGunRecord.PortalGunSide.orange
        );
        
        if (success) {
            player.awardStat(Stats.ITEM_USED.get(this));
            
            // always fail, to cancel hand swing
            return InteractionResult.FAIL;
        }
        else {
            return InteractionResult.FAIL;
        }
    }
    
    @Override
    public void appendHoverText(
        @NotNull ItemStack stack, @NotNull Item.TooltipContext context,
        @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, display, tooltip, flag);

        tooltip.accept(Component.translatable("item.portalgun.portal_gun_desc").withStyle(ChatFormatting.GOLD));

        ItemInfo itemInfo = readInfo(stack);
        
        if (!itemInfo.allowedBlocks.list().isEmpty()) {
            tooltip.accept(Component.translatable("portal_gun.limit_allowed_blocks"));
            int displayLimit = 5;
            List<Block> allowedBlocks = itemInfo.allowedBlocks.asStream().limit(displayLimit + 1).toList();
            for (int i = 0; i < displayLimit; i++) {
                if (i < allowedBlocks.size()) {
                    Block block = allowedBlocks.get(i);
                    tooltip.accept(block.getName().withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            if (allowedBlocks.size() > displayLimit) {
                tooltip.accept(Component.literal("..."));
            }
        }
        
        if (itemInfo.limitsEnergy()) {
            tooltip.accept(Component.translatable(
                "portal_gun.remaining_energy", itemInfo.remainingEnergy, itemInfo.maxEnergy
            ));
        }
        
        if (itemInfo.side1Color != null || itemInfo.side2Color != null) {
            Style s1 = Style.EMPTY.withColor(
                itemInfo.getColor(PortalGunRecord.PortalGunSide.blue)
            );
            Style s2 = Style.EMPTY.withColor(
                itemInfo.getColor(PortalGunRecord.PortalGunSide.orange)
            );
            
            MutableComponent t1 = Component.literal("█").setStyle(s1);
            MutableComponent t2 = Component.literal("█").setStyle(s2);
            
            tooltip.accept(Component.empty().append(t1).append(" ").append(t2));
        }
        
        if (itemInfo.transformGravity) {
            tooltip.accept(
                Component.translatable("portal_gun.transform_gravity")
                    .withStyle(ChatFormatting.AQUA)
            );
        }
    }
    
    public InteractionResult onAttack(
        Player player, Level world, InteractionHand hand
    ) {
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        
        boolean success = interact(
            (ServerPlayer) player, hand,
            PortalGunRecord.PortalGunSide.blue
        );
        
        if (success) {
            player.awardStat(Stats.ITEM_USED.get(this));
            
            return InteractionResult.SUCCESS;
        }
        else {
            return InteractionResult.FAIL;
        }
    }
    
    // return whether successful
    public boolean interact(
        ServerPlayer player,
        InteractionHand hand,
        PortalGunRecord.PortalGunSide side
    ) {
        ItemStack itemStack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(itemStack, COOLDOWN_TICKS);
        
        PortalUtils.PortalAwareRaytraceResult raytraceResult = PortalUtils.portalAwareRayTrace(player, 100);
        
        if (raytraceResult == null) {
            return false;
        }
        
        BlockHitResult blockHit = raytraceResult.hitResult();
        ServerLevel world = ((ServerLevel) raytraceResult.world());
        Direction wallFacing = blockHit.getDirection();
        
        if (!checkAction(player, world)) {
            return false;
        }
        
        Validate.isTrue(blockHit.getType() == HitResult.Type.BLOCK);
        
        player.level().playSound(
            null,
            player.getX(), player.getY(), player.getZ(),
            side == PortalGunRecord.PortalGunSide.blue ?
                PortalGunMod.PORTAL1_SHOOT_EVENT : PortalGunMod.PORTAL2_SHOOT_EVENT,
            SoundSource.PLAYERS,
            1.0F, 1.0F
        );
        
        PortalGunRecord.PortalGunKind kind = PortalGunRecord.PortalGunKind._2x1;
        
        PortalGunRecord.PortalDescriptor descriptor =
            new PortalGunRecord.PortalDescriptor(player.getUUID(), kind, side);
        
        ItemInfo itemInfo = readInfo(itemStack);

        if (itemInfo.limitsEnergy()) {
            if (itemInfo.remainingEnergy <= 0) {
                player.sendOverlayMessage(
                    Component.translatable("portal_gun.out_of_energy")
                );
                return false;
            }
        }
        
        BiPredicate<Level, BlockPos> wallPredicate = itemInfo.allowedBlocks.getWallPredicate();
        
        PortalPlacement placement = findPortalPlacement(
            player, kind, raytraceResult, descriptor, wallPredicate
        );
        
        if (placement == null) {
            return false;
        }
        
        if (itemInfo.limitsEnergy()) {
            itemInfo.remainingEnergy--;
            writeInfo(itemStack, itemInfo);
        }
        
        Direction rightDir = placement.rotation.transformedX;
        Direction upDir = placement.rotation.transformedY;
        
        triggerAnim(
            player,
            GeoItem.getOrAssignId(player.getItemInHand(hand), ((ServerLevel) player.level())),
            "portalGunController", "shoot_anim"
        );
        
        PortalGunRecord record = PortalGunRecord.get();
        
        PortalGunRecord.PortalDescriptor otherSideDescriptor = descriptor.getTheOtherSide();
        
        PortalGunRecord.PortalInfo thisSideInfo = record.data.get(descriptor);
        PortalGunRecord.PortalInfo otherSideInfo = record.data.get(otherSideDescriptor);
        
        Vec3 wallFacingVec = Vec3.atLowerCornerOf(wallFacing.getUnitVec3i());
        Vec3 newPortalOrigin = Helper
            .getBoxSurface(placement.areaBox.toRealNumberBox(), wallFacing.getOpposite())
            .getCenter()
            .add(wallFacingVec.scale(PortalGunMod.portalOffset));
        
        CustomPortal portal = null;
        boolean isExistingPortal = false;
        
        if (thisSideInfo != null) {
            Entity entity = world.getEntity(thisSideInfo.portalId());
            if (entity instanceof CustomPortal customPortal) {
                portal = customPortal;
                isExistingPortal = true;
            }
        }
        
        if (portal == null) {
            portal = CustomPortal.entityType.create(world, EntitySpawnReason.TRIGGERED);
            assert portal != null;
        }
        
        portal.setOriginPos(newPortalOrigin);
        portal.setOrientationAndSize(
            Vec3.atLowerCornerOf(rightDir.getUnitVec3i()),
            Vec3.atLowerCornerOf(upDir.getUnitVec3i()),
            kind.getWidth() * SIZE_MULTIPLIER,
            kind.getHeight() * SIZE_MULTIPLIER
        );
        portal.descriptor = descriptor;
        portal.wallBox = placement.wallBox;
        portal.airBox = placement.areaBox;
        portal.setAllowedBlocks(itemInfo.allowedBlocks);
        portal.thisSideUpdateCounter = thisSideInfo == null ? 0 : thisSideInfo.updateCounter();
        portal.otherSideUpdateCounter = otherSideInfo == null ? 0 : otherSideInfo.updateCounter();
        PortalManipulation.makePortalRound(portal, 20);
        portal.disableDefaultAnimation();
        
        portal.customColor = itemInfo.getCustomColor(side);
        portal.setTeleportChangesGravity(itemInfo.transformGravity);
        
        if (otherSideInfo == null) {
            // it's unpaired, invisible and not teleportable
            portal.setDestinationDimension(world.dimension());
            portal.setDestination(newPortalOrigin.add(0, 10, 0));
            portal.setIsVisible(false);
            portal.setTeleportable(false);
        }
        else {
            // it's linked
            portal.setDestinationDimension(otherSideInfo.portalDim());
            portal.setDestination(otherSideInfo.portalPos());
            portal.setOtherSideOrientation(otherSideInfo.portalOrientation());
            portal.setIsVisible(true);
            portal.setTeleportable(true);
            player.level().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                PortalGunMod.PORTAL_OPEN_EVENT,
                SoundSource.PLAYERS,
                1.0F, 1.0F
            );
        }
        
        portal.thisSideUpdateCounter += 1;
        thisSideInfo = new PortalGunRecord.PortalInfo(
            portal.getUUID(),
            world.dimension(),
            newPortalOrigin,
            portal.getOrientationRotation(),
            portal.thisSideUpdateCounter
        );
        record.data.put(descriptor, thisSideInfo);
        record.setDirty();
        
        if (!isExistingPortal) {
            McHelper.spawnServerEntity(portal);
        }
        else {
            portal.reloadAndSyncToClient();
        }
        
        return true;
    }
    
    private static boolean checkAction(ServerPlayer player, ServerLevel world) {
        if (world.dimension() == Level.END) {
            EnderDragonFight endDragonFight = world.getDragonFight();
            if (endDragonFight != null) {
                if (!endDragonFight.hasPreviouslyKilledDragon()) {
                    player.sendOverlayMessage(
                        Component.literal("Using portal gun in end before killing any ender dragon is not allowed")
                    );
                    return false;
                }
            }
        }
        return true;
    }
    
    private static record PortalPlacement(
        AARotation rotation,
        IntBox areaBox,
        IntBox wallBox
    ) {
    }
    
    @Nullable
    private static PortalPlacement findPortalPlacement(
        ServerPlayer player,
        PortalGunRecord.PortalGunKind kind,
        PortalUtils.PortalAwareRaytraceResult raytraceResult,
        PortalGunRecord.PortalDescriptor descriptor,
        BiPredicate<Level, BlockPos> wallPredicate
    ) {
        BlockHitResult blockHit = raytraceResult.hitResult();
        ServerLevel world = ((ServerLevel) raytraceResult.world());
        BlockPos interactingAirPos = blockHit.getBlockPos().relative(blockHit.getDirection());
        Direction wallFacing = blockHit.getDirection();
        
        Direction playerGravity = GravityChangerInterface.invoker.getGravityDirection(player);
        Direction effectiveGravity = raytraceResult.portalsPassingThrough().stream().reduce(
            playerGravity,
            (gravity, portal) -> portal.getTeleportedGravityDirection(gravity),
            (g1, g2) -> {throw new RuntimeException();}
        );
        Vec3 viewVector = player.getViewVector(1);
        Vec3 transformedViewVector = raytraceResult.portalsPassingThrough().stream().reduce(
            viewVector,
            (v, portal) -> portal.transformLocalVec(v),
            (v1, v2) -> {throw new RuntimeException();}
        );
        
        Vec3 localViewVector = GravityChangerInterface.invoker
            .transformWorldToPlayer(effectiveGravity, transformedViewVector);
        
        Direction localWallFacing = GravityChangerInterface.invoker
            .transformDirWorldToPlayer(effectiveGravity, wallFacing);
        
        Direction[] localUpDirCandidates =
            Helper.getAnotherFourDirections(localWallFacing.getAxis());
        
        Arrays.sort(localUpDirCandidates, Comparator.comparingDouble((Direction dir) -> {
            if (dir == Direction.UP) {
                // the local up direction has the highest priority
                return 1;
            }
            // horizontal dot product
            return dir.getUnitVec3i().getX() * localViewVector.x + dir.getUnitVec3i().getZ() * localViewVector.z;
        }).reversed());
        
        BlockPos portalAreaSize = new BlockPos(kind.getWidth(), kind.getHeight(), 1);
        
        for (Direction localUpDir : localUpDirCandidates) {
            Direction upDir = GravityChangerInterface.invoker
                .transformDirPlayerToWorld(effectiveGravity, localUpDir);
            
            AARotation rot = AARotation.getAARotationFromYZ(upDir, wallFacing);
            BlockPos transformedSize = rot.transform(portalAreaSize);
            IntBox portalArea = IntBox.getBoxByPosAndSignedSize(interactingAirPos, transformedSize);
            IntBox wallArea = portalArea.getMoved(wallFacing.getOpposite().getUnitVec3i());
            
            if (PortalGunMod.isAreaClear(world, portalArea) &&
                wallArea.fastStream().allMatch(p -> wallPredicate.test(world, p)) &&
                !otherPortalExistsInArea(world, wallArea, wallFacing, descriptor)
            ) {
                return new PortalPlacement(rot, portalArea, wallArea);
            }
        }
        
        return null;
    }
    
    private static boolean otherPortalExistsInArea(
        Level world, IntBox wallArea, Direction wallFacing,
        PortalGunRecord.PortalDescriptor descriptor
    ) {
        List<CustomPortal> portals = McHelper.findEntitiesByBox(
            CustomPortal.class,
            world,
            wallArea.toRealNumberBox().inflate(0.1),
            IPGlobal.maxNormalPortalRadius,
            p -> p.getApproximateFacingDirection() == wallFacing
                && IntBox.getIntersect(p.wallBox, wallArea) != null
                && !Objects.equals(p.descriptor, descriptor)
        );
        return !portals.isEmpty();
    }
    
    // PortalGun does not use vanilla damage system
    // because vanilla max damage is unchangeable
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return readInfo(stack).limitsEnergy();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        ItemInfo itemInfo = readInfo(stack);

        if (!itemInfo.limitsEnergy()) {
            return 0;
        }

        return Math.round((float) itemInfo.remainingEnergy * 13.0F / (float) itemInfo.maxEnergy);
    }
    
    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return 0x00d907e0;
    }
}
