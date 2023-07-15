package com.dmsafe;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("DMSafe")
public interface DMSafeConfig extends Config
{
	@ConfigItem(
		keyName = "DMSafe",
		name = "Welcome DMSafe",
		description = "DMSafe The message to show to the user when they login"
	)
	default String greeting()
	{
		return "Hello";
	}
}
