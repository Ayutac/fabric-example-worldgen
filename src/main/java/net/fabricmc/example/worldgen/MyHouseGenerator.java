package net.fabricmc.example.worldgen;

import net.fabricmc.example.ExampleMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.*;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class MyHouseGenerator {

    public static void addPieces(StructureTemplateManager manager, BlockPos pos, BlockRotation rotation, StructurePiecesHolder holder, Random random) {
        holder.addPiece(new Piece(manager, new Identifier(ExampleMod.MOD_ID, "my_house"), pos, rotation, 0));
    }

    public static class Piece extends SimpleStructurePiece {
        public Piece(StructureTemplateManager manager, Identifier identifier, BlockPos pos, BlockRotation rotation, int yOffset) {
            super(MyHouseStructure.PIECE_TYPE, 0, manager, identifier, identifier.toString(), createPlacementData(rotation, identifier), getPosOffset(identifier, pos, yOffset));
        }

        public Piece(StructureTemplateManager manager, NbtCompound nbt) {
            super(MyHouseStructure.PIECE_TYPE, nbt, manager, (identifier) -> {
                return createPlacementData(BlockRotation.valueOf(nbt.getString("Rot")), identifier);
            });
        }

        private static StructurePlacementData createPlacementData(BlockRotation rotation, Identifier identifier) {
            return (new StructurePlacementData()).setRotation(rotation).setMirror(BlockMirror.NONE).setPosition(new BlockPos(3, 5, 5)).addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS);
        }

        private static BlockPos getPosOffset(Identifier identifier, BlockPos pos, int yOffset) {
            return pos.add((Vec3i)BlockPos.ORIGIN).down(yOffset);
        }

        protected void writeNbt(StructureContext context, NbtCompound nbt) {
            super.writeNbt(context, nbt);
            nbt.putString("Rot", this.placementData.getRotation().name());
        }

        protected void handleMetadata(String metadata, BlockPos pos, ServerWorldAccess world, Random random, BlockBox boundingBox) {
            if ("chest".equals(metadata)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                BlockEntity blockEntity = world.getBlockEntity(pos.down());
                if (blockEntity instanceof ChestBlockEntity) {
                    ((ChestBlockEntity)blockEntity).setLootTable(LootTables.IGLOO_CHEST_CHEST, random.nextLong());
                }

            }
        }

        public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            Identifier identifier = new Identifier(this.templateIdString);
            StructurePlacementData structurePlacementData = createPlacementData(this.placementData.getRotation(), identifier);
            BlockPos blockPos = new BlockPos(3, 5, 5);
            BlockPos blockPos2 = this.pos.add(StructureTemplate.transform(structurePlacementData, new BlockPos(3 - blockPos.getX(), 0, -blockPos.getZ())));
            int i = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, blockPos2.getX(), blockPos2.getZ());
            BlockPos blockPos3 = this.pos;
            this.pos = this.pos.add(0, i - 90 - 1, 0);
            super.generate(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pivot);

            BlockPos blockPos4 = this.pos.add(StructureTemplate.transform(structurePlacementData, new BlockPos(3, 0, 5)));
            BlockState blockState = world.getBlockState(blockPos4.down());
            if (!blockState.isAir() && !blockState.isOf(Blocks.LADDER)) {
                world.setBlockState(blockPos4, Blocks.SNOW_BLOCK.getDefaultState(), 3);
            }

            this.pos = blockPos3;
        }
    }
}
