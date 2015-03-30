package mca.entity;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

import mca.ai.AIBlink;
import mca.ai.AIBuild;
import mca.ai.AIConverse;
import mca.ai.AICooking;
import mca.ai.AIDefend;
import mca.ai.AIEat;
import mca.ai.AIFarming;
import mca.ai.AIFollow;
import mca.ai.AIGreet;
import mca.ai.AIGrow;
import mca.ai.AIHunting;
import mca.ai.AIIdle;
import mca.ai.AIManager;
import mca.ai.AIMining;
import mca.ai.AIMood;
import mca.ai.AIPatrol;
import mca.ai.AIProcreate;
import mca.ai.AIProgressStory;
import mca.ai.AIRegenerate;
import mca.ai.AIRespondToAttack;
import mca.ai.AISleep;
import mca.ai.AIWoodcutting;
import mca.ai.AbstractAI;
import mca.core.Constants;
import mca.core.MCA;
import mca.core.minecraft.ModItems;
import mca.data.PlayerData;
import mca.data.PlayerMemory;
import mca.data.PlayerMemoryHandler;
import mca.data.WatcherIDsHuman;
import mca.enums.EnumBabyState;
import mca.enums.EnumMovementState;
import mca.enums.EnumPersonality;
import mca.enums.EnumProfession;
import mca.enums.EnumProfessionGroup;
import mca.enums.EnumProgressionStep;
import mca.enums.EnumSleepingState;
import mca.packets.PacketOpenGUIOnEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIRestrictOpenDoor;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITradePlayer;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import radixcore.constant.Font.Color;
import radixcore.data.DataWatcherEx;
import radixcore.data.IPermanent;
import radixcore.data.IWatchable;
import radixcore.data.WatchedBoolean;
import radixcore.data.WatchedFloat;
import radixcore.data.WatchedInt;
import radixcore.data.WatchedString;
import radixcore.inventory.Inventory;
import radixcore.network.ByteBufIO;
import radixcore.util.RadixLogic;
import radixcore.util.RadixMath;

public class EntityHuman extends EntityVillager implements IWatchable, IPermanent, IEntityAdditionalSpawnData
{
	private final WatchedString name;
	private final WatchedString skin;
	private final WatchedInt professionId;
	private final WatchedInt personalityId;
	private final WatchedInt permanentId;
	private final WatchedBoolean isMale;
	private final WatchedBoolean isEngaged;
	private final WatchedInt spouseId;
	private final WatchedString spouseName;
	private final WatchedInt babyState;
	private final WatchedInt movementState;
	private final WatchedBoolean isChild;
	private final WatchedInt age;
	private final WatchedString parentNames;
	private final WatchedString parentIDs;
	private final WatchedBoolean isInteracting;
	private final WatchedFloat scaleHeight;
	private final WatchedFloat scaleGirth;
	private final WatchedBoolean doDisplay;
	private final WatchedBoolean isSwinging;
	private final WatchedInt heldItem;

	private final Inventory inventory;

	@SideOnly(Side.CLIENT)
	public boolean displayNameForPlayer;

	protected int ticksAlive;
	protected int swingProgressTicks;
	protected AIManager aiManager;
	protected Map<String, PlayerMemory> playerMemories;
	protected DataWatcherEx dataWatcherEx;

	public EntityHuman(World world) 
	{
		super(world);
		dataWatcherEx = new DataWatcherEx(this, MCA.ID);
		playerMemories = new HashMap<String, PlayerMemory>();

		isMale = new WatchedBoolean(RadixLogic.getBooleanWithProbability(50), WatcherIDsHuman.IS_MALE, dataWatcherEx);
		name = new WatchedString(getRandomName(), WatcherIDsHuman.NAME, dataWatcherEx);
		professionId = new WatchedInt(EnumProfession.getAtRandom().getId(), WatcherIDsHuman.PROFESSION, dataWatcherEx);
		personalityId = new WatchedInt(EnumPersonality.getAtRandom().getId(), WatcherIDsHuman.PERSONALITY_ID, dataWatcherEx);
		permanentId = new WatchedInt(RadixLogic.generatePermanentEntityId(this), WatcherIDsHuman.PERMANENT_ID, dataWatcherEx);
		skin = new WatchedString(getRandomSkin(), WatcherIDsHuman.SKIN, dataWatcherEx);
		isEngaged = new WatchedBoolean(false, WatcherIDsHuman.IS_ENGAGED, dataWatcherEx);
		spouseId = new WatchedInt(0, WatcherIDsHuman.SPOUSE_ID, dataWatcherEx);
		spouseName = new WatchedString("null", WatcherIDsHuman.SPOUSE_NAME, dataWatcherEx);
		babyState = new WatchedInt(EnumBabyState.NONE.getId(), WatcherIDsHuman.BABY_STATE, dataWatcherEx);
		movementState = new WatchedInt(EnumMovementState.MOVE.getId(), WatcherIDsHuman.MOVEMENT_STATE, dataWatcherEx);
		isChild = new WatchedBoolean(false, WatcherIDsHuman.IS_CHILD, dataWatcherEx);
		age = new WatchedInt(0, WatcherIDsHuman.AGE, dataWatcherEx);
		parentNames = new WatchedString("null", WatcherIDsHuman.PARENT_NAMES, dataWatcherEx);
		parentIDs = new WatchedString("null", WatcherIDsHuman.PARENT_IDS, dataWatcherEx);
		isInteracting = new WatchedBoolean(false, WatcherIDsHuman.IS_INTERACTING, dataWatcherEx);
		scaleHeight = new WatchedFloat(RadixMath.getNumberInRange(-0.03F, 0.18F), WatcherIDsHuman.HEIGHT, dataWatcherEx);
		scaleGirth = new WatchedFloat(RadixMath.getNumberInRange(-0.01F, 0.5F), WatcherIDsHuman.GIRTH, dataWatcherEx);
		doDisplay = new WatchedBoolean(false, WatcherIDsHuman.DO_DISPLAY, dataWatcherEx);
		isSwinging = new WatchedBoolean(false, WatcherIDsHuman.IS_SWINGING, dataWatcherEx);
		heldItem = new WatchedInt(-1, WatcherIDsHuman.HELD_ITEM, dataWatcherEx);

		aiManager = new AIManager(this);
		aiManager.addAI(new AIIdle(this));
		aiManager.addAI(new AIRegenerate(this));
		aiManager.addAI(new AISleep(this));
		aiManager.addAI(new AIFollow(this));
		aiManager.addAI(new AIEat(this));
		aiManager.addAI(new AIGreet(this, playerMemories));
		aiManager.addAI(new AIProgressStory(this));
		aiManager.addAI(new AIProcreate(this));
		aiManager.addAI(new AIRespondToAttack(this));
		aiManager.addAI(new AIPatrol(this));
		aiManager.addAI(new AIGrow(this));
		aiManager.addAI(new AIMood(this));
		aiManager.addAI(new AIConverse(this));
		aiManager.addAI(new AIBlink(this));
		aiManager.addAI(new AIBuild(this));
		aiManager.addAI(new AIMining(this));
		aiManager.addAI(new AIWoodcutting(this));
		aiManager.addAI(new AIHunting(this));
		aiManager.addAI(new AICooking(this));
		aiManager.addAI(new AIFarming(this));
		aiManager.addAI(new AIDefend(this));
		
		addAI();

		if (!worldObj.isRemote)
		{
			doDisplay.setValue(true);
		}

		inventory = new Inventory("Villager Inventory", false, 41);
	}

	public double getBaseAttackDamage() 
	{
		switch (getPersonality())
		{
		case STRONG: return 2.0D;
		case CONFIDENT: return 1.0D;
		default: 
			if (getProfessionGroup() == EnumProfessionGroup.Guard)
			{
				return 5.0D;
			}

			else
			{
				return 0.5D;
			}
		}
	}

	public EntityHuman(World world, boolean isMale)
	{
		this(world);

		this.isMale.setValue(isMale);
		this.name.setValue(getRandomName());
		this.skin.setValue(getRandomSkin());
	}

	public EntityHuman(World world, boolean isMale, int profession, boolean isOverwrite)
	{
		this(world, isMale);

		if (isOverwrite)
		{
			this.professionId.setValue(EnumProfession.getNewProfessionFromVanilla(profession).getId());
		}

		else
		{
			this.professionId.setValue(profession);
		}

		this.skin.setValue(this.getRandomSkin());
	}

	private EntityHuman(World world, boolean isMale, boolean isChild)
	{
		this(world, isMale);
		this.isChild.setValue(isChild);
	}

	public EntityHuman(World world, boolean isMale, boolean isChild, String motherName, String fatherName, int motherId, int fatherId, boolean isPlayerChild)
	{
		this(world, isMale, isChild);

		this.parentNames.setValue(fatherName + "|" + motherName);
		this.parentIDs.setValue(fatherId + "|" + motherId);

		if (isPlayerChild)
		{
			this.professionId.setValue(EnumProfession.Child.getId());
			this.skin.setValue(this.getRandomSkin());
		}
	}

	public void addAI()
	{
		this.tasks.taskEntries.clear();

		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new EntityAITradePlayer(this));
		this.tasks.addTask(3, new EntityAIRestrictOpenDoor(this));
		this.tasks.addTask(4, new EntityAIOpenDoor(this, true));
		this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, getSpeed()));
		this.tasks.addTask(9, new EntityAIWatchClosest2(this, EntityPlayer.class, 3.0F, 1.0F));
		this.tasks.addTask(9, new EntityAIWatchClosest2(this, EntityHuman.class, 5.0F, 0.02F));
		this.tasks.addTask(9, new EntityAIWander(this, getSpeed()));
		this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
		
		int maxHealth = this.getProfessionGroup() == EnumProfessionGroup.Guard ? MCA.getConfig().guardMaxHealth : MCA.getConfig().villagerMaxHealth;
		getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(maxHealth);
		
		if (this.getHealth() > maxHealth || this.getProfessionGroup() == EnumProfessionGroup.Guard)
		{
			this.setHealth(maxHealth);
		}
	}

	public String getRandomSkin()
	{
		final EnumProfessionGroup professionGroup = EnumProfession.getProfessionById(professionId.getInt()).getSkinGroup();
		return isMale.getBoolean() ? professionGroup.getMaleSkin() : professionGroup.getFemaleSkin();
	}

	private String getRandomName()
	{
		if (isMale.getBoolean())
		{
			return MCA.getLanguageManager().getString("name.male");
		}

		else
		{
			return MCA.getLanguageManager().getString("name.female");
		}
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		aiManager.onUpdate();

		if (!worldObj.isRemote)
		{
			ticksAlive++;

			//Reset interacting state by checking if any players are still nearby.
			if (isInteracting.getBoolean())
			{
				EntityPlayer nearestPlayer = worldObj.getClosestPlayerToEntity(this, 10.0D);

				if (nearestPlayer == null)
				{
					isInteracting.setValue(false);
				}
			}

			for (PlayerMemory memory : this.playerMemories.values())
			{
				memory.doTick();
			}
		}

		else
		{
			updateSwinging();
		}
	}

	private void updateSwinging()
	{
		if (isSwinging.getBoolean())
		{
			swingProgressTicks++;

			if (swingProgressTicks >= 8)
			{
				swingProgressTicks = 0;

				if (!DataWatcherEx.allowClientSideModification)
				{
					DataWatcherEx.allowClientSideModification = true;
					isSwinging.setValue(false);
					DataWatcherEx.allowClientSideModification = false;
				}
				
				else
				{
					isSwinging.setValue(false);					
				}
			}
		}

		else
		{
			swingProgressTicks = 0;
		}

		swingProgress = (float) swingProgressTicks / (float) 8;
	}

	@Override
	public boolean interact(EntityPlayer player)
	{
		if (!worldObj.isRemote)
		{
			if (!isInteracting.getBoolean())
			{
				MCA.getPacketHandler().sendPacketToPlayer(new PacketOpenGUIOnEntity(this.getEntityId()), (EntityPlayerMP) player);
				isInteracting.setValue(true);
			}

			else
			{
				player.addChatMessage(new ChatComponentText("That villager is being interacted with."));
			}
		}

		return true;
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt) 
	{
		super.writeEntityToNBT(nbt);
		aiManager.writeToNBT(nbt);

		nbt.setString("name", name.getString());
		nbt.setString("skin", skin.getString());
		nbt.setInteger("professionId", professionId.getInt());
		nbt.setInteger("personalityId", personalityId.getInt());
		nbt.setInteger("permanentId", permanentId.getInt());
		nbt.setBoolean("isMale", isMale.getBoolean());
		nbt.setBoolean("isEngaged", isEngaged.getBoolean());
		nbt.setInteger("spouseId", spouseId.getInt());
		nbt.setString("spouseName", spouseName.getString());
		nbt.setInteger("babyState", babyState.getInt());
		nbt.setInteger("movementState", movementState.getInt());
		nbt.setBoolean("isChild", isChild.getBoolean());
		nbt.setInteger("age", age.getInt());
		nbt.setString("parentNames", parentNames.getString());
		nbt.setString("parentIDs", parentIDs.getString());
		nbt.setBoolean("isInteracting", isInteracting.getBoolean());
		nbt.setFloat("scaleHeight", scaleHeight.getFloat());
		nbt.setFloat("scaleGirth", scaleGirth.getFloat());

		nbt.setInteger("ticksAlive", ticksAlive);

		PlayerMemoryHandler.writePlayerMemoryToNBT(playerMemories, nbt);
		dataWatcherEx.writeDataWatcherToNBT(nbt);

		nbt.setTag("inventory", inventory.saveInventoryToNBT());
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt)
	{
		super.readEntityFromNBT(nbt);
		aiManager.readFromNBT(nbt);

		name.setValue(nbt.getString("name"));
		skin.setValue(nbt.getString("skin"));
		professionId.setValue(nbt.getInteger("professionId"));
		personalityId.setValue(nbt.getInteger("personalityId"));
		permanentId.setValue(nbt.getInteger("permanentId"));
		isMale.setValue(nbt.getBoolean("isMale"));
		isEngaged.setValue(nbt.getBoolean("isEngaged"));
		spouseId.setValue(nbt.getInteger("spouseId"));
		spouseName.setValue(nbt.getString("spouseName"));
		babyState.setValue(nbt.getInteger("babyState"));
		movementState.setValue(nbt.getInteger("movementState"));
		isChild.setValue(nbt.getBoolean("isChild"));
		age.setValue(nbt.getInteger("age"));
		parentNames.setValue(nbt.getString("parentNames"));
		parentIDs.setValue(nbt.getString("parentIDs"));
		isInteracting.setValue(nbt.getBoolean("isInteracting"));
		scaleHeight.setValue(nbt.getFloat("scaleHeight"));
		scaleGirth.setValue(nbt.getFloat("scaleGirth"));

		ticksAlive = nbt.getInteger("ticksAlive");

		PlayerMemoryHandler.readPlayerMemoryFromNBT(this, playerMemories, nbt);
		dataWatcherEx.readDataWatcherFromNBT(nbt);
		doDisplay.setValue(true);
		addAI();

		final NBTTagList tagList = nbt.getTagList("inventory", 10);
		inventory.loadInventoryFromNBT(tagList);
	}

	@Override
	public void verifySellingItem(ItemStack itemStack)
	{
		//Disables trading villager sounds.
	}

	@Override
	public void swingItem()
	{
		if (!isSwinging.getBoolean() || swingProgressTicks >= 8 / 2 || swingProgressTicks < 0)
		{
			swingProgressTicks = -1;
			isSwinging.setValue(true);
		}
	}

	@Override
	protected String getLivingSound()
	{
		return null;
	}

	@Override
	protected String getHurtSound() 
	{
		return null;
	}

	@Override
	protected String getDeathSound() 
	{
		return null;
	}

	@Override
	public void onDeath(DamageSource damageSource) 
	{
		super.onDeath(damageSource);

		if (!worldObj.isRemote)
		{
			EntityPlayer killingPlayer = damageSource.getSourceOfDamage() instanceof EntityPlayer ? (EntityPlayer)damageSource.getSourceOfDamage() : null;
			String source = killingPlayer != null ? killingPlayer.getName() : damageSource.getDamageType();

			MCA.getLog().info("Villager '" + name.getString() + "(" + getProfessionEnum().toString() + ")' was killed by " + source + ". ");

			aiManager.disableAllToggleAIs();
			getAI(AISleep.class).transitionSkinState(true);

			if (isMarriedToAPlayer())
			{
				EntityPlayer playerPartner = getPlayerSpouse();

				if (playerPartner != null)
				{
					PlayerData data = MCA.getPlayerData(playerPartner);
					playerPartner.addChatMessage(new ChatComponentText(Color.RED + name.getString() + " has died."));
					data.setNotMarried();
				}

				else
				{
					for (PlayerMemory memory : playerMemories.values())
					{
						if (memory.getPermanentId() == this.spouseId.getInt())
						{
							PlayerData data = MCA.getPlayerData(memory.getUUID());
							data.setNotMarried();
							break;
						}
					}
				}
			}

			else if (isMarriedToAVillager())
			{
				EntityHuman partner = getVillagerSpouse();

				if (partner != null)
				{
					partner.setIsMarried(false, (EntityHuman)null);
				}
			}

			for (Entity entity : RadixLogic.getAllEntitiesOfTypeWithinDistance(EntityHuman.class, this, 20))
			{
				if (entity instanceof EntityHuman)
				{
					EntityHuman human = (EntityHuman)entity;
					human.getAI(AIMood.class).modifyMoodLevel(-2.0F);
				}
			}
		}
	}

	private boolean isMarriedToAVillager() 
	{
		return spouseId.getInt() > 0;
	}

	@Override
	protected void updateAITasks()
	{
		AISleep sleepAI = getAI(AISleep.class);
		EnumMovementState moveState = EnumMovementState.fromId(movementState.getInt());
		boolean isSleeping = sleepAI.getIsSleeping();

		if (!isSleeping && (moveState == EnumMovementState.MOVE || moveState == EnumMovementState.FOLLOW))
		{
			super.updateAITasks();
		}

		if (moveState == EnumMovementState.STAY && !isSleeping)
		{
			tasks.onUpdateTasks();
			getLookHelper().onUpdateLook();
		}

		if (moveState == EnumMovementState.STAY || isSleeping || isInteracting.getBoolean())
		{
			getNavigator().clearPathEntity();
		}
	}

	@Override
	protected void damageEntity(DamageSource damageSource, float damageAmount)
	{
		super.damageEntity(damageSource, damageAmount);

		final AIRespondToAttack aiRespondToAttack = getAI(AIRespondToAttack.class);

		if (!aiRespondToAttack.getIsRetaliating())
		{
			aiRespondToAttack.startResponse(damageSource.getEntity());
		}

		//Reset sleeping if appropriate.
		AISleep aiSleep = aiManager.getAI(AISleep.class);

		if (aiSleep.getIsSleeping())
		{
			aiSleep.setIsSleeping(false);
			aiSleep.setSleepingState(EnumSleepingState.INTERRUPTED);
		}
	}

	public void sayRaw(String text, EntityPlayer target)
	{
		final StringBuilder sb = new StringBuilder();

		if (MCA.getConfig().villagerChatPrefix != null && !MCA.getConfig().villagerChatPrefix.equals("null"))
		{
			sb.append(MCA.getConfig().villagerChatPrefix);
		}

		sb.append(getTitle(target));
		sb.append(": ");
		sb.append(text);

		if (target != null)
		{
			target.addChatMessage(new ChatComponentText(sb.toString()));
		}

		aiManager.getAI(AIIdle.class).reset();		

	}

	public void say(String phraseId, EntityPlayer target, Object... arguments)
	{
		final StringBuilder sb = new StringBuilder();

		//Handle chat prefix.
		if (MCA.getConfig().villagerChatPrefix != null && !MCA.getConfig().villagerChatPrefix.equals("null"))
		{
			sb.append(MCA.getConfig().villagerChatPrefix);
		}

		//Add title and text.
		sb.append(getTitle(target));
		sb.append(": ");
		sb.append(MCA.getLanguageManager().getString(phraseId, arguments));

		//Just in case the target is no longer present, somehow.
		if (target != null)
		{
			target.addChatMessage(new ChatComponentText(sb.toString()));
		}

		aiManager.getAI(AIIdle.class).reset();
		aiManager.getAI(AISleep.class).setIsSleeping(false);
		aiManager.getAI(AISleep.class).setSleepingState(EnumSleepingState.INTERRUPTED);
	}

	public void say(String phraseId, EntityPlayer target)
	{
		say(phraseId, target, this, target);
	}

	/**
	 * @return	The appropriate title for this entity given the player who is requesting it.
	 */
	public String getTitle(EntityPlayer player)
	{
		return MCA.getLanguageManager().getString(isMale.getBoolean() ? "title.nonrelative.male" : "title.nonrelative.female", this);
	}

	public boolean isInOverworld()
	{
		return worldObj.provider.getDimensionId() == 0;
	}

	public EnumProfession getProfessionEnum()
	{
		return EnumProfession.getProfessionById(professionId.getInt());
	}

	public EnumProfessionGroup getProfessionGroup()
	{
		return EnumProfession.getProfessionById(professionId.getInt()).getSkinGroup();
	}

	public <T extends AbstractAI> T getAI(Class<T> clazz)
	{
		return this.aiManager.getAI(clazz);
	}

	public boolean getIsMale()
	{
		return isMale.getBoolean();
	}

	public void setSkin(String value)
	{
		this.skin.setValue(value);
	}

	public String getSkin()
	{
		return skin.getString();
	}

	public boolean getIsChild()
	{
		return isChild.getBoolean();
	}

	public EnumBabyState getBabyState()
	{
		return EnumBabyState.fromId(babyState.getInt());
	}

	public int getTicksAlive()
	{
		return ticksAlive;
	}

	public void setTicksAlive(int value)
	{
		this.ticksAlive = value;
	}

	public void setPlayerMemory(EntityPlayer player, PlayerMemory memory)
	{
		playerMemories.put(player.getName(), memory);
	}

	public PlayerMemory getPlayerMemory(EntityPlayer player)
	{
		String playerName = player.getName();
		PlayerMemory returnMemory = playerMemories.get(playerName);

		if (returnMemory == null)
		{
			returnMemory = new PlayerMemory(this, player);
			playerMemories.put(playerName, returnMemory);
		}

		return returnMemory;
	}

	public boolean hasMemoryOfPlayer(EntityPlayer player)
	{
		return playerMemories.containsKey(player.getName());
	}

	public void setIsMarried(boolean value, EntityHuman partner) 
	{
		if (value)
		{
			spouseId.setValue(partner.permanentId.getInt());
			spouseName.setValue(partner.name.getString());

			AIProgressStory storyAI = getAI(AIProgressStory.class);
			AIProgressStory partnerAI = partner.getAI(AIProgressStory.class);

			storyAI.setProgressionStep(EnumProgressionStep.TRY_FOR_BABY);
			partnerAI.setProgressionStep(EnumProgressionStep.TRY_FOR_BABY);

			if (getIsMale())
			{
				storyAI.setDominant(true);
				partnerAI.setDominant(false);
			}

			else if (partner.getIsMale())
			{
				partnerAI.setDominant(true);
				storyAI.setDominant(false);
			}
		}

		else
		{
			spouseId.setValue(0);
			spouseName.setValue("none");
			isEngaged.setValue(false);

			getAI(AIProgressStory.class).reset();
		}
	}

	public void setIsMarried(boolean value, EntityPlayer partner) 
	{
		if (value)
		{
			if (isEngaged.getBoolean())
			{
				for (Entity entity : RadixLogic.getAllEntitiesOfTypeWithinDistance(EntityHuman.class, this, 50))
				{
					if (entity != this)
					{
						EntityHuman human = (EntityHuman)entity;
						PlayerMemory memory = human.getPlayerMemory(partner);
						memory.setHasGift(true);
					}
				}
			}

			PlayerData data = MCA.getPlayerData(partner);
			spouseId.setValue(data.permanentId.getInt());
			spouseName.setValue(partner.getName());
			isEngaged.setValue(false);

			getAI(AIProgressStory.class).setProgressionStep(EnumProgressionStep.FINISHED);
		}

		else
		{
			setIsMarried(value, (EntityHuman)null);
			getAI(AIProgressStory.class).setProgressionStep(EnumProgressionStep.SEARCH_FOR_PARTNER);
		}
	}	

	public boolean getIsMarried()
	{
		return spouseId.getInt() != 0 && (!getIsEngaged());
	}

	public boolean getIsEngaged() 
	{
		return isEngaged.getBoolean();
	}

	public void setIsEngaged(boolean value, EntityPlayer partner)
	{
		isEngaged.setValue(value);

		if (value)
		{
			PlayerData data = MCA.getPlayerData(partner);
			spouseId.setValue(data.permanentId.getInt());
			spouseName.setValue(partner.getName());
		}

		else
		{
			spouseId.setValue(0);
			spouseName.setValue("none");
		}
	}

	public int getPermanentId()
	{
		return permanentId.getInt();
	}

	@Override
	public void setPermanentId(int value)
	{
		permanentId.setValue(value);
	}

	public int getAge()
	{
		return this.age.getInt();
	}

	public void setAge(int value)
	{
		this.age.setValue(value);
	}

	public int getSpouseId() 
	{
		return spouseId.getInt();
	}

	public boolean isMarriedToAPlayer()
	{
		return spouseId.getInt() < 0;
	}

	public EntityHuman getVillagerSpouse()
	{
		for (Object obj : worldObj.loadedEntityList)
		{
			if (obj instanceof EntityHuman)
			{
				EntityHuman human = (EntityHuman)obj;

				if (human.getPermanentId() == this.getSpouseId())
				{
					return human;
				}
			}
		}

		return null;
	}

	public EntityPlayer getPlayerSpouse()
	{
		for (PlayerMemory memory : playerMemories.values())
		{
			if (memory.getPermanentId() == this.spouseId.getInt())
			{
				return worldObj.getPlayerEntityByName(memory.getPlayerName());
			}
		}

		return null;
	}

	public String getSpouseName()
	{
		return spouseName.getString();
	}

	public EnumPersonality getPersonality()
	{
		return EnumPersonality.getById(personalityId.getInt());
	}

	@Override
	public ItemStack getHeldItem()
	{
		if (babyState.getInt() > 0)
		{
			switch (babyState.getInt())
			{
			case 1: return new ItemStack(ModItems.babyBoy);
			case 2: return new ItemStack(ModItems.babyGirl);
			}
		}

		else if (getProfessionEnum() == EnumProfession.Guard)
		{
			return new ItemStack(Items.iron_sword);
		}
		
		else if (getProfessionEnum() == EnumProfession.Archer)
		{
			return new ItemStack(Items.bow);
		}

		else if (heldItem.getInt() != -1 && aiManager.isToggleAIActive())
		{
			return new ItemStack(Item.getItemById(heldItem.getInt()));
		}

		else if (inventory.contains(ModItems.babyBoy) || inventory.contains(ModItems.babyGirl))
		{
			int slot = inventory.getFirstSlotContainingItem(ModItems.babyBoy);
			slot = slot == -1 ? inventory.getFirstSlotContainingItem(ModItems.babyGirl) : slot;

			if (slot != -1)
			{
				return inventory.getStackInSlot(slot);
			}
		}

		return null;
	}

	public void setHeldItem(Item item)
	{
		if (item != null)
		{
			heldItem.setValue(Item.getIdFromItem(item));
		}

		else
		{
			heldItem.setValue(-1);
		}
	}

	public boolean damageHeldItem(int amount)
	{
		try
		{
			ItemStack heldItem = getHeldItem();

			if (heldItem != null)
			{
				Item item = heldItem.getItem();
				int slot = inventory.getFirstSlotContainingItem(item);

				ItemStack itemInSlot = inventory.getStackInSlot(slot);
				itemInSlot.damageItem(amount, this);

				if (itemInSlot.stackSize == 0)
				{
					aiManager.disableAllToggleAIs();
					inventory.setInventorySlotContents(slot, null);
					return true;
				}

				else
				{
					inventory.setInventorySlotContents(slot, itemInSlot);
					return false;
				}
			}
		}

		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}

	public void setBabyState(EnumBabyState state) 
	{
		babyState.setValue(state.getId());
	}

	public void setIsChild(boolean value) 
	{
		this.isChild.setValue(value);
	}

	public String getName() 
	{
		return name.getString();
	}

	public String getParentNames() 
	{
		return parentNames.getString();
	}

	@Override
	public DataWatcherEx getDataWatcherEx() 
	{
		return dataWatcherEx;
	}

	public void halt()
	{
		getNavigator().clearPathEntity();

		moveForward = 0.0F;
		moveStrafing = 0.0F;
		motionX = 0.0D;
		motionY = 0.0D;
		motionZ = 0.0D;
	}

	@Override
	public boolean canBePushed()
	{
		final AISleep sleepAI = aiManager.getAI(AISleep.class);		
		return !sleepAI.getIsSleeping();
	}

	public float getHeight() 
	{
		return scaleHeight.getFloat();
	}

	public float getGirth() 
	{
		return scaleGirth.getFloat();
	}

	@Override
	public void useRecipe(MerchantRecipe merchantRecipe)
	{
		//Representation of EntityVillager's useRecipe without playing sounds.
		merchantRecipe.incrementToolUses();
		livingSoundTime = -getTalkInterval();

		final MerchantRecipeList buyingList = getBuyingList();

		for (Object obj : buyingList)
		{
			MerchantRecipe recipe = (MerchantRecipe)obj;
		}

		if (merchantRecipe.func_180321_e() == 1 || this.rand.nextInt(5) == 0)
		{
			ObfuscationReflectionHelper.setPrivateValue(EntityVillager.class, this, Integer.valueOf(40), 6);
			ObfuscationReflectionHelper.setPrivateValue(EntityVillager.class, this, true, 7);
			final EntityPlayer buyingPlayer = ObfuscationReflectionHelper.getPrivateValue(EntityVillager.class, this, 4);

			if (buyingPlayer == null)
			{
				ObfuscationReflectionHelper.setPrivateValue(EntityVillager.class, this, null, 9);
			}

			else
			{
				ObfuscationReflectionHelper.setPrivateValue(EntityVillager.class, this, buyingPlayer.getName(), 9);
			}
		}

		if (merchantRecipe.getItemToBuy().getItem() == Items.emerald)
		{
			final int wealth = (Integer)ObfuscationReflectionHelper.getPrivateValue(EntityVillager.class, this, 8);
			ObfuscationReflectionHelper.setPrivateValue(EntityVillager.class, this, Integer.valueOf(wealth + merchantRecipe.getItemToBuy().stackSize), 8);
		}
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) 
	{
		ByteBufIO.writeObject(buffer, playerMemories);
	}

	@Override
	public void readSpawnData(ByteBuf additionalData) 
	{
		Map<String, PlayerMemory> recvMemories = (Map<String, PlayerMemory>) ByteBufIO.readObject(additionalData);
		this.playerMemories = recvMemories;
	}

	public void setIsInteracting(boolean value)
	{
		this.isInteracting.setValue(value);
	}

	public void setSizeOverride(float width, float height)
	{
		this.setSize(width, height);
	}

	public String getFatherName() 
	{
		try
		{
			return parentNames.getString().substring(0, parentNames.getString().indexOf("|"));
		}

		catch (Exception e)
		{
			return "?";
		}
	}

	public String getMotherName()
	{
		try
		{
			return parentNames.getString().substring(parentNames.getString().indexOf("|") + 1, parentNames.getString().length());
		}

		catch (Exception e)
		{
			return "?";
		}
	}

	public int getFatherId() 
	{
		try
		{
			return Integer.parseInt(parentIDs.getString().substring(0, parentIDs.getString().indexOf("|")));
		}

		catch (Exception e)
		{
			return -1;
		}
	}

	public void setMovementState(EnumMovementState state)
	{
		movementState.setValue(state.getId());
	}

	public EnumMovementState getMovementState()
	{
		return EnumMovementState.fromId(movementState.getInt());
	}

	public int getMotherId()
	{
		try
		{
			return Integer.parseInt(parentIDs.getString().substring(parentIDs.getString().indexOf("|") + 1, parentIDs.getString().length()));
		}

		catch (Exception e)
		{
			return -1;
		}
	}

	public boolean getDoDisplay()
	{
		return doDisplay.getBoolean();
	}

	public void setName(String string) 
	{
		this.name.setValue(string);
	}

	public boolean allowIntimateInteractions(EntityPlayer player)
	{
		return !getIsChild() && !isPlayerAParent(player);
	}

	public boolean isPlayerAParent(EntityPlayer player)
	{
		final PlayerData data = MCA.getPlayerData(player);	
		return getMotherId() == data.permanentId.getInt() || getFatherId() == data.permanentId.getInt();
	}

	public boolean allowControllingInteractions(EntityPlayer player)
	{
		final PlayerData data = MCA.getPlayerData(player);

		if (data.isSuperUser.getBoolean())
		{
			return true;
		}

		else if (isMarriedToAPlayer() && getSpouseId() != data.permanentId.getInt())
		{
			return false;
		}

		else if (getIsChild())
		{
			if (isPlayerAParent(player))
			{
				return true;
			}

			else if (isChildOfAVillager())
			{
				return true;
			}

			else
			{
				return false;
			}
		}

		return true;
	}

	public boolean allowWorkInteractions(EntityPlayer player)
	{
		final PlayerData data = MCA.getPlayerData(player);
		final PlayerMemory memory = getPlayerMemory(player);

		if (data.isSuperUser.getBoolean())
		{
			return true;
		}

		else if (memory.getIsHiredBy())
		{
			return true;
		}

		else if (isPlayerAParent(player))
		{
			return true;
		}

		return false;
	}

	public void openInventory(EntityPlayer player)
	{
		player.displayGUIChest(inventory);
	}

	public boolean isChildOfAVillager() 
	{
		return getMotherId() >= 0 && getFatherId() >= 0;
	}

	public Inventory getEntityInventory() 
	{
		return inventory;
	}

	public AIManager getAIManager() 
	{
		return aiManager;
	}

	public float getSpeed()
	{
		return getPersonality() == EnumPersonality.ATHLETIC ? Constants.SPEED_RUN : Constants.SPEED_WALK;
	}

	public boolean getCanBeHired(EntityPlayer player) 
	{
		return getPlayerSpouse() != player && (getProfessionGroup() == EnumProfessionGroup.Farmer || 
				getProfessionGroup() == EnumProfessionGroup.Miner || 
				getProfessionGroup() == EnumProfessionGroup.Guard);
	}

	public void setIsMale(boolean value) 
	{
		this.isMale.setValue(value);
		this.setSkin(this.getRandomSkin());
	}

	public void setHeight(float f) 
	{
		this.scaleHeight.setValue(f);
	}

	public void setGirth(float f) 
	{
		this.scaleGirth.setValue(f);
	}

	public void setProfessionId(int profession)
	{
		this.professionId.setValue(profession);
	}

	public void setPersonality(int personalityId) 
	{
		this.personalityId.setValue(personalityId);
	}

	@Override
	public int getProfession()
	{
		return getProfessionGroup().getVanillaProfessionId();
	}

	private MerchantRecipeList getBuyingList()
	{
		return (MerchantRecipeList) ObfuscationReflectionHelper.getPrivateValue(EntityVillager.class, this, 5);
	}
}
