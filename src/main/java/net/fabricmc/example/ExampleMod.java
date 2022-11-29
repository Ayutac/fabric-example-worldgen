package net.fabricmc.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public class ExampleMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("modid");

	public static final RegistryKey<ConfiguredFeature<?,?>> MY_ORE_CF = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("modid", "my_ore"));

	public static final RegistryKey<PlacedFeature> MY_ORE_PF = RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier("modid", "my_ore"));

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		BiomeModifications.create(new Identifier("modid", "features"))
				.add(ModificationPhase.ADDITIONS,
						// we want our ore possibly everywhere in the overworld
						BiomeSelectors.foundInOverworld(),
						myOreModifier());
	}

	private static BiConsumer<BiomeSelectionContext, BiomeModificationContext> myOreModifier() {
		return (biomeSelectionContext, biomeModificationContext) -> {
			// here we can potentially narrow our biomes down
			// but here we won't
			biomeModificationContext.getGenerationSettings().addFeature(
					// ores to ores
					GenerationStep.Feature.UNDERGROUND_ORES,
					// this is the key of the placed feature
					MY_ORE_PF);
		};
	}
}
