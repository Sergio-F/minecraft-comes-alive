package mca.core.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import mca.core.MCA;
import mca.entity.AbstractEntity;

import com.google.common.io.Files;
import com.radixshock.radixcore.core.RadixCore;

import cpw.mods.fml.relauncher.Side;

public final class ActorScriptHandler 
{
	private static final String[] KEYWORDS = {"SAY", "INIT", "WAITFOR", "RESTART"};

	private transient AbstractEntity owner;
	private transient List<String> scriptLines;
	private boolean waitFlag;

	public ActorScriptHandler()
	{
		//For reading from NBT
	}
	
	public ActorScriptHandler(AbstractEntity entity)
	{
		this.owner = entity;

		try
		{
			this.scriptLines = Files.readLines(getScriptFile(entity.currentActorScript), Charset.forName("UTF-8"));
		}

		catch (Exception e)
		{
			//When loading.
		}
	}

	public static void initialize()
	{
		File scriptsFolder = new File(RadixCore.getInstance().runningDirectory + "/config/MCA/Scripts/");

		if (!scriptsFolder.exists())
		{
			scriptsFolder.mkdirs();
		}
	}

	public static File getScriptFile(String scriptName)
	{
		String location = RadixCore.getInstance().runningDirectory + "/config/MCA/Scripts/" + scriptName + ".txt";
		return new File(location);
	}

	public static boolean isScriptValid(String scriptName)
	{
		if (doesScriptExist(scriptName))
		{
			File scriptFile = getScriptFile(scriptName);

			try
			{
				List<String> lines = Files.readLines(scriptFile, Charset.forName("UTF-8"));
				int lineNumber = 0;

				for (String line : lines)
				{
					String lineToUpper = line.toUpperCase();

					lineNumber++;
				}

				return true;
			}

			catch (IOException e)
			{
				return false;
			}
		}

		return false;
	}

	public static boolean doesScriptExist(String scriptName)
	{
		return getScriptFile(scriptName).exists();
	}

	public static void resetScriptedEntity(AbstractEntity entity)
	{
		entity.isInActorMode = false;
		entity.currentActorScript = "";
		entity.actorActionProgress = 0;
	}

	public void execute() 
	{
		if (scriptLines == null)
		{
			try
			{
				this.scriptLines = Files.readLines(getScriptFile(owner.currentActorScript), Charset.forName("UTF-8"));
			}

			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		try
		{			
			if (!waitFlag)
			{
				String nextLine = scriptLines.get(owner.actorActionProgress + 1);

				if (nextLine.toUpperCase().startsWith("SAY"))
				{
					String sayLine = nextLine.substring(3).trim();
					owner.sayScripted(sayLine);
				}

				else if (nextLine.toUpperCase().startsWith("RESTART"))
				{
					owner.actorActionProgress = 0;
					return;
				}

				else if (nextLine.toUpperCase().startsWith("WAITFOR"))
				{
					waitFlag = true;
				}

				owner.actorActionProgress++;
			}
		}

		catch (IndexOutOfBoundsException e)
		{
			waitFlag = true;
		}
	}

	public void clearWaitFlag()
	{
		waitFlag = false;
	}

	public void writeHandlerToNBT(AbstractEntity abstractEntity, NBTTagCompound nbt) 
	{
		//Loop through each field in this class and write to NBT.
		for (final Field field : this.getClass().getFields())
		{
			try
			{
				if (field.getModifiers() != Modifier.TRANSIENT)
				{
					if (field.getType().toString().contains("int"))
					{
						nbt.setInteger(field.getName(), Integer.parseInt(field.get(owner.huntingChore).toString()));
					}

					else if (field.getType().toString().contains("double"))
					{
						nbt.setDouble(field.getName(), Double.parseDouble(field.get(owner.huntingChore).toString()));
					}

					else if (field.getType().toString().contains("float"))
					{
						nbt.setFloat(field.getName(), Float.parseFloat(field.get(owner.huntingChore).toString()));
					}

					else if (field.getType().toString().contains("String"))
					{
						nbt.setString(field.getName(), field.get(owner.huntingChore).toString());
					}

					else if (field.getType().toString().contains("boolean"))
					{
						nbt.setBoolean(field.getName(), Boolean.parseBoolean(field.get(owner.huntingChore).toString()));
					}
				}
			}

			catch (final IllegalAccessException e)
			{
				MCA.getInstance().getLogger().log(e);
				continue;
			}
		}
	}

	public void readHandlerFromNBT(AbstractEntity abstractEntity, NBTTagCompound nbt) 
	{
		this.owner = abstractEntity;
		
		//Loop through each field in this class and read from NBT.
		for (final Field field : this.getClass().getFields())
		{
			try
			{
				if (field.getModifiers() != Modifier.TRANSIENT)
				{
					if (field.getType().toString().contains("int"))
					{
						field.set(owner.huntingChore, nbt.getInteger(field.getName()));
					}

					else if (field.getType().toString().contains("double"))
					{
						field.set(owner.huntingChore, nbt.getDouble(field.getName()));
					}

					else if (field.getType().toString().contains("float"))
					{
						field.set(owner.huntingChore, nbt.getFloat(field.getName()));
					}

					else if (field.getType().toString().contains("String"))
					{
						field.set(owner.huntingChore, nbt.getString(field.getName()));
					}

					else if (field.getType().toString().contains("boolean"))
					{
						field.set(owner.huntingChore, nbt.getBoolean(field.getName()));
					}
				}
			}

			catch (final IllegalAccessException e)
			{
				MCA.getInstance().getLogger().log(e);
				continue;
			}
		}
	}
}
