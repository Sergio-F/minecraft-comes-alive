package mca.items;

import java.text.DecimalFormat;
import java.util.List;

import mca.core.MCA;
import mca.core.minecraft.ModAchievements;
import mca.data.PlayerData;
import mca.data.PlayerMemory;
import mca.entity.EntityHuman;
import mca.enums.EnumDialogueType;
import mca.packets.PacketOpenBabyNameGUI;
import mca.util.TutorialManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import radixcore.constant.Font.Color;
import radixcore.constant.Font.Format;
import radixcore.constant.Time;

public class ItemBaby extends Item 
{
	private final boolean isBoy;

	public ItemBaby(boolean isBoy)
	{
		final String itemName = isBoy ? "BabyBoy" : "BabyGirl";

		this.isBoy = isBoy;
		this.setCreativeTab(MCA.getCreativeTabMain());
		this.setMaxStackSize(1);
		this.setUnlocalizedName(itemName);
//		this.setTextureName("mca:" + itemName);

		GameRegistry.registerItem(this, itemName);
	}


	@Override
	public void onUpdate(ItemStack itemStack, World world, Entity entity, int unknownInt, boolean unknownBoolean)
	{
		super.onUpdate(itemStack, world, entity, unknownInt, unknownBoolean);

		if (!world.isRemote)
		{
			if (!itemStack.hasTagCompound())
			{
				EntityPlayer player = (EntityPlayer) entity;

				itemStack.setTagCompound(new NBTTagCompound());
				itemStack.getTagCompound().setString("name", "Unnamed");
				itemStack.getTagCompound().setInteger("age", 0);
				itemStack.getTagCompound().setString("owner", player.getName());

				if (player.capabilities.isCreativeMode)
				{
					TutorialManager.sendMessageToPlayer(player, "You can name a baby retrieved from", "creative mode by right-clicking the air.");
				}
			}

			else 
			{
				updateBabyGrowth(itemStack);
			}
		}
	}


	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World worldObj, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) 
	{
		if (!worldObj.isRemote && isReadyToGrowUp(stack))
		{
			ItemBaby baby = (ItemBaby)stack.getItem();
			PlayerData data = MCA.getPlayerData(player);
			EntityHuman playerSpouse = MCA.getHumanByPermanentId(data.spousePermanentId.getInt());
			boolean isPlayerMale = data.isMale.getBoolean();

			String motherName = "N/A";
			int motherId = 0;
			String fatherName = "N/A";
			int fatherId = 0;

			if (isPlayerMale)
			{
				motherName = data.spouseName.getString();
				motherId = data.spousePermanentId.getInt();
				fatherName = player.getName();
				fatherId = data.permanentId.getInt();
			}
			
			else
			{
				fatherName = data.spouseName.getString();
				fatherId = data.spousePermanentId.getInt();
				motherName = player.getName();
				motherId = data.permanentId.getInt();				
			}

			final EntityHuman child = new EntityHuman(worldObj, baby.isBoy, true, motherName, fatherName, motherId, fatherId, true);
			child.setPosition(pos.getX(), pos.getY() + 1, pos.getZ());
			child.setName(stack.getTagCompound().getString("name"));
			worldObj.spawnEntityInWorld(child);
			
			PlayerMemory childMemory = child.getPlayerMemory(player);
			childMemory.setHearts(100);
			childMemory.setDialogueType(EnumDialogueType.PLAYERCHILD);

			player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
			player.triggerAchievement(ModAchievements.babyToChild);
			
			data.shouldHaveBaby.setValue(false);
		}

		return true;
	}	


	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) 
	{
		if (!world.isRemote && itemStack.getTagCompound().getString("name").equals("Unnamed"))
		{
			ItemBaby baby = (ItemBaby) itemStack.getItem();
			MCA.getPacketHandler().sendPacketToPlayer(new PacketOpenBabyNameGUI(baby.isBoy), (EntityPlayerMP)player);
		}

		return super.onItemRightClick(itemStack, world, player);
	}


	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem) 
	{
		updateBabyGrowth(entityItem.getEntityItem());
		return super.onEntityItemUpdate(entityItem);
	}


	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List infoList, boolean unknown)
	{
		super.addInformation(itemStack, entityPlayer, infoList, unknown);

		DecimalFormat nearestTenth = new DecimalFormat("0.0");

		if (itemStack.hasTagCompound())
		{
			//Text color is blue for boys, purple for girls.
			String textColor = ((ItemBaby)itemStack.getItem()).isBoy ? Color.AQUA : Color.LIGHTPURPLE;
			float ageInMinutes = (float)itemStack.getTagCompound().getInteger("age") / Time.MINUTE;

			//Owner name is You for the current owner. Otherwise, the player's name.
			String ownerName = itemStack.getTagCompound().getString("owner");
			ownerName = ownerName.equals(entityPlayer.getName()) ? "You" : ownerName;

			infoList.add(textColor + "Name: " + Format.RESET + itemStack.getTagCompound().getString("name"));
			infoList.add(textColor + "Age: "  + Format.RESET + nearestTenth.format(ageInMinutes) + " minutes.");
			infoList.add(textColor + "Parent: " + Format.RESET + ownerName);

			if (isReadyToGrowUp(itemStack))
			{
				infoList.add(Color.GREEN + "Ready to grow up!");
			}
		}
	}

	private void updateBabyGrowth(ItemStack itemStack)
	{
		if (itemStack.hasTagCompound())
		{
			int age = itemStack.getTagCompound().getInteger("age");
			age++;
			itemStack.getTagCompound().setInteger("age", age);
		}
	}

	private boolean isReadyToGrowUp(ItemStack itemStack)
	{
		if (itemStack.hasTagCompound())
		{
			final float ageInMinutes = (float)itemStack.getTagCompound().getInteger("age") / Time.MINUTE;
			return ageInMinutes >= MCA.getConfig().babyGrowUpTime;
		}

		return false;
	}

	public boolean getIsBoy()
	{
		return isBoy;
	}
}
