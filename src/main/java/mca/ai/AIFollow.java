package mca.ai;

import mca.core.Constants;
import mca.data.WatcherIDsHuman;
import mca.entity.EntityHuman;
import mca.enums.EnumMovementState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import radixcore.data.WatchedString;
import radixcore.util.BlockPosHelper;
import radixcore.util.RadixMath;

public class AIFollow extends AbstractAI
{
	private final WatchedString playerFollowingName;

	public AIFollow(EntityHuman entityHuman) 
	{
		super(entityHuman);
		playerFollowingName = new WatchedString("EMPTY", WatcherIDsHuman.PLAYER_FOLLOWING_NAME, owner.getDataWatcherEx());
	}

	@Override
	public void reset() 
	{	
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) 
	{
		nbt.setString("playerFollowingName", playerFollowingName.getString());
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) 
	{
		playerFollowingName.setValue(nbt.getString("playerFollowingName"));
	}

	@Override
	public void onUpdateCommon() 
	{
	}

	@Override
	public void onUpdateClient() 
	{
	}

	@Override
	public void onUpdateServer() 
	{
		if (owner.getMovementState() == EnumMovementState.FOLLOW)
		{
			final EntityLiving entityPathController = (EntityLiving) (owner.ridingEntity instanceof EntityHorse ? owner.ridingEntity : owner);
			final EntityPlayer entityPlayer = owner.worldObj.getPlayerEntityByName(playerFollowingName.getString());

			if (entityPlayer != null)
			{
				entityPathController.getLookHelper().setLookPositionWithEntity(entityPlayer, 10.0F, owner.getVerticalFaceSpeed());
				
				final double distanceToPlayer = RadixMath.getDistanceToEntity(owner, entityPlayer);

				if (distanceToPlayer >= 10.0D)
				{
					final int playerX = net.minecraft.util.MathHelper.floor_double(entityPlayer.posX) - 2;
					final int playerY = net.minecraft.util.MathHelper.floor_double(entityPlayer.getBoundingBox().minY);
					final int playerZ = net.minecraft.util.MathHelper.floor_double(entityPlayer.posZ) - 2;

					for (int i = 0; i <= 4; ++i)
					{
						for (int i2 = 0; i2 <= 4; ++i2)
						{
							if ((i < 1 || i2 < 1 || i > 3 || i2 > 3) && World.doesBlockHaveSolidTopSurface(owner.worldObj, new BlockPos(playerX + i, playerY - 1, playerZ + i2)) && !BlockPosHelper.getBlock(owner.worldObj, playerX + i, playerY, playerZ + i2).isNormalCube() && !BlockPosHelper.getBlock(owner.worldObj, playerX + i, playerY + 1, playerZ + i2).isNormalCube())
							{
								entityPathController.setLocationAndAngles(playerX + i + 0.5F, playerY, playerZ + i2 + 0.5F, entityPlayer.rotationYaw, entityPlayer.rotationPitch);
								entityPathController.getNavigator().clearPathEntity();
							}
						}
					}
				}

				else if (distanceToPlayer >= 4.5D && owner.getNavigator().noPath())
				{
					float speed = entityPathController instanceof EntityHorse ? Constants.SPEED_HORSE_RUN :  entityPlayer.isSprinting() ? Constants.SPEED_SPRINT : owner.getSpeed();
					entityPathController.getNavigator().tryMoveToEntityLiving(entityPlayer, speed);
				}

				else if (distanceToPlayer <= 2.0D) //To avoid crowding the player.
				{
					entityPathController.getNavigator().clearPathEntity();
				}
			}

			else
			{
				owner.setMovementState(EnumMovementState.MOVE);
			}
		}
	}

	public String getPlayerFollowingName()
	{
		return playerFollowingName.getString();
	}

	public void setPlayerFollowingName(String value)
	{
		playerFollowingName.setValue(value);
	}
}
