package com.wave100.item;

import com.wave100.entity.WaveMotorcycleEntity;
import com.wave100.registry.WaveEntities;
import com.wave100.registry.WaveSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The deployable Wave motorcycle item.
 *
 * <p>Right click on suitable terrain to place a fully fueled motorcycle. The
 * spawn is validated against block collisions so the bike never appears
 * inside walls.</p>
 */
public class WaveItem extends Item {

    public WaveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos spawnPos = clicked.relative(context.getClickedFace());
        Player player = context.getPlayer();

        // do not spawn inside a solid block (e.g. against a wall)
        BlockState state = level.getBlockState(spawnPos);
        if (state.isSolidRender(level, spawnPos)) {
            return InteractionResult.FAIL;
        }

        double x = spawnPos.getX() + 0.5;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5;

        WaveMotorcycleEntity bike = new WaveMotorcycleEntity(WaveEntities.WAVE_MOTORCYCLE.get(), level);
        float yaw = player != null ? player.getYRot() : 0.0F;
        bike.moveTo(x, y, z, yaw, 0.0F);
        bike.setYRot(yaw);
        bike.yRotO = yaw;

        if (!level.noCollision(bike, bike.getBoundingBox())) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            if (player != null) {
                bike.setOwner(player.getUUID());
            }
            bike.setFuel((float) com.wave100.WaveConfig.Fuel.capacity());
            bike.setHealth((float) com.wave100.WaveConfig.Damage.maxHealth());
            level.addFreshEntity(bike);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, x, y, z, WaveSounds.REPAIR.get(),
                        SoundSource.BLOCKS, 0.9F, 0.7F);
            }
            if (player != null && !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.wave100.place").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.wave100.ride").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
