package net.fabricmc.example.worldgen;

import com.mojang.serialization.Codec;
import net.fabricmc.example.ExampleMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.IglooStructure;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Optional;

public class MyHouseStructure extends Structure {
    public static final Codec<MyHouseStructure> CODEC = createCodec(MyHouseStructure::new);
    public static final StructurePieceType PIECE_TYPE = Registry.register(Registries.STRUCTURE_PIECE, new Identifier(ExampleMod.MOD_ID, "my_house_piece"), (StructurePieceType.ManagerAware)(MyHouseGenerator.Piece::new));
    public static final StructureType<MyHouseStructure> TYPE = Registry.register(Registries.STRUCTURE_TYPE, new Identifier(ExampleMod.MOD_ID, "my_house"), () -> {return CODEC;});

    public MyHouseStructure(Structure.Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        // we want houses on the surface
        return getStructurePosition(context, Heightmap.Type.WORLD_SURFACE_WG, (collector) -> {
            this.addPieces(collector, context);
        });
    }

    private void addPieces(StructurePiecesCollector collector, Structure.Context context) {
        // I dunno
        ChunkPos chunkPos = context.chunkPos();
        ChunkRandom chunkRandom = context.random();
        BlockPos blockPos = new BlockPos(chunkPos.getStartX(), 90, chunkPos.getStartZ());
        BlockRotation blockRotation = BlockRotation.random(chunkRandom);
        MyHouseGenerator.addPieces(context.structureTemplateManager(), blockPos, blockRotation, collector, chunkRandom);
    }

    @Override
    public StructureType<?> getType() {
        return TYPE;
    }
}
