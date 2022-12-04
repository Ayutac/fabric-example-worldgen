package net.fabricmc.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public class ExampleMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("modid");

	public static final String MOD_ID = "modid";

	public static final RegistryKey<ConfiguredFeature<?,?>> MY_ORE_CF = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(MOD_ID, "my_ore"));
	public static final RegistryKey<ConfiguredFeature<?,?>> MY_LAKE_CF = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(MOD_ID, "my_lake"));
	public static final RegistryKey<ConfiguredFeature<?,?>> MY_TREE_CF = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(MOD_ID, "my_tree"));
	public static final RegistryKey<ConfiguredFeature<?,?>> MY_TREE_PATCH_CF = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(MOD_ID, "my_tree_patch"));

	public static final RegistryKey<PlacedFeature> MY_ORE_PF = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(MOD_ID, "my_ore"));
	public static final RegistryKey<PlacedFeature> MY_LAKE_PF = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(MOD_ID, "my_lake"));
	public static final RegistryKey<PlacedFeature> MY_TREE_PF = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(MOD_ID, "my_tree"));
	public static final RegistryKey<PlacedFeature> MY_TREE_PATCH_PF = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(MOD_ID, "my_tree_patch"));

	public static final RegistryKey<Biome> MY_BIOME = RegistryKey.of(RegistryKeys.BIOME, new Identifier(MOD_ID, "my_biome"));

	public static final RegistryKey<Structure> MY_HOUSE_STRUCTURE = RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(MOD_ID, "my_house"));
	public static final RegistryKey<Structure> MY_DUNGEON_STRUCTURE = RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(MOD_ID, "my_dungeon"));

	public static final RegistryKey<StructureSet> MY_HOUSE_STRUCTURE_SET = RegistryKey.of(RegistryKeys.STRUCTURE_SET, new Identifier(MOD_ID, "my_house_set"));
	public static final RegistryKey<StructureSet> MY_DUNGEON_STRUCTURE_SET = RegistryKey.of(RegistryKeys.STRUCTURE_SET, new Identifier(MOD_ID, "my_dungeon_set"));

	public static final RegistryKey<StructurePool> MY_HOUSE_TEMPLATE_POOL = RegistryKey.of(RegistryKeys.TEMPLATE_POOL, new Identifier(MOD_ID, "my_house_pool"));
	public static final RegistryKey<StructurePool> MY_DUNGEON_ROOMS_TEMPLATE_POOL = RegistryKey.of(RegistryKeys.TEMPLATE_POOL, new Identifier(MOD_ID, "my_dungeon/rooms"));
	public static final RegistryKey<StructurePool> MY_DUNGEON_FODDER_TEMPLATE_POOL = RegistryKey.of(RegistryKeys.TEMPLATE_POOL, new Identifier(MOD_ID, "my_dungeon/monsters/fodder"));
	public static final RegistryKey<StructurePool> MY_DUNGEON_MIDBOSSES_TEMPLATE_POOL = RegistryKey.of(RegistryKeys.TEMPLATE_POOL, new Identifier(MOD_ID, "my_dungeon/monsters/midbosses"));
	public static final RegistryKey<StructurePool> MY_DUNGEON_BOSSES_TEMPLATE_POOL = RegistryKey.of(RegistryKeys.TEMPLATE_POOL, new Identifier(MOD_ID, "my_dungeon/monsters/bosses"));

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// modify the biomes
		BiomeModifications.create(new Identifier(MOD_ID, "features"))
				.add(ModificationPhase.ADDITIONS,
						// we want our ore possibly everywhere in the overworld
						BiomeSelectors.foundInOverworld(),
						myOreModifier())
				.add(ModificationPhase.ADDITIONS,
						// we want our lake in any forest (even in other dimensions)
						BiomeSelectors.tag(BiomeTags.IS_FOREST),
						myLakeModifier())
				.add(ModificationPhase.ADDITIONS,
						// we want our tree anywhere (even other dimensions) but in the ocean
						BiomeSelectors.tag(BiomeTags.IS_OCEAN).negate(),
						myTreePatchModifier());
	}

	private static BiConsumer<BiomeSelectionContext, BiomeModificationContext> myOreModifier() {
		return (biomeSelectionContext, biomeModificationContext) ->
				// here we can potentially narrow our biomes down
				// but here we won't
				biomeModificationContext.getGenerationSettings().addFeature(
						// ores to ores
						GenerationStep.Feature.UNDERGROUND_ORES,
						// this is the key of the placed feature
						MY_ORE_PF);
	}

	private static BiConsumer<BiomeSelectionContext, BiomeModificationContext> myLakeModifier() {
		return (biomeSelectionContext, biomeModificationContext) ->
				biomeModificationContext.getGenerationSettings().addFeature(
						// lakes to lakes
						GenerationStep.Feature.LAKES,
						// this is the key of the placed feature
						MY_LAKE_PF);
	}

	private static BiConsumer<BiomeSelectionContext, BiomeModificationContext> myTreePatchModifier() {
		return (biomeSelectionContext, biomeModificationContext) ->
			biomeModificationContext.getGenerationSettings().addFeature(
				// trees to vegetation
				GenerationStep.Feature.VEGETAL_DECORATION,
				// this is the key of the PATCH of the placed feature
				MY_TREE_PATCH_PF);
	}

}
