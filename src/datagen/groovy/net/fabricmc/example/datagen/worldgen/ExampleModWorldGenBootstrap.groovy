package net.fabricmc.example.datagen.worldgen


import com.mojang.datafixers.util.Pair
import net.fabricmc.example.ExampleMod

import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.registry.Registerable
import net.minecraft.registry.RegistryEntryLookup
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntryList
import net.minecraft.registry.tag.BlockTags
import net.minecraft.structure.StructureSet
import net.minecraft.structure.pool.StructurePool
import net.minecraft.structure.pool.StructurePoolElement
import net.minecraft.structure.pool.StructurePools
import net.minecraft.structure.rule.TagMatchRuleTest
import net.minecraft.util.collection.DataPool
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.intprovider.ConstantIntProvider
import net.minecraft.util.math.random.Random
import net.minecraft.world.Heightmap
import net.minecraft.world.biome.Biome
import net.minecraft.world.biome.BiomeEffects
import net.minecraft.world.biome.BiomeKeys
import net.minecraft.world.biome.GenerationSettings
import net.minecraft.world.biome.SpawnSettings
import net.minecraft.world.gen.GenerationStep
import net.minecraft.world.gen.HeightContext
import net.minecraft.world.gen.StructureTerrainAdaptation
import net.minecraft.world.gen.YOffset
import net.minecraft.world.gen.blockpredicate.BlockPredicate
import net.minecraft.world.gen.carver.ConfiguredCarver
import net.minecraft.world.gen.carver.ConfiguredCarvers
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement
import net.minecraft.world.gen.chunk.placement.SpreadType
import net.minecraft.world.gen.chunk.placement.StructurePlacement
import net.minecraft.world.gen.feature.*
import net.minecraft.world.gen.feature.size.TwoLayersFeatureSize
import net.minecraft.world.gen.foliage.BlobFoliagePlacer
import net.minecraft.world.gen.heightprovider.BiasedToBottomHeightProvider
import net.minecraft.world.gen.heightprovider.ConstantHeightProvider
import net.minecraft.world.gen.heightprovider.HeightProvider
import net.minecraft.world.gen.heightprovider.HeightProviderType
import net.minecraft.world.gen.heightprovider.UniformHeightProvider
import net.minecraft.world.gen.placementmodifier.BiomePlacementModifier
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier
import net.minecraft.world.gen.placementmodifier.EnvironmentScanPlacementModifier
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier
import net.minecraft.world.gen.placementmodifier.SurfaceThresholdFilterPlacementModifier
import net.minecraft.world.gen.stateprovider.BlockStateProvider
import net.minecraft.world.gen.stateprovider.WeightedBlockStateProvider
import net.minecraft.world.gen.structure.JigsawStructure
import net.minecraft.world.gen.structure.Structure
import net.minecraft.world.gen.trunk.StraightTrunkPlacer

class ExampleModWorldGenBootstrap {

    private ExampleModWorldGenBootstrap() {
        /* No instantiation */
    }

    /**
     * Main method for creating configured features.
     *
     * See also <a href="https://minecraft.fandom.com/wiki/Custom_feature#Configured_Feature">Configured Feature</a>
     * on the Minecraft Wiki.
     */
    static void configuredFeatures(Registerable<ConfiguredFeature> registry) {
        def placedFeatureLookup = registry.getRegistryLookup(RegistryKeys.PLACED_FEATURE)

        registry.register(ExampleMod.MY_ORE_CF, createMyOreConfiguredFeature())
        registry.register(ExampleMod.MY_LAKE_CF, createMyLakeConfiguredFeature())
        // ores and lakes don't need a patched feature, other things do
        registry.register(ExampleMod.MY_TREE_CF, createMyTreeConfiguredFeature())
        registry.register(ExampleMod.MY_TREE_PATCH_CF, createMyTreePatchConfiguredFeature(placedFeatureLookup))
    }

    /**
     * Creates a configured feature for an ore. Notice that the configured feature
     * only says what form the feature has, what it can replace and if it should be discarded
     * if next to air. There is no information about its location, including y level and dimension.
     *
     * @see #createMyOrePlacedFeature(net.minecraft.registry.RegistryEntryLookup)
     */
    private static ConfiguredFeature createMyOreConfiguredFeature() {
        return new ConfiguredFeature<>(Feature.ORE, new OreFeatureConfig(
                // what we want to replace (can be multiple rules/targets with List.of)
                new TagMatchRuleTest(BlockTags.STONE_ORE_REPLACEABLES),
                // what we want to insert, as a block state
                Blocks.ANCIENT_DEBRIS.getDefaultState(),
                // vein size; bigger number = more blocks per vein, but number != blocks per vein
                8,
                // chance between 0f and 1f (i.e. 0% to 100%, 0.5f would be 50%) to discard this vein if it is next to air blocks
                0f))
    }

    /**
     * Creates a configured feature for a lake. Notice that the configured feature
     * only says what form the feature has, what the fluid is and the bank material.
     * There is no information about its location, including y level and dimension.
     *
     * @see #createMyLakePlacedFeature(net.minecraft.registry.RegistryEntryLookup)
     */
    private static ConfiguredFeature createMyLakeConfiguredFeature() {
        return new ConfiguredFeature<>(Feature.LAKE,
                new LakeFeature.Config(
                        // blocks inside the lake
                        BlockStateProvider.of(Blocks.POWDER_SNOW.getDefaultState()),
                        // blocks around the lake
                        BlockStateProvider.of(Blocks.AMETHYST_BLOCK.getDefaultState()),
                )
        )
    }

    /**
     * Creates a configured feature for a tree. Notice that the configured feature
     * only says what form the feature has, including width and height and what blocks it consists of.
     * There is no information about its location, including y level and dimension.
     *
     * @see #createMyTreePlacedFeature(net.minecraft.registry.RegistryEntryLookup)
     */
    private static ConfiguredFeature createMyTreeConfiguredFeature() {
        // what can be used for logs of the tree, via block state
        final DataPool.Builder<BlockState> logDataPool = DataPool.<BlockState>builder()
                .add(Blocks.COBBLESTONE.getDefaultState(), 3)
                .add(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 1)
        // this will produce a stony tree log with about 1 mossy cobblestone in 4 log blocks

        return new ConfiguredFeature<>(Feature.TREE,
                new TreeFeatureConfig.Builder(
                        // blocks for the logs
                        new WeightedBlockStateProvider(logDataPool),
                        // size of the trunk: first value is min height (0-32), other two values are random height additions (0-24)
                        new StraightTrunkPlacer(6, 8, 3),
                        // blocks for the leaves
                        BlockStateProvider.of(Blocks.COBBLED_DEEPSLATE),
                        // according to what pattern the leaves are distributed
                        new BlobFoliagePlacer(
                                ConstantIntProvider.create(2),
                                ConstantIntProvider.create(0),
                                3
                        ),
                        // how thick/wide the tree is depending on its height (the limit(er))
                        new TwoLayersFeatureSize(
                                // the limit or separator
                                10,
                                // lower size thickness
                                1,
                                // upper size thickness
                                2
                        )
                        // we can define decorators to generate here, like bee hives, but won't
                ).build()
        )
    }

    private static ConfiguredFeature createMyTreePatchConfiguredFeature(RegistryEntryLookup<PlacedFeature> lookup) {
        return new ConfiguredFeature<>(Feature.RANDOM_PATCH,
                ConfiguredFeatures.createRandomPatchFeatureConfig(
                        // how many tries per patch are made (> 0)
                        30,
                        // the ID of the PLACED feature
                        lookup.getOrThrow(ExampleMod.MY_TREE_PF)
                )
        )
    }

    /**
     * Main method for creating placed features.
     *
     * See also <a href="https://minecraft.fandom.com/wiki/Custom_feature#Placed_Feature">Placed Feature</a>
     * on the Minecraft Wiki.
     */
    static void placedFeatures(Registerable<PlacedFeature> registry) {
        def configuredFeatureLookup = registry.getRegistryLookup(RegistryKeys.CONFIGURED_FEATURE)

        registry.register(ExampleMod.MY_ORE_PF, createMyOrePlacedFeature(configuredFeatureLookup))
        registry.register(ExampleMod.MY_LAKE_PF, createMyLakePlacedFeature(configuredFeatureLookup))
        // ores and lakes don't need a patched feature, other things do
        registry.register(ExampleMod.MY_TREE_PF, createMyTreePlacedFeature(configuredFeatureLookup))
        registry.register(ExampleMod.MY_TREE_PATCH_PF, createMyTreePatchPlacedFeature(configuredFeatureLookup))
    }

    /**
     * Creates a placed feature for an ore. Note that the placed feature isn't given
     * information about how it will look (besides giving it its configured feature of course),
     * only where it can be found, with the exception of the biome because
     * biomes choose their placed features.
     *
     * @see #createMyOreConfiguredFeature()
     */
    private static PlacedFeature createMyOrePlacedFeature(RegistryEntryLookup<ConfiguredFeature> configuredFeatureLookup) {
        return new PlacedFeature(
                // assign a configured feature to this placed feature by looking up its (configured feature) ID
                configuredFeatureLookup.getOrThrow(ExampleMod.MY_ORE_CF),
                // placement modifiers so this placed feature doesn't end up EVERYWHERE
                List.of(
                        // how many veins per chunk (remember a chunk is quite high)
                        CountPlacementModifier.of(8),
                        // put it somewhere random in the chunk, not at 0,y,0
                        SquarePlacementModifier.of(),
                        // most vanilla ores are trapezoid instead of uniform
                        // this example places uniform between -64 and 219 (if in overworld)
                        HeightRangePlacementModifier.uniform(
                                // smallest Y
                                YOffset.aboveBottom(0),
                                // biggest Y
                                YOffset.belowTop(100)
                        ),
                        // the biome decides if it wants this feature or not
                        BiomePlacementModifier.of()
                )
        )
    }

    /**
     * Creates a placed feature for a lake. Note that the placed feature isn't given
     * information about how it will look (besides giving it its configured feature of course),
     * only where it can be found, with the exception of the biome because
     * biomes choose their placed features.
     *
     * @see #createMyLakeConfiguredFeature()
     */
    private static PlacedFeature createMyLakePlacedFeature(RegistryEntryLookup<ConfiguredFeature> lookup) {
        return new PlacedFeature(
                // ID of the configured feature
                lookup.getOrThrow(ExampleMod.MY_LAKE_CF),
                List.of(
                        // chance of placement 1 / number (number = integer > 0)
                        RarityFilterPlacementModifier.of(5),
                        // can generate underground or on the ground
                        HeightRangePlacementModifier.of(UniformHeightProvider.create(YOffset.fixed(0), YOffset.getTop())),
                        // don't place it on the air
                        EnvironmentScanPlacementModifier.of(Direction.DOWN, BlockPredicate.bothOf(BlockPredicate.not(BlockPredicate.IS_AIR), BlockPredicate.insideWorldBounds(new BlockPos(0, -5, 0))), 32),
                        // something something don't mess with oceans
                        SurfaceThresholdFilterPlacementModifier.of(Heightmap.Type.OCEAN_FLOOR_WG, Integer.MIN_VALUE, -5))
        )
    }

    /**
     * Creates a placed feature for a tree. Note that the placed feature isn't given
     * information about how it will look, only where it can be found, with the exception
     * of the biome because biomes choose their placed features.
     *
     * @see #createMyTreeConfiguredFeature()
     */
    private static PlacedFeature createMyTreePlacedFeature(RegistryEntryLookup<ConfiguredFeature> lookup) {
        return new PlacedFeature(
                // the ID of the configured feature
                lookup.getOrThrow(ExampleMod.MY_TREE_CF), List.of(
                // only place on ground where oak saplings would survive
                PlacedFeatures.wouldSurvive(Blocks.OAK_SAPLING))
        )
    }

    private static PlacedFeature createMyTreePatchPlacedFeature(RegistryEntryLookup<ConfiguredFeature> lookup) {
        return new PlacedFeature(
                lookup.getOrThrow(ExampleMod.MY_TREE_PATCH_CF),
                List.of(
                        // chance of placement 1 / number (number = integer > 0)
                        RarityFilterPlacementModifier.of(3),
                        // put it somewhere random in the chunk or patch, not at 0,y,0
                        SquarePlacementModifier.of(),
                        // the heightmap to use
                        PlacedFeatures.MOTION_BLOCKING_HEIGHTMAP,
                        // the biome decides if it wants this feature or not
                        BiomePlacementModifier.of()
                )
        )
    }

    /**
     * Main method for creating biomes.
     *
     * See also <a href="https://minecraft.fandom.com/wiki/Custom_biome">Custom Biome</a>
     * on the Minecraft Wiki.
     */
    static void biomes(Registerable<Biome> registry) {
        def placedFeatureLookup = registry.getRegistryLookup(RegistryKeys.PLACED_FEATURE)
        def configuredCarverLookup = registry.getRegistryLookup(RegistryKeys.CONFIGURED_CARVER)

        registry.register(ExampleMod.MY_BIOME, createMyBiome(placedFeatureLookup, configuredCarverLookup))
    }

    private static Biome createMyBiome(RegistryEntryLookup<PlacedFeature> placedFeatureLookup, RegistryEntryLookup<ConfiguredCarver<?>> configuredCarverLookup) {

        return new Biome.Builder()
                // if it rains, snows or nothing falls, like in a desert
                .precipitation(Biome.Precipitation.RAIN)
                // temperature, between 0f and 1f. Lower is hotter (?), don't have 0.15f or lower if precipitation is not NONE
                .temperature(0.5f)
                // frozen is only other option, maybe use that for cold biomes
                .temperatureModifier(Biome.TemperatureModifier.NONE)
                // downfall, affects grass and foliage color. Between 0f and 1f, > 0.85f makes fire go out quicker, for wet biomes
                .downfall(0.8f)
                .effects(new BiomeEffects.Builder().
                        // color of fog over land in the distance
                        fogColor(0x1D8CB3).
                        skyColor(0x23BAEE).
                        waterColor(0x5D8ADC).
                        // color of fog in water in the distance
                        waterFogColor(0x9A9AD2).
                        // optional
                        grassColor(0x508E4A).
                        // optional
                        foliageColor(0x9FD29A).
                        // optional, this is the default
                        grassColorModifier(BiomeEffects.GrassColorModifier.NONE).
                        // biome particles (optional) also go here
                        //particleConfig(...).
                        // sound stuff is also optional, but goes here too
                        //moodSound(...)
                        build())
                .generationSettings(new GenerationSettings.LookupBackedBuilder(placedFeatureLookup, configuredCarverLookup).
                        // same carvers as in plains biome
                        carver(GenerationStep.Carver.AIR, ConfiguredCarvers.CAVE).
                        carver(GenerationStep.Carver.AIR, ConfiguredCarvers.CAVE_EXTRA_UNDERGROUND).
                        carver(GenerationStep.Carver.AIR, ConfiguredCarvers.CANYON).
                        // Since we don't change existing biomes, we can simply register our own features here.
                        // for floating islands
                        //feature(GenerationStep.Feature.RAW_GENERATION, ...).
                        // for stuff like lava lakes
                        feature(GenerationStep.Feature.LAKES, ExampleMod.MY_LAKE_PF).
                        // for stuff like geodes and icebergs
                        //feature(GenerationStep.Feature.LOCAL_MODIFICATIONS, ...).
                        // for stuff like dungeons or underground fossils
                        feature(GenerationStep.Feature.UNDERGROUND_STRUCTURES, UndergroundPlacedFeatures.MONSTER_ROOM).
                        // for surface structures like desert wells (NOT trees!)
                        feature(GenerationStep.Feature.SURFACE_STRUCTURES, MiscPlacedFeatures.DESERT_WELL).
                        // for ore blobs, including gravel/dirt/tuff blobs, and disks of clay etc
                        feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_DIAMOND_LARGE).
                        // this one is used for nether ore blobs for whatever reason
                        //feature(GenerationStep.Feature.UNDERGROUND_DECORATION, ...).
                        // for surface lakes I think
                        //feature(GenerationStep.Feature.FLUID_SPRINGS, ...).
                        // trees as well as the small stuff like flowers, vines etc
                        feature(GenerationStep.Feature.VEGETAL_DECORATION, TreePlacedFeatures.JUNGLE_BUSH).
                        feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_BERRY_COMMON).
                        // be careful to NOT add features here AND so in biome modifications that it applies to this biome
                        // that can lead to confusing feature cycle order exceptions
                        build())
                .spawnSettings(new SpawnSettings.Builder().
                        // TODO
                        build())
                .build()
    }

    /**
     * Main method for creating structures.
     *
     * See also <a href="https://minecraft.fandom.com/wiki/Custom_structure#Configured_Structure_Feature">Configured Structure Feature</a>
     * on the Minecraft Wiki and the <a href="https://misode.github.io/guides/adding-custom-structures/#the-structure">1.19 gist</a>.
     */
    static void structures(Registerable<Structure> registry) {
        def biomeLookup = registry.getRegistryLookup(RegistryKeys.BIOME)
        def templatePoolLookup = registry.getRegistryLookup(RegistryKeys.TEMPLATE_POOL)
        registry.register(ExampleMod.MY_HOUSE_STRUCTURE, createMyHouseStructure(biomeLookup, templatePoolLookup))
        registry.register(ExampleMod.MY_DUNGEON_STRUCTURE, createMyDungeonStructure(biomeLookup, templatePoolLookup))
    }

    private static Structure createMyHouseStructure(RegistryEntryLookup<Biome> biomeLookup, RegistryEntryLookup<StructurePool> templatePoolLookup) {
        return new JigsawStructure(new Structure.Config(
                // only spawn in plains
                new RegistryEntryList.Direct<>(List.of(biomeLookup.getOrThrow(BiomeKeys.PLAINS))),
                // no creature spawns on the structure
                Collections.emptyMap(),
                // generate while surface structures are generated
                GenerationStep.Feature.SURFACE_STRUCTURES,
                // how to accommodate to the surroundings
                StructureTerrainAdaptation.BEARD_THIN
        ),
                templatePoolLookup.getOrThrow(ExampleMod.MY_HOUSE_TEMPLATE_POOL),
                // needed for proper jigsaws, for simple structures we can simply set it to 1
                1,
                // no change in relation to world surface
                ConstantHeightProvider.ZERO,
                // always set this to false
                false,
                // generate on surface
                Heightmap.Type.WORLD_SURFACE_WG
        )
    }

    private static Structure createMyDungeonStructure(RegistryEntryLookup<Biome> biomeLookup, RegistryEntryLookup<StructurePool> templatePoolLookup) {
        return new JigsawStructure(new Structure.Config(
                // only spawn under deserts
                new RegistryEntryList.Direct<>(List.of(biomeLookup.getOrThrow(BiomeKeys.DESERT))),
                // no creature spawns on the structure // TODO add some?
                Collections.emptyMap(),
                // generate while surface structures are generated
                GenerationStep.Feature.UNDERGROUND_STRUCTURES,
                // how to accommodate to the surroundings
                StructureTerrainAdaptation.BEARD_THIN
        ),
                templatePoolLookup.getOrThrow(ExampleMod.MY_DUNGEON_ROOMS_TEMPLATE_POOL),
                // depth of 4
                4,
                // generate in deepslate level // TODO what is the "inner" for?
                BiasedToBottomHeightProvider.create(YOffset.BOTTOM, YOffset.aboveBottom(64), 10),
                // always set this to false
                false,
                // generate on ocean floor level
                Heightmap.Type.MOTION_BLOCKING
        )
    }

    /**
     * Main method for creating structure sets.
     *
     * See also <a href="https://minecraft.fandom.com/wiki/Custom_structure#Structure_Set">Structure Set</a>
     * on the Minecraft Wiki and the <a href="https://misode.github.io/guides/adding-custom-structures/#the-structure-set">1.19 gist</a>.
     */
    static void structureSets(Registerable<StructureSet> registry) {
        def structureLookup = registry.getRegistryLookup(RegistryKeys.STRUCTURE)
        registry.register(ExampleMod.MY_HOUSE_STRUCTURE_SET, createMyHouseStructureSet(structureLookup))
        registry.register(ExampleMod.MY_DUNGEON_STRUCTURE_SET, createMyDungeonStructureSet(structureLookup))
    }

    private static StructureSet createMyHouseStructureSet(RegistryEntryLookup<Structure> lookup) {
        return new StructureSet(
                // we only want to spawn the house with this set
                lookup.getOrThrow(ExampleMod.MY_HOUSE_STRUCTURE),
                // spawn rules for the house
                new RandomSpreadStructurePlacement(
                        // spacing in chunks, only one structure of this set per number x number chunks
                        5,
                        // min separation in chunks between two structures from this set
                        2,
                        SpreadType.LINEAR,
                        // a random number seed; change for every structure set
                        975406478
                )
        )
    }

    private static StructureSet createMyDungeonStructureSet(RegistryEntryLookup<Structure> lookup) {
        return new StructureSet(
                // we only want to spawn the dungeon with this set
                lookup.getOrThrow(ExampleMod.MY_DUNGEON_STRUCTURE),
                // spawn rules for the house
                new RandomSpreadStructurePlacement(
                        // spacing in chunks, only one structure of this set per number x number chunks
                        20,
                        // min separation in chunks between two structures from this set
                        10,
                        SpreadType.LINEAR,
                        // a random number seed; change for every structure set
                        19803475
                )
        )
    }

    /**
     * Main method for creating structure template pools.
     *
     * See also <a href="https://minecraft.fandom.com/wiki/Custom_structure#Structure_Pool">Structure Set</a>
     * on the Minecraft Wiki and the <a href="https://misode.github.io/guides/adding-custom-structures/#the-template-pool">1.19 gist</a>.
     */
    static void templatePools(Registerable<StructurePool> registry) {
        def templatePoolLookup = registry.getRegistryLookup(RegistryKeys.TEMPLATE_POOL)
        registry.register(ExampleMod.MY_HOUSE_TEMPLATE_POOL, createMyHouseStructurePool(templatePoolLookup))
        registry.register(ExampleMod.MY_DUNGEON_ROOMS_TEMPLATE_POOL, createMyDungeonRoomsStructurePool(templatePoolLookup))
        registry.register(ExampleMod.MY_DUNGEON_FODDER_TEMPLATE_POOL, createMyDungeonFodderStructurePool(templatePoolLookup))
        registry.register(ExampleMod.MY_DUNGEON_MIDBOSSES_TEMPLATE_POOL, createMyDungeonMidbossesStructurePool(templatePoolLookup))
        registry.register(ExampleMod.MY_DUNGEON_BOSSES_TEMPLATE_POOL, createMyDungeonBossesStructurePool(templatePoolLookup))
    }

    private static StructurePool createMyHouseStructurePool(RegistryEntryLookup<StructurePool> lookup) {
        return new StructurePool(
                // since we have no jigsaw, we can default to empty
                lookup.getOrThrow(StructurePools.EMPTY),
                // just one house please, with our structure ID
                List.of(Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_house").apply(StructurePool.Projection.RIGID), 1))
        )
    }

    private static StructurePool createMyDungeonRoomsStructurePool(RegistryEntryLookup<StructurePool> lookup) {
        return new StructurePool(
                // TODO comment/correct
                lookup.getOrThrow(StructurePools.EMPTY),
                // the rooms with their structure ID
                List.of(Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/rooms/fork_four").apply(StructurePool.Projection.RIGID), 4),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/rooms/fork_three").apply(StructurePool.Projection.RIGID), 5),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/rooms/fork_three_lava").apply(StructurePool.Projection.RIGID), 1),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/rooms/gangway").apply(StructurePool.Projection.RIGID), 4),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/rooms/turnway").apply(StructurePool.Projection.RIGID), 4),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/rooms/treasure_room").apply(StructurePool.Projection.RIGID), 3))
        )
    }

    private static StructurePool createMyDungeonFodderStructurePool(RegistryEntryLookup<StructurePool> lookup) {
        return new StructurePool(
                // TODO comment/correct
                lookup.getOrThrow(StructurePools.EMPTY),
                // the fodder with their structure ID
                List.of(Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/fodder/zombie").apply(StructurePool.Projection.RIGID), 5),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/fodder/husk").apply(StructurePool.Projection.RIGID), 3),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/fodder/drowned").apply(StructurePool.Projection.RIGID), 2),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/fodder/skeleton").apply(StructurePool.Projection.RIGID), 7),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/fodder/stray").apply(StructurePool.Projection.RIGID), 3),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/fodder/witch").apply(StructurePool.Projection.RIGID), 3),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/fodder/pillager").apply(StructurePool.Projection.RIGID), 5),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/fodder/vindicator").apply(StructurePool.Projection.RIGID), 2),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/fodder/cave_spider").apply(StructurePool.Projection.RIGID), 2))
        )
    }

    private static StructurePool createMyDungeonMidbossesStructurePool(RegistryEntryLookup<StructurePool> lookup) {
        return new StructurePool(
                // if midboss not available, switch to fodder
                lookup.getOrThrow(ExampleMod.MY_DUNGEON_FODDER_TEMPLATE_POOL),
                // the mid bosses with their structure ID
                List.of(Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/midbosses/creeper").apply(StructurePool.Projection.RIGID), 3),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/midbosses/iron_skeleton").apply(StructurePool.Projection.RIGID), 5),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/midbosses/iron_zombie").apply(StructurePool.Projection.RIGID), 5),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/midbosses/phantom").apply(StructurePool.Projection.RIGID), 1),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/midbosses/wither_skeleton").apply(StructurePool.Projection.RIGID), 2))
        )
    }

    private static StructurePool createMyDungeonBossesStructurePool(RegistryEntryLookup<StructurePool> lookup) {
        return new StructurePool(
                // if boss not available, switch to midboss
                lookup.getOrThrow(ExampleMod.MY_DUNGEON_MIDBOSSES_TEMPLATE_POOL),
                // the bosses with their structure ID
                List.of(Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/bosses/charged_creeper").apply(StructurePool.Projection.RIGID), 1),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/bosses/diamond_skeleton").apply(StructurePool.Projection.RIGID), 6),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/bosses/diamond_zombie").apply(StructurePool.Projection.RIGID), 4),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/bosses/evoker").apply(StructurePool.Projection.RIGID), 1),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/bosses/iron_wither_skeleton").apply(StructurePool.Projection.RIGID), 3),
                        Pair.of(StructurePoolElement.ofSingle(ExampleMod.MOD_ID + ":my_dungeon/monsters/bosses/iron_baby_zombie").apply(StructurePool.Projection.RIGID), 2))
        )
    }

}
