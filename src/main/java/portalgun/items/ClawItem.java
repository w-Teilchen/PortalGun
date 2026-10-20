package portalgun.items;

import net.minecraft.world.item.Item;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import portalgun.client.renderer.ClawItemRenderer;

import java.util.function.Consumer;

public class ClawItem extends Item implements GeoItem {

    public AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public ClawItem(Properties settings) {
        super(settings);

        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        // Utilise GeckoLib's GeoRenderProvider hook for the custom item renderer
        consumer.accept(new GeoRenderProvider() {
            private ClawItemRenderer renderer;

            @Override
            public ClawItemRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ClawItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
            new AnimationController<ClawItem>(
                "clawController", 1, state -> PlayState.CONTINUE
            )
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
