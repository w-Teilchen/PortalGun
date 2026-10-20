package portalgun.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import portalgun.PortalGunMod;
import portalgun.entities.CustomPortal;
import portalgun.misc.RemoteCallables;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.miscellaneous.IPortalInitialScreen;
import qouteall.imm_ptl.core.platform_specific.IPConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shoots both portals with the real item code paths (right click = Item.use, left click = the
 * packet handler the client sends), checks that they link, looks through one and walks through it.
 * Results go to build/portal-test/report.txt, screenshots next to it.
 */
public class PortalGunTest implements FabricClientGameTest {
    private final List<String> results = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();
    private Path out;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        out = Path.of(System.getProperty("portalgun.test.out", "portal-test"));
        try {
            Files.createDirectories(out);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        results.add("mods: sodium=" + FabricLoader.getInstance().isModLoaded("sodium")
            + " iris=" + FabricLoader.getInstance().isModLoaded("iris")
            + " distanthorizons=" + FabricLoader.getInstance().isModLoaded("distanthorizons")
            + " physicsmod=" + FabricLoader.getInstance().isModLoaded("physicsmod"));

        ctx.runOnClient(mc -> {
            IPConfig c = IPConfig.getConfig();
            c.initialScreenShown = true;
            c.initialScreenVersion = IPortalInitialScreen.CONTENT_VERSION;
            c.saveConfigFile();
            if (mc.gui.screen() instanceof IPortalInitialScreen s) {
                s.onClose();
            }
            mc.options.bobView().set(false);
            IPGlobal.disableTeleportation = false;
        });
        ctx.waitForScreen(TitleScreen.class);
        boolean dhScene = "dh".equals(System.getProperty("portalgun.test.scene"));
        boolean inventoryScene = "inventory".equals(System.getProperty("portalgun.test.scene"));
        if (dhScene) {
            // LODs only start beyond the vanilla render distance
            ctx.runOnClient(mc -> mc.options.renderDistance().set(6));
        }

        try (TestSingleplayerContext sp = ctx.worldBuilder()
            // the DH scene needs real terrain and the run directory's shaderpack and mod configs
            .setUseConsistentSettings(!dhScene)
            .adjustSettings(s -> {
                s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                if (dhScene) {
                    s.setSeed("portalgun-dh");
                }
            })
            .create()
        ) {
            TestServerContext srv = sp.getServer();
            try {
                sp.getConnection().waitForChunksRender();
            }
            catch (Throwable e) {
                // with a shaderpack and DH generating, the first chunks can take longer than the harness waits
                if (!dhScene) {
                    throw e;
                }
                results.add("note: waitForChunksRender: " + e);
            }
            srv.runCommand("time set noon");
            srv.runCommand("gamerule doDaylightCycle false");
            try {
                if (dhScene) {
                    dhScene(ctx, srv);
                }
                else if (inventoryScene) {
                    inventoryScene(ctx);
                }
                else {
                    scene(ctx, srv);
                }
            }
            catch (Throwable e) {
                check("scene finished without an exception", false, e.toString());
            }
            // written before closing the world: with the full mod set, closing can deadlock the test harness
            writeReport();
            if (dhScene) {
                Runtime.getRuntime().halt(failures.isEmpty() ? 0 : 1);
            }
        }

        writeReport();
        if (!failures.isEmpty()) {
            throw new AssertionError(failures.size() + " checks failed:\n" + String.join("\n", failures));
        }
    }

    /** The creative inventory on the Portal Gun tab and the items in the hotbar, to check their GUI icons. */
    private void inventoryScene(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            mc.player.getInventory().setItem(0, new ItemStack(PortalGunMod.PORTAL_GUN));
            mc.player.getInventory().setItem(1, new ItemStack(PortalGunMod.PORTAL_GUN_CLAW));
            mc.player.getInventory().setItem(2, new ItemStack(PortalGunMod.PORTAL_GUN_BODY));
        });
        ctx.waitTicks(10);
        screenshot(ctx, "hotbar");
        ctx.setScreen(() -> new CreativeModeInventoryScreen(ctx.computeOnClient(mc -> mc.player),
            ctx.computeOnClient(mc -> mc.player.connection.enabledFeatures()), true));
        ctx.waitTicks(5);
        boolean selected = ctx.computeOnClient(mc -> mc.gui.screen() instanceof FabricCreativeModeInventoryScreen s
            && s.setSelectedTab(PortalGunMod.TAB));
        check("portal gun creative tab selected", selected, "");
        ctx.waitTicks(10);
        screenshot(ctx, "creative-tab");
        // Immersive Portals' own tab (portal wand, command sticks, portal helper)
        ctx.runOnClient(mc -> ((FabricCreativeModeInventoryScreen) mc.gui.screen())
            .setSelectedTab(qouteall.imm_ptl.peripheral.PeripheralModMain.TAB));
        ctx.waitTicks(10);
        screenshot(ctx, "creative-tab-ivp");
        ctx.setScreen(() -> null);
    }

    private void scene(ClientGameTestContext ctx, TestServerContext srv) {
        int g = srv.computeOnServer(s -> {
            ServerLevel level = s.overworld();
            level.getChunk(0, 0);
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING, 10, 0);
        }) + 3;
        // a stone room: wall A at x=0 (faces +x), wall B at x=20 (faces -x), floor and roof
        srv.runCommand(fill(-1, g - 1, -4, 21, g + 6, 4, "minecraft:stone"));
        srv.runCommand(fill(1, g, -3, 19, g + 5, 3, "minecraft:air"));
        srv.runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:glowstone", 5, g + 5, -3, 15, g + 5, 3));
        srv.runCommand("give Player0 portalgun:portal_gun");
        ctx.waitTicks(10);

        AABB room = new AABB(-2, g - 2, -5, 22, g + 8, 5);

        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 90 0", g));
        ctx.waitTicks(60);
        ctx.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
        screenshot(ctx, "before_portals");

        // right click towards wall B (orange), via Item.use
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 -90 0", g));
        ctx.waitTicks(10);
        srv.runOnServer(s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            p.getInventory().setSelectedSlot(0);
            PortalGunMod.PORTAL_GUN.use(p.level(), p, InteractionHand.MAIN_HAND);
        });
        ctx.waitTicks(10);

        // left click towards wall A (blue), via the packet handler
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 90 0", g));
        ctx.waitTicks(10);
        srv.runOnServer(s -> RemoteCallables.onClientLeftClickPortalGun(s.getPlayerList().getPlayers().get(0)));
        ctx.waitTicks(40);

        List<CustomPortal> portals = srv.computeOnServer(s ->
            s.overworld().getEntitiesOfClass(CustomPortal.class, room));
        check("two portals were created", portals.size() == 2, "count " + portals.size());
        boolean linked = portals.size() == 2 && portals.stream().allMatch(p -> p.isVisible() && p.isTeleportable());
        check("both portals are linked (visible and teleportable)", linked, describe(portals));
        CustomPortal portalA = portals.stream().filter(p -> p.getOriginPos().x < 10).findFirst().orElse(null);
        check("the blue portal is on wall A and leads to wall B", portalA != null && portalA.getDestPos().x > 15,
            describe(portals));

        boolean clientHasPortals = waitFor(ctx, 200, () -> ctx.computeOnClient(mc ->
            mc.level.getEntitiesOfClass(CustomPortal.class, room).size() == 2));
        check("the client received both portals", clientHasPortals, "");

        ctx.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
        // look at wall A through the room: the blue portal should show the view out of the orange one
        ctx.waitTicks(40);
        screenshot(ctx, "look_at_blue_portal");
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 -90 0", g));
        ctx.waitTicks(40);
        screenshot(ctx, "look_at_orange_portal");

        // close up: a marker block in front of wall B must be visible through the blue portal
        srv.runCommand(String.format(Locale.ROOT, "setblock 17 %d 0 minecraft:gold_block", g + 1));
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 4.5 %d 0.5 90 0", g));
        ctx.waitTicks(60);
        screenshot(ctx, "close_up_blue_portal");
        srv.runCommand(String.format(Locale.ROOT, "setblock 17 %d 0 minecraft:air", g + 1));

        // walk into the blue portal (moving -x), the player should come out of the orange one (x > 15)
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 3.0 %d 0.5 90 0", g));
        ctx.waitTicks(20);
        ctx.runOnClient(mc -> {
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        });
        boolean through = false;
        for (int i = 0; i < 60 && !through; i++) {
            ctx.runOnClient(mc -> mc.player.setPos(mc.player.getX() - 0.1, mc.player.getY(), mc.player.getZ()));
            ctx.waitTick();
            through = ctx.computeOnClient(mc -> mc.player.getX() > 15);
        }
        ctx.waitTicks(20);
        Vec3 pos = ctx.computeOnClient(mc -> mc.player.position());
        check("walking into the blue portal comes out of the orange one", through, "client pos " + pos);
        screenshot(ctx, "after_walking_through");

        // clearing the portals (the R key handler on the server) removes both
        srv.runOnServer(s -> RemoteCallables.onClientClearPortalGun(s.getPlayerList().getPlayers().get(0)));
        boolean cleared = waitFor(ctx, 100, () -> srv.computeOnServer(s ->
            s.overworld().getEntitiesOfClass(CustomPortal.class, room).isEmpty()));
        check("clearing removes both portals", cleared, "");
    }

    /**
     * A closed stone room with Distant Horizons: no LOD terrain may show on the walls,
     * neither before nor after the portals exist. Judged from the screenshots.
     */
    private void dhScene(ClientGameTestContext ctx, TestServerContext srv) {
        int g = srv.computeOnServer(s -> {
            ServerLevel level = s.overworld();
            level.getChunk(0, 0);
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING, 10, 0);
        }) + 3;
        srv.runCommand("give Player0 portalgun:portal_gun");
        ctx.runOnClient(mc -> {
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        });

        // let DH generate LODs around the spawn, seen from high above
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 90 20", g + 60));
        int waitSeconds = Integer.getInteger("portalgun.test.dhWait", 90);
        ctx.waitTicks(waitSeconds * 20);
        screenshot(ctx, "dh_overview");
        // the sky, to see which cloud style the shaderpack draws
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 90 -25", g + 60));
        ctx.waitTicks(40);
        screenshot(ctx, "dh_sky");

        srv.runCommand(fill(-1, g - 1, -4, 21, g + 6, 4, "minecraft:stone"));
        srv.runCommand(fill(1, g, -3, 19, g + 5, 3, "minecraft:air"));
        srv.runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:glowstone", 5, g + 5, -3, 15, g + 5, 3));
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 0 0", g));
        ctx.waitTicks(60);
        // side wall (+z), no portal on it
        screenshot(ctx, "dh_side_wall_no_portals");
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 -90 0", g));
        ctx.waitTicks(20);
        screenshot(ctx, "dh_end_wall_no_portals");

        srv.runOnServer(s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            p.getInventory().setSelectedSlot(0);
            PortalGunMod.PORTAL_GUN.use(p.level(), p, InteractionHand.MAIN_HAND);
        });
        ctx.waitTicks(10);
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 90 0", g));
        ctx.waitTicks(10);
        srv.runOnServer(s -> RemoteCallables.onClientLeftClickPortalGun(s.getPlayerList().getPlayers().get(0)));
        ctx.waitTicks(60);
        ctx.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));

        AABB room = new AABB(-2, g - 2, -5, 22, g + 8, 5);
        List<CustomPortal> portals = srv.computeOnServer(s ->
            s.overworld().getEntitiesOfClass(CustomPortal.class, room));
        check("two portals were created", portals.size() == 2, "count " + portals.size());

        screenshot(ctx, "dh_look_at_blue_portal");
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 0 0", g));
        ctx.waitTicks(40);
        screenshot(ctx, "dh_side_wall_with_portals");
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 45 0", g));
        ctx.waitTicks(40);
        screenshot(ctx, "dh_portal_at_edge");
        srv.runCommand(String.format(Locale.ROOT, "tp Player0 10.5 %d 0.5 90 20", g + 60));
        ctx.waitTicks(60);
        screenshot(ctx, "dh_overview_with_portals");
    }

    private void writeReport() {
        try {
            Files.write(out.resolve("report.txt"), results);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        return String.format(Locale.ROOT, "fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2, block);
    }

    private static String describe(List<CustomPortal> portals) {
        StringBuilder sb = new StringBuilder();
        for (CustomPortal p : portals) {
            sb.append(String.format(Locale.ROOT, "[origin %s dest %s visible %b teleportable %b] ",
                p.getOriginPos(), p.getDestPos(), p.isVisible(), p.isTeleportable()));
        }
        return sb.toString();
    }

    private boolean waitFor(ClientGameTestContext ctx, int maxTicks, java.util.function.BooleanSupplier condition) {
        for (int i = 0; i < maxTicks; i += 5) {
            if (condition.getAsBoolean()) {
                return true;
            }
            ctx.waitTicks(5);
        }
        return condition.getAsBoolean();
    }

    private void screenshot(ClientGameTestContext ctx, String name) {
        ctx.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(out));
    }

    private void check(String name, boolean ok, String detail) {
        String line = (ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() || ok ? "" : "  (" + detail + ")");
        results.add(line);
        if (!ok) {
            failures.add(line);
        }
    }
}
