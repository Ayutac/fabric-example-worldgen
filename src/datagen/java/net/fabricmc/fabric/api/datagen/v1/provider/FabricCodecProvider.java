package net.fabricmc.fabric.api.datagen.v1.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;

public abstract class FabricCodecProvider<T> implements DataProvider {
    private final Codec<T> codec;
    private final FabricDataOutput output;
    private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;

    private FabricCodecProvider(Codec<T> codec, FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        this.codec = codec;
        this.output = output;
        this.registriesFuture = registriesFuture;
    }

    public abstract void generate(RegistryWrapper.WrapperLookup registries, Entries<T> entries);

    public abstract DataOutput.PathResolver getPathResolver(DataOutput dataOutput);

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        return registriesFuture.thenApplyAsync(registries -> {
            Entries<T> entries = new Entries<>();
            generate(registries, entries);
            return writeEntries(writer, entries.getEntries());
        });
    }

    private CompletableFuture<?> writeEntries(DataWriter writer, Collection<Entry<T>> entries) {
        final DataOutput.PathResolver pathResolver = getPathResolver(output);
        return CompletableFuture.allOf(
                entries.stream()
                        .map(entry -> writeEntry(writer, pathResolver, entry))
                        .toArray(CompletableFuture[]::new)
        );
    }

    private CompletableFuture<?> writeEntry(DataWriter writer, DataOutput.PathResolver pathResolver, Entry<T> entry) {
        return CompletableFuture.supplyAsync(() -> encode(entry))
                .thenCompose((jsonElement -> DataProvider.writeToPath(writer, jsonElement, pathResolver.resolveJson(entry.identifier()))));
    }

    private JsonElement encode(Entry<T> entry) {
        return codec.encodeStart(JsonOps.INSTANCE, entry.value).resultOrPartial((error) -> {
            throw new RuntimeException("Couldn't serialize element %s: %s".formatted(entry.identifier, error));
        }).orElseThrow();
    }

    @Override
    public String getName() {
        return null;
    }

    private record Entry<T>(Identifier identifier, T value) { }

    public static final class Entries<T> {
        private final List<Entry<T>> entries = new LinkedList<>();

        private Entries() {
        }

        void add(Identifier identifier, T value) {
            entries.add(new Entry<>(identifier, value));
        }

        void add(RegistryWrapper.Impl<T> registry, RegistryKey<T> registryKey) {
            add(registryKey.getValue(), registry.getOrThrow(registryKey).value());
        }

        private Collection<Entry<T>> getEntries() {
            return Collections.unmodifiableCollection(entries);
        }
    }

}