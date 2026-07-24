package mchorse.bbs_mod.forms.structure;

import mchorse.bbs_mod.items.StructurePickerExporter;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cached structure-local collision boxes (after structure pivot, before form/model transforms).
 */
public final class StructureCollisionData
{
    private static final Map<String, StructureCollisionData> CACHE = new ConcurrentHashMap<>();

    public final List<Box> localBoxes;
    public final Box localBounds;

    private StructureCollisionData(List<Box> localBoxes, Box localBounds)
    {
        this.localBoxes = List.copyOf(localBoxes);
        this.localBounds = localBounds;
    }

    public static StructureCollisionData get(String structurePath)
    {
        if (structurePath == null || structurePath.isEmpty())
        {
            return null;
        }

        return CACHE.computeIfAbsent(structurePath, StructureCollisionData::build);
    }

    public static void invalidate(String structurePath)
    {
        if (structurePath != null)
        {
            CACHE.remove(structurePath);
        }
    }

    private static StructureCollisionData build(String path)
    {
        NbtCompound root = StructurePickerExporter.readStructureNbt(path);

        if (root == null || !root.contains("blocks", NbtElement.LIST_TYPE) || !root.contains("palette", NbtElement.LIST_TYPE))
        {
            return new StructureCollisionData(List.of(), new Box(0, 0, 0, 0, 0, 0));
        }

        List<BlockState> palette = new ArrayList<>();
        NbtList paletteNbt = root.getList("palette", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < paletteNbt.size(); i++)
        {
            palette.add(readBlockState(paletteNbt.getCompound(i)));
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        List<BlockPos> solid = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        NbtList blocks = root.getList("blocks", NbtElement.COMPOUND_TYPE);
        boolean hasAny = false;

        for (int i = 0; i < blocks.size(); i++)
        {
            NbtCompound entry = blocks.getCompound(i);
            int stateIndex = entry.getInt("state");

            if (stateIndex < 0 || stateIndex >= palette.size())
            {
                continue;
            }

            BlockState state = palette.get(stateIndex);

            if (state == null || state.isAir())
            {
                continue;
            }

            NbtList posList = entry.getList("pos", NbtElement.INT_TYPE);

            if (posList.size() < 3)
            {
                continue;
            }

            BlockPos pos = new BlockPos(posList.getInt(0), posList.getInt(1), posList.getInt(2));

            /* Bounds from all non-air blocks — same pivot basis as StructureFormRenderer. */
            hasAny = true;
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());

            VoxelShape shape = state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);

            if (shape.isEmpty())
            {
                continue;
            }

            solid.add(pos);
            states.add(state);
        }

        if (!hasAny || solid.isEmpty())
        {
            return new StructureCollisionData(List.of(), new Box(0, 0, 0, 0, 0, 0));
        }

        /* Same pivot as StructureFormRenderer.calculateRenderInfo / getStructurePivot. */
        float pivotX = (minX + maxX) / 2F;
        float pivotY = minY;
        float pivotZ = (minZ + maxZ) / 2F;
        int widthX = maxX - minX + 1;
        int widthZ = maxZ - minZ + 1;
        float parityX = (widthX % 2 == 1) ? -0.5F : 0F;
        float parityZ = (widthZ % 2 == 1) ? -0.5F : 0F;

        pivotX -= parityX;
        pivotZ -= parityZ;

        List<Box> boxes = new ArrayList<>();
        double boundsMinX = Double.POSITIVE_INFINITY;
        double boundsMinY = Double.POSITIVE_INFINITY;
        double boundsMinZ = Double.POSITIVE_INFINITY;
        double boundsMaxX = Double.NEGATIVE_INFINITY;
        double boundsMaxY = Double.NEGATIVE_INFINITY;
        double boundsMaxZ = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < solid.size(); i++)
        {
            BlockPos pos = solid.get(i);
            BlockState state = states.get(i);
            VoxelShape shape = state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);

            for (Box part : shape.getBoundingBoxes())
            {
                Box local = new Box(
                    pos.getX() - pivotX + part.minX,
                    pos.getY() - pivotY + part.minY,
                    pos.getZ() - pivotZ + part.minZ,
                    pos.getX() - pivotX + part.maxX,
                    pos.getY() - pivotY + part.maxY,
                    pos.getZ() - pivotZ + part.maxZ
                );

                boxes.add(local);
                boundsMinX = Math.min(boundsMinX, local.minX);
                boundsMinY = Math.min(boundsMinY, local.minY);
                boundsMinZ = Math.min(boundsMinZ, local.minZ);
                boundsMaxX = Math.max(boundsMaxX, local.maxX);
                boundsMaxY = Math.max(boundsMaxY, local.maxY);
                boundsMaxZ = Math.max(boundsMaxZ, local.maxZ);
            }
        }

        Box bounds = new Box(boundsMinX, boundsMinY, boundsMinZ, boundsMaxX, boundsMaxY, boundsMaxZ);

        return new StructureCollisionData(boxes, bounds);
    }

    private static BlockState readBlockState(NbtCompound entry)
    {
        String name = entry.getString("Name");
        Block block;

        try
        {
            block = Registries.BLOCK.get(Identifier.of(name));

            if (block == null)
            {
                block = Blocks.AIR;
            }
        }
        catch (Exception e)
        {
            block = Blocks.AIR;
        }

        if ("minecraft:jigsaw".equals(name) || block == Blocks.JIGSAW)
        {
            return Blocks.AIR.getDefaultState();
        }

        BlockState state = block.getDefaultState();

        if (entry.contains("Properties", NbtElement.COMPOUND_TYPE))
        {
            NbtCompound props = entry.getCompound("Properties");

            for (String key : props.getKeys())
            {
                String value = props.getString(key);
                Property<?> property = block.getStateManager().getProperty(key);

                if (property == null)
                {
                    continue;
                }

                Optional<?> parsed = property.parse(value);

                if (parsed.isPresent())
                {
                    try
                    {
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        Property raw = property;
                        @SuppressWarnings("unchecked")
                        Comparable c = (Comparable) parsed.get();

                        state = state.with(raw, c);
                    }
                    catch (Exception ignored)
                    {}
                }
            }
        }

        return state;
    }
}
