# 6.3.0+port.1 (Minecraft 26.3)

- Port to Minecraft 26.3 / Fabric (Java 25)
- Immersive Portals is archived at 1.21.1; this uses its 26.3 port, [Immersively Vibed Portals](https://github.com/nyannoying1337/ImmersivelyVibedPortals) 6.1.0-beta.12 (bundles DimLib)
- GeckoLib 5.5.7 (`items/*.json` with GeckoLib's special item model, geo/animation files moved to `geckolib/`)
- Cloth Config 26.3.159, Mod Menu 21.0.0; recipes moved to `data/portalgun/recipe/`
- Dependencies come from Maven / the IVP GitHub release instead of local jars
- Client gametest (`./gradlew runClientGameTest`): shoots both portals, checks they link, looks and walks through, clears them;
  `-Pscene=inventory` screenshots the creative tab. Results and screenshots go to `build/portal-test/`.
- Toolchain: Loom 1.18, Gradle 9.7.1, Fabric Loader 0.19.5, Fabric API 0.163.0+26.3

# 6.3.0 (Minecraft 1.21.1)

- Port to Minecraft 1.21.1 / Fabric (Java 21)
- Immersive Portals v6.0.6-mc1.21.1 (Modrinth) + DimLib
- GeckoLib 4.8.4, Cloth Config 15.0.130, Mod Menu 11.0.1
- Migrate portal gun item data from raw NBT tags to a custom data component
- Update recipe, SavedData, ResourceLocation, GeckoLib, and model rendering APIs for 1.21
- Fix AutoConfig maxEnergy label translations (including Italian)
- Toolchain: Loom 1.9, Gradle 8.12, Fabric Loader 0.16.9, Fabric API 0.116.15+1.21.1

# 6.2.1

possibly fix compat issue with cc:tweaked when maxEnergy is 0
