package mchorse.bbs_mod.blocks;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.network.ServerNetwork;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;

public class ModelBlock extends Block implements BlockEntityProvider, Waterloggable
{
    public static final IntProperty LIGHT_LEVEL = IntProperty.of("light_level", 0, 15);

    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> validateTicker(BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker)
    {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }

    public ModelBlock(Settings settings)
    {
        super(settings);

        this.setDefaultState(getDefaultState()
            .with(Properties.WATERLOGGED, false)
            .with(LIGHT_LEVEL, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
    {
        builder.add(Properties.WATERLOGGED, LIGHT_LEVEL);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx)
    {
        return this.getDefaultState()
            .with(Properties.WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).isOf(Fluids.WATER));
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData)
    {
        BlockEntity entity = world.getBlockEntity(pos);

        if (entity instanceof ModelBlockEntity modelBlock)
        {
            ItemStack stack = new ItemStack(this);
            stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(modelBlock.createNbtWithId(world.getRegistryManager())));
            
            stack.set(DataComponentTypes.BLOCK_STATE, new BlockStateComponent(Map.of("light_level", String.valueOf(modelBlock.getProperties().getLightLevel()))));

            return stack;
        }

        return super.getPickStack(world, pos, state, includeData);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state)
    {
        return BlockRenderType.INVISIBLE;
    }

    protected boolean isTransparent(BlockState state, BlockView world, BlockPos pos)
    {
        return true;
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos)
    {
        return 1.0F;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
    {
        return validateTicker(type, BBSMod.MODEL_BLOCK_ENTITY, (w, p, s, e) -> ModelBlockEntity.tick(w, p, s, e));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
    {
        return new ModelBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
    {
        try
        {
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof ModelBlockEntity model)
            {
                ModelProperties properties = model.getProperties();

                if (!properties.isHitbox())
                {
                    return VoxelShapes.empty();
                }
            }

            return this.getShape(world, pos);
        }
        catch (Exception e)
        {

        }

        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
    {
        try
        {
            return this.getShape(world, pos);
        }
        catch (Exception e)
        {

        }

        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos)
    {
        try
        {
            return this.getShape(world, pos);
        }
        catch (Exception e)
        {

        }

        return VoxelShapes.empty();
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos)
    {
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof ModelBlockEntity model)
        {
            float hardness = model.getProperties().getHardness();

            if (hardness <= 0F)
            {
                return 1F;
            }

            float speed = player.getBlockBreakingSpeed(state);

            if (speed <= 0F)
            {
                return 0F;
            }

            int divisor = player.canHarvest(state) ? 30 : 100;

            return speed / hardness / (float) divisor;
        }

        return super.calcBlockBreakingDelta(state, player, world, pos);
    }

    private VoxelShape getShape(BlockView world, BlockPos pos)
    {
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof ModelBlockEntity model)
        {
            ModelProperties properties = model.getProperties();

            Vector3f pos1 = properties.getHitboxPos1();
            Vector3f pos2 = properties.getHitboxPos2();

            double minX = Math.min(pos1.x, pos2.x);
            double minY = Math.min(pos1.y, pos2.y);
            double minZ = Math.min(pos1.z, pos2.z);
            double maxX = Math.max(pos1.x, pos2.x);
            double maxY = Math.max(pos1.y, pos2.y);
            double maxZ = Math.max(pos1.z, pos2.z);

            minX = Math.max(0D, minX);
            minY = Math.max(0D, minY);
            minZ = Math.max(0D, minZ);
            maxX = Math.min(1D, maxX);
            maxY = Math.min(1D, maxY);
            maxZ = Math.min(1D, maxZ);

            if (minX < maxX && minY < maxY && minZ < maxZ)
            {
                return VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ);
            }
        }

        return VoxelShapes.fullCube();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit)
    {
        if (player instanceof ServerPlayerEntity serverPlayer)
        {
            ServerNetwork.sendClickedModelBlock(serverPlayer, pos);
        }

        return ActionResult.SUCCESS;
    }

    /* Waterloggable implementation */

    @Override
    public FluidState getFluidState(BlockState state)
    {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity be, ItemStack tool)
    {
        if (!world.isClient && !player.getAbilities().creativeMode)
        {
            if (be instanceof ModelBlockEntity model)
            {
                ItemStack stack = new ItemStack(this);
                stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(model.createNbtWithId(world.getRegistryManager())));
                
                stack.set(DataComponentTypes.BLOCK_STATE, new BlockStateComponent(Map.of("light_level", String.valueOf(model.getProperties().getLightLevel()))));

                ItemScatterer.spawn(world, pos, DefaultedList.ofSize(1, stack));
            }
        }

        super.afterBreak(world, player, pos, state, be, tool);
    }
}
