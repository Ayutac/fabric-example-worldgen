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
    }

    @Override
    String getEffectiveModId() {
        return ExampleMod.MOD_ID
    }
}
