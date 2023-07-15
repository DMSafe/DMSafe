package com.example;

import com.dmsafe.DMSafePlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DMSafePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DMSafePlugin.class);
		RuneLite.main(args);
	}
}