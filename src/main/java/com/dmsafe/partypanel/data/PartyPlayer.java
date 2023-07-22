/*
 * Copyright (c) 2020, TheStonedTurtle <https://github.com/TheStonedTurtle>
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
package com.dmsafe.partypanel.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyMember;

import static com.dmsafe.DMSafePlugin.DMER_NAME;

@Data
@EqualsAndHashCode
public class PartyPlayer
{
    private transient PartyMember member;
    private String username;
    private Stats stats;
    private GameItem[] inventory;
    private GameItem[] equipment;
    private Prayers prayers;
    private int poison;
    private int disease;
    private int world;

    private int vengActive;
    private String hardwareID = "";
    private String accountID = "";
    private String rank = "";
    private boolean dataLogged;

    public PartyPlayer(final PartyMember member) {
        this.member = member;
        this.username = "";
        this.stats = null;
        this.inventory = new GameItem[28];
        this.equipment = new GameItem[EquipmentInventorySlot.AMMO.getSlotIdx() + 1];
        this.prayers = null;
        this.poison = 0;
        this.disease = 0;
        this.world = 0;
        this.rank = DMER_NAME;
        this.vengActive = 0;
        this.hardwareID = "n/a";
        this.accountID = "n/a";
        this.dataLogged = false;
    }

    public PartyPlayer(final PartyMember member, final Client client, final ItemManager itemManager) {
        this(member);
        this.poison = client.getVarpValue(VarPlayer.POISON);
        this.disease = client.getVarpValue(VarPlayer.DISEASE_VALUE);
        this.world = client.getWorld();
        this.vengActive = client.getVarbitValue(Varbits.VENGEANCE_ACTIVE);
        updatePlayerInfo(client, itemManager);
    }

    public void updatePlayerInfo(final Client client, final ItemManager itemManager) {
        // Player is logged in
        if (client.getLocalPlayer() != null) {
            username = client.getLocalPlayer().getName();
            stats = new Stats(client);

            final ItemContainer invi = client.getItemContainer(InventoryID.INVENTORY);
            if (invi != null) {
                inventory = GameItem.convertItemsToGameItems(invi.getItems(), itemManager);
            }

            final ItemContainer equip = client.getItemContainer(InventoryID.EQUIPMENT);
            if (equip != null) {
                equipment = GameItem.convertItemsToGameItems(equip.getItems(), itemManager);
            }

            if (prayers == null) {
                prayers = new Prayers(client);
            }
        }
    }

    public int getSkillBoostedLevel(final Skill skill) {
        if (stats == null) {
            return 0;
        }

        return stats.getBoostedLevels().get(skill);
    }

    public int getSkillRealLevel(final Skill skill) {
        if (stats == null) {
            return 0;
        }

        assert skill != Skill.OVERALL;

        return Math.min(stats.getBaseLevels().get(skill), 99);
    }

    public void setSkillsBoostedLevel(final Skill skill, final int level) {
        if (stats == null) {
            return;
        }

        stats.getBoostedLevels().put(skill, level);
    }

    public void setSkillsRealLevel(final Skill skill, final int level) {
        if (stats == null) {
            return;
        }

        stats.getBaseLevels().put(skill, level);
    }
}