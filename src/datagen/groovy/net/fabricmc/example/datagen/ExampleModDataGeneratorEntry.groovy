package net.fabricmc.example.datagen

import net.fabricmc.example.ExampleMod
import net.fabricmc.example.datagen.worldgen.ExampleModWorldGenBootstrap
import net.fabricmc.example.datagen.worldgen.ExampleModWorldGenProvider
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.minecraft.registry.RegistryBuilder
import net.minecraft.registry.RegistryKeys

class ExampleModDataGeneratorEntry implements DataGeneratorEntrypoint {

    @Override
    void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        def pack = fabricDataGenerator.createPack()
        def add = {FabricDataGenerator.Pack.RegistryDependentFactory factory ->
            pack.addProvider factory
        }

        add ExampleModWorldGenProvider::new
    }

    @Override
    void buildRegistry(RegistryBuilder registryBuilder) {
        registryBuilder.addRegistry(RegistryKeys.CONFIGURED_FEATURE, ExampleModWorldGenBootstrap::configuredFeatures)
        registryBuilder.addRegistry(RegistryKeys.PLACED_FEATURE, ExampleModWorldGenBootstrap::placedFeatures)
        registryBuilder.addRegistry(RegistryKeys.BIOME, ExampleModWorldGenBootstrap::biomes)
        registryBuilder.addRegistry(RegistryKeys.DIMENSION_TYPE, ExampleModWorldGenBootstrap::dimensionTypes)
        registryBuilder.addRegistry(RegistryKeys.CHUNK_GENERATOR_SETTINGS, ExampleModWorldGenBootstrap::chunkGeneratorSettings)
        registryBuilder.addRegistry(RegistryKeys.DIMENSION, ExampleModWorldGenBootstrap::dimensions)
        registryBuilder.addRegistry(RegistryKeys.STRUCTURE, ExampleModWorldGenBootstrap::structures)
        registryBuilder.addRegistry(RegistryKeys.STRUCTURE_SET, ExampleModWorldGenBootstrap::structureSets)
        registryBuilder.addRegistry(RegistryKeys.TEMPLATE_POOL, ExampleModWorldGenBootstrap::templatePools)
    }

    @Override
    String getEffectiveModId() {
        return ExampleMod.MOD_ID
    }
}
