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

package com.dmsafe.rsnoverlay;

import java.awt.Color;
import java.util.function.BiConsumer;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.dmsafe.DMSafePlugin;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.FriendsChatRank;

import static com.dmsafe.DMSafePlugin.*;
import static net.runelite.api.FriendsChatRank.UNRANKED;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.client.util.Text;

@Singleton
public
class PlayerNameService
{
    private final Client client;
    private final DMSafePlugin plugin;

    @Inject
    private PlayerNameService(Client client, DMSafePlugin plugin) {
        this.client = client;
        this.plugin = plugin;
    }

    void forEachPlayer(final BiConsumer<Player, Decorations> consumer) {
        for (Player player : client.getPlayers()) {
            if (player == null || player.getName() == null) {
                continue;
            }

            Decorations decorations = getDecorations(player);
            if (decorations != null && decorations.getColor() != null) {
                consumer.accept(player, decorations);
            }
        }
    }
    public Decorations getDecorations(Player player) {
        if (player.getName() == null) {
            return null;
        }

        Color color = plugin.data.getColor(Text.toJagexName(player.getName()));

        FriendsChatRank rank = null;
        ClanTitle clanTitle = null;
        if (player.isFriendsChatMember()) {
            rank = getFriendsChatRank(player);
        }
        if (player.isClanMember()) {
            clanTitle = getClanTitle(player);
        }

        if (color == null && rank == null && clanTitle == null) {
            return null;
        }

        return new Decorations(rank, clanTitle, color);
    }

    @Value
    public static class Decorations {
        FriendsChatRank friendsChatRank;
        ClanTitle clanTitle;
        Color color;
    }

    private ClanTitle getClanTitle(Player player) {
        ClanChannel clanChannel = client.getClanChannel();
        ClanSettings clanSettings = client.getClanSettings();
        if (clanChannel == null || clanSettings == null) {
            return null;
        }

        ClanChannelMember member = clanChannel.findMember(player.getName());
        if (member == null) {
            return null;
        }

        ClanRank rank = member.getRank();
        return clanSettings.titleForRank(rank);
    }

    private FriendsChatRank getFriendsChatRank(Player player) {
        final FriendsChatManager friendsChatManager = client.getFriendsChatManager();
        if (friendsChatManager == null) {
            return UNRANKED;
        }

        FriendsChatMember friendsChatMember = friendsChatManager.findByName(Text.removeTags(player.getName()));
        return friendsChatMember != null ? friendsChatMember.getRank() : UNRANKED;
    }
}