package net.fabricmc.example.datagen.worldgen

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper

import java.util.concurrent.CompletableFuture

class ExampleModWorldGenProvider extends FabricDynamicRegistryProvider {
    ExampleModWorldGenProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture)
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
        entries.addAll(registries.getWrapperOrThrow(RegistryKeys.CONFIGURED_FEATURE))
        entries.addAll(registries.getWrapperOrThrow(RegistryKeys.PLACED_FEATURE))
        entries.addAll(registries.getWrapperOrThrow(RegistryKeys.STRUCTURE))
        entries.addAll(registries.getWrapperOrThrow(RegistryKeys.STRUCTURE_SET))
        entries.addAll(registries.getWrapperOrThrow(RegistryKeys.TEMPLATE_POOL))
    }

    @Override
    String getName() {
        return "Example Mod World gen"
    }
}
