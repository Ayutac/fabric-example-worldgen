package net.fabricmc.example.datagen.worldgen

import net.fabricmc.example.ExampleMod
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.fluid.Fluids
import net.minecraft.registry.Registerable
import net.minecraft.registry.RegistryEntryLookup
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.BlockTags
import net.minecraft.structure.rule.TagMatchRuleTest
import net.minecraft.util.collection.DataPool
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.intprovider.ConstantIntProvider
import net.minecraft.world.Heightmap
import net.minecraft.world.gen.YOffset
import net.minecraft.world.gen.blockpredicate.BlockPredicate
import net.minecraft.world.gen.feature.*
import net.minecraft.world.gen.feature.size.TwoLayersFeatureSize
import net.minecraft.world.gen.foliage.BlobFoliagePlacer
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

}
