/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.dmsafe;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("DMSafe")
public interface DMSafeConfig extends Config
{
	@ConfigSection(
			name = "Rank Detection",
			description = "Detecting Scammers & Ranks in DMSafe CC",
			position = 0
	)
	String RANK_DETECTION_SECTION = "Rank Detection";

	@ConfigItem(
			keyName = "drawNamesOverhead",
			name = "Draw names overhead",
			description = "Configures whether or not player names will render on their character model",
			section = RANK_DETECTION_SECTION,
			position = 1
	)

	default boolean drawOverheadNames()
	{
		return true;
	}

	@ConfigItem(
			keyName = "drawMinimapNames",
			name = "Draw names on minimap",
			description = "Configures whether or not minimap names for players with rendered names should be drawn",
			section = RANK_DETECTION_SECTION,
			position = 2
	)
	default boolean drawMinimapNames()
	{
		return true;
	}

	@ConfigSection(
			name = "Chatbox",
			description = "Chatbox togglable options",
			position = 3
	)
	String CHATBOX_DETECTION_SECTION = "Chatbox Spam";
	@ConfigItem(
			keyName = "chatboxScammerSpam",
			name = "Chatbox scammer information",
			description = "Write in the chatbox if a scammer is nearby",
			section = CHATBOX_DETECTION_SECTION,
			position = 4
	)
	default boolean showScammerSpam() { return true;}

	@ConfigSection(
			name = "Data Endpoint",
			description = "By default a GitHub data endpoint is used. Tick the option below to use our external data endpoint.",
			position = 5
	)
	String DATA_ENDPOINT_SECTION = "Data Endpoint";
	@ConfigItem(
			keyName = "useExternalDataEndpoint",
			name = "Use External Data Endpoint",
			description = "Use our external data endpoint instead of the default which is hosted on GitHub for faster real time updating. ",
			section = DATA_ENDPOINT_SECTION,
			position = 6
	)
	default boolean useExternalDataEndpoint() { return false;}

	@ConfigSection(
			name = "Side Panel",
			description = "Side Panel Auto Popup",
			position = 7
	)
	String SIDE_PANEL_SECTION = "Side Panel";
	@ConfigItem(
			keyName = "sidePanelPopup",
			name = "Popup automatically",
			description = "Automatically pop open the side panel when you Deathmatch them.",
			section = SIDE_PANEL_SECTION,
			position = 8
	)
	default boolean popupSidePanel() { return true;}

	@ConfigItem(
			keyName = "autoExpandMembers",
			name = "Expand members by default",
			description = "Controls whether party member details are automatically expanded (checked) or collapsed into banners (unchecked)",
			section = SIDE_PANEL_SECTION,
			position = 9
	)
	default boolean autoExpandMembers()
	{
		return false;
	}
}

