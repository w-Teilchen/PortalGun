package portalgun.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import portalgun.PortalGunMod;

@Config(name = PortalGunMod.MODID)
public class PortalGunConfig implements ConfigData {
    /**
     * Default max energy for newly created portal guns.
     * 0 means infinite energy.
     */
    @ConfigEntry.Gui.Tooltip
    public int maxEnergy = 100;

    public static void register() {
        AutoConfig.register(PortalGunConfig.class, JanksonConfigSerializer::new);
    }

    public static PortalGunConfig get() {
        return AutoConfig.getConfigHolder(PortalGunConfig.class).getConfig();
    }

    public static void save() {
        AutoConfig.getConfigHolder(PortalGunConfig.class).save();
    }

}
