package mchorse.bbs_mod.blocks.entities;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.settings.values.misc.ValueVector3f;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.triggers.Trigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import mchorse.bbs_mod.events.TriggerBlockEntityUpdateCallback;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ServerNetwork;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TriggerBlockEntity extends BlockEntity
{
    public final ValueList<Trigger> left = new ValueList<Trigger>("left")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueList<Trigger> right = new ValueList<Trigger>("right")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueList<Trigger> enter = new ValueList<Trigger>("enter")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueList<Trigger> exit = new ValueList<Trigger>("exit")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueList<Trigger> whileIn = new ValueList<Trigger>("whileIn")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueBoolean collidable = new ValueBoolean("collidable", false);
    public final ValueBoolean region = new ValueBoolean("region", false);
    public final ValueInt regionDelay = new ValueInt("regionDelay", 15);
    public final ValueVector3f pos1 = new ValueVector3f("pos1", new Vector3f(0, 0, 0));
    public final ValueVector3f pos2 = new ValueVector3f("pos2", new Vector3f(1, 1, 1));
    public final ValueVector3f regionOffset = new ValueVector3f("regionOffset", new Vector3f(0, 0, 0));
    public final ValueVector3f regionSize = new ValueVector3f("regionSize", new Vector3f(1, 1, 1));

    private Set<UUID> playersInRegion = new HashSet<>();
    private java.util.Map<UUID, Long> regionNextTriggerTick = new java.util.HashMap<>();

    public TriggerBlockEntity(BlockPos pos, BlockState state)
    {
        super(BBSMod.TRIGGER_BLOCK_ENTITY, pos, state);
    }

    public void trigger(ServerPlayer player, boolean rightClick)
    {
        this.trigger(player, rightClick ? this.right.getList() : this.left.getList());
    }

    public void trigger(ServerPlayer player, List<Trigger> triggers)
    {
        for (Trigger trigger : triggers)
        {
            String type = trigger.type.get();
            
            if (type.equals("command"))
            {
                String cmd = trigger.command.get();
                
                if (!cmd.isEmpty())
                {
                    try
                    {
                        player.createCommandSourceStack().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), cmd);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            else if (type.equals("form"))
            {
                Form form = trigger.form.get();

                ServerNetwork.sendMorphToTracked(player, form);
                Morph.getMorph(player).setForm(FormUtils.copy(form));
            }
            else if (type.equals("block"))
            {
                int x = trigger.x.get();
                int y = trigger.y.get();
                int z = trigger.z.get();
                Form form = trigger.blockForm.get();
                
                BlockPos pos = new BlockPos(x, y, z);
                
                if (this.level.isLoaded(pos))
                {
                    BlockEntity be = this.level.getBlockEntity(pos);
                    
                    if (be instanceof ModelBlockEntity modelBlock)
                    {
                        modelBlock.getProperties().setForm(FormUtils.copy(form));
                        modelBlock.setChanged();
                        this.level.sendBlockUpdated(pos, this.level.getBlockState(pos), this.level.getBlockState(pos), 3);
                    }
                }
            }
        }
    }
    
    public static void tick(Level world, BlockPos pos, BlockState state, TriggerBlockEntity blockEntity)
    {
        if (!world.isClientSide() && blockEntity.region.get())
        {
            blockEntity.tickRegion();
        }

        TriggerBlockEntityUpdateCallback.EVENT.invoker().update(blockEntity);
    }

    public AABB getRegionBox()
    {
        return this.getRegionBox(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ());
    }

    public AABB getRegionBoxRelative()
    {
        return this.getRegionBox(0, 0, 0);
    }

    public AABB getRegionBox(double x, double y, double z)
    {
        Vector3f offset = this.regionOffset.get();
        Vector3f size = this.regionSize.get();

        /* Slightly expand the region box so it's bigger than the 1x1 hitbox by default */
        double expansion = 1.0;
        double minX = offset.x + 0.5 - size.x / 2.0 - expansion;
        double minY = offset.y + 0.5 - size.y / 2.0 - expansion;
        double minZ = offset.z + 0.5 - size.z / 2.0 - expansion;
        double maxX = offset.x + 0.5 + size.x / 2.0 + expansion;
        double maxY = offset.y + 0.5 + size.y / 2.0 + expansion;
        double maxZ = offset.z + 0.5 + size.z / 2.0 + expansion;

        return new AABB(
            x + minX, y + minY, z + minZ,
            x + maxX, y + maxY, z + maxZ
        );
    }

    private void tickRegion()
    {
        AABB box = this.getRegionBox();
        List<ServerPlayer> players = this.level.getEntitiesOfClass(ServerPlayer.class, box, (p) -> true);
        Set<UUID> currentPlayers = new HashSet<>();
        long time = this.level.getGameTime();

        for (ServerPlayer player : players)
        {
            UUID uuid = player.getUUID();
            currentPlayers.add(uuid);

            boolean isNew = !this.playersInRegion.contains(uuid);
            long nextTick = this.regionNextTriggerTick.getOrDefault(uuid, 0L);

            if (isNew)
            {
                this.trigger(player, this.enter.getList());
                this.regionNextTriggerTick.put(uuid, time + this.regionDelay.get());
            }
            else if (time >= nextTick)
            {
                this.trigger(player, this.whileIn.getList());
                this.regionNextTriggerTick.put(uuid, time + this.regionDelay.get());
            }
        }

        for (UUID uuid : this.playersInRegion)
        {
            if (!currentPlayers.contains(uuid))
            {
                ServerPlayer player = (ServerPlayer) this.level.getPlayerByUUID(uuid);

                if (player != null)
                {
                    this.trigger(player, this.exit.getList());
                }
                
                this.regionNextTriggerTick.remove(uuid);
            }
        }

        this.playersInRegion = currentPlayers;
    }

    @Override
    protected void loadAdditional(ValueInput view)
    {
        super.loadAdditional(view);
        
        view.getString("Left").ifPresent((value) -> {
            BaseType type = DataToString.fromString(value);

            if (type != null) this.left.fromData(type);
        });
        view.getString("Right").ifPresent((value) -> {
            BaseType type = DataToString.fromString(value);

            if (type != null) this.right.fromData(type);
        });
        view.getString("Enter").ifPresent((value) -> {
            BaseType type = DataToString.fromString(value);

            if (type != null) this.enter.fromData(type);
        });
        view.getString("Exit").ifPresent((value) -> {
            BaseType type = DataToString.fromString(value);

            if (type != null) this.exit.fromData(type);
        });
        view.getString("WhileIn").ifPresent((value) -> {
            BaseType type = DataToString.fromString(value);

            if (type != null) this.whileIn.fromData(type);
        });
        this.regionDelay.set(view.getIntOr("RegionDelay", this.regionDelay.get()));
        this.collidable.set(view.getBooleanOr("Collidable", this.collidable.get()));
        this.region.set(view.getBooleanOr("Region", this.region.get()));
        view.getString("Pos1").ifPresent((value) -> {
            BaseType type = DataToString.fromString(value);

            if (type != null) this.pos1.fromData(type);
        });
        view.getString("Pos2").ifPresent((value) -> {
            BaseType type = DataToString.fromString(value);

            if (type != null) this.pos2.fromData(type);
        });
        view.getString("RegionOffset").ifPresent((value) -> {
            BaseType type = DataToString.fromString(value);

            if (type != null) this.regionOffset.fromData(type);
        });
        view.getString("RegionSize").ifPresent((value) -> {
            BaseType type = DataToString.fromString(value);

            if (type != null) this.regionSize.fromData(type);
        });
    }

    @Override
    protected void saveAdditional(ValueOutput view)
    {
        super.saveAdditional(view);
        
        view.putString("Left", DataToString.toString(this.left.toData()));
        view.putString("Right", DataToString.toString(this.right.toData()));
        view.putString("Enter", DataToString.toString(this.enter.toData()));
        view.putString("Exit", DataToString.toString(this.exit.toData()));
        view.putString("WhileIn", DataToString.toString(this.whileIn.toData()));
        view.putInt("RegionDelay", this.regionDelay.get());
        view.putBoolean("Collidable", this.collidable.get());
        view.putBoolean("Region", this.region.get());
        view.putString("Pos1", DataToString.toString(this.pos1.toData()));
        view.putString("Pos2", DataToString.toString(this.pos2.toData()));
        view.putString("RegionOffset", DataToString.toString(this.regionOffset.toData()));
        view.putString("RegionSize", DataToString.toString(this.regionSize.toData()));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup)
    {
        return this.saveWithoutMetadata(registryLookup);
    }
}
