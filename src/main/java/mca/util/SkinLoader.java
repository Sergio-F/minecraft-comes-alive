/*******************************************************************************
 * SkinLoader.java
 * Copyright (c) 2014 WildBamaBoy.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MCA Minecraft Mod license.
 ******************************************************************************/

package mca.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import mca.enums.EnumProfessionGroup;
import radixcore.core.RadixCore;
import radixcore.util.RadixExcept;

/**
 * Handles loading of MCA's skins skins.
 */
public final class SkinLoader
{
	public static void loadSkins()
	{
		try
		{
			final File modFile = findModDataFile();

			if (modFile.isFile())
			{
				loadSkinsFromFile(modFile);
			}

			else
			{
				RadixExcept.logFatalCatch(new FileNotFoundException("Unable to locate MCA assets!"), "Unable to find MCA JAR file in mods folder. If using a third party launcher, check that it is set up properly. Ensure that MCA is a .JAR file, not a .ZIP file or folder.");
			}
		}

		catch (final IOException e)
		{
			RadixExcept.logFatalCatch(e, "Unexpected exception while loading skins.");
		}
		
		catch (final NullPointerException e)
		{
			RadixExcept.logFatalCatch(e, "Unexpected exception while loading skins.");
		}
	}

	private static File findModDataFile() throws ZipException, IOException
	{
		File modData = findModAsArchive();

		if (modData == null)
		{
				RadixExcept.logFatalCatch(new FileNotFoundException("Unable to locate MCA assets! This may be due to an issue with your launcher (if made by a third party), or your MCA installation. Try reinstalling the mod, or try a different launcher."), 
						"Unable to find file containing MCA assets.");
		}

		return modData;
	}

	private static File findModAsArchive() throws ZipException, IOException
	{
		final File modsFolder = new File(RadixCore.getRunningDirectory() + "/mods");

		for (final File fileInMods : modsFolder.listFiles())
		{
			if (fileInMods.isFile() && fileInMods.getName().contains(".zip") || fileInMods.getName().contains(".jar"))
			{
				if (fileContainsModData(fileInMods))
				{
					return fileInMods;
				}
			}

			else if (fileInMods.isDirectory())
			{
				final File modData = getModFileFromNestedFolder(fileInMods);

				if (modData != null)
				{
					return modData;
				}
			}
		}

		return null;
	}

	private static void loadSkinsFromFile(File modDataFile) throws ZipException, IOException
	{
		final ZipFile modArchive = new ZipFile(modDataFile);
		final Enumeration enumerator = modArchive.entries();

		while (enumerator.hasMoreElements())
		{
			//Loop through each entry within the JAR until the MCA folder is hit.
			final ZipEntry file = (ZipEntry) enumerator.nextElement();
			String archiveFilePath = "/" + file.getName();

			if (archiveFilePath.contains("textures/skins") && !archiveFilePath.contains("/sleeping/"))
			{
				for (EnumProfessionGroup skinGroup : EnumProfessionGroup.values())
				{
					if (file.getName().contains(skinGroup.toString()))
					{
						skinGroup.addSkin(archiveFilePath);
					}
				}
			}
		}

		modArchive.close();
	}

	private static File getModFileFromNestedFolder(File nestedFolder) throws IOException
	{
		final File[] nestedFiles = nestedFolder.listFiles();

		for (final File file : nestedFiles)
		{
			if (file.isDirectory())
			{
				getModFileFromNestedFolder(file);
			}

			else
			{
				if (fileContainsModData(file))
				{
					return file;
				}
			}
		}

		return null;
	}

	private static boolean fileContainsModData(File fileToTest) throws IOException
	{
		if (fileToTest.getName().contains(".zip") || fileToTest.getName().contains(".jar"))
		{
			try
			{
				final ZipFile archive = new ZipFile(fileToTest);
				final Enumeration enumerator = archive.entries();
				ZipEntry entry;

				while (enumerator.hasMoreElements())
				{
					entry = (ZipEntry) enumerator.nextElement();

					//Test for random files unique to MCA.
					if (entry.getName().contains("mca/core/MCA.class") || entry.getName().contains("sleeping/EE1.png"))
					{
						archive.close();
						return true;
					}
				}

				archive.close();
			}

			catch (final ZipException e)
			{
				e.printStackTrace();
			}
		}

		return false;
	}
}
