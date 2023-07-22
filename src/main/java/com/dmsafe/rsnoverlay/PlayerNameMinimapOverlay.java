package com.dmsafe.rsnoverlay;

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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.dmsafe.DMSafeConfig;
import com.dmsafe.DMSafePlugin;
import com.dmsafe.web.Deathmatcher;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import static com.dmsafe.DMSafePlugin.DMER_NAME;

@Singleton
public class PlayerNameMinimapOverlay extends Overlay {
    private final DMSafePlugin plugin;
    private final DMSafeConfig config;
    private final PlayerNameService playerNameService;
    private final PlayerRankImage playerRankImage;

    @Inject
    private PlayerNameMinimapOverlay(DMSafePlugin plugin, DMSafeConfig config, PlayerNameService playerNameService, ChatIconManager chatIconManager) {
        this.plugin = plugin;
        this.config = config;
        this.playerNameService = playerNameService;
        playerRankImage = new PlayerRankImage(chatIconManager);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        playerNameService.forEachPlayer((player, decorations) -> renderPlayerOverlay(graphics, player, decorations));
        return null;
    }

    private boolean loadedLocally(String name) {
        if (plugin.getLocalDeathmatchers().containsKey(name)) {
            String rankName = plugin.getLocalDeathmatchers().get(name).getRank();
            return plugin.data.showRankMinimap(rankName);
        }
        return false;
    }

    private void renderPlayerOverlay(Graphics2D graphics, Player player, PlayerNameService.Decorations decorations) {
        final String name = Objects.requireNonNull(player.getName()).replace('\u00A0', ' ');

        if (config.drawMinimapNames() && (plugin.data.isTrustedRanked(name) || loadedLocally(name))) {
            BufferedImage rankImage = playerRankImage.getRankImage(plugin, player.getName());
            final Point minimapLocation = player.getMinimapLocation();
            final Point imageMinimapLocation = new Point(minimapLocation.getX() - 10, minimapLocation.getY() - 10);

            if (rankImage != null) {
                OverlayUtil.renderTextLocation(graphics, minimapLocation, name, decorations.getColor());
                OverlayUtil.renderImageLocation(graphics, imageMinimapLocation, rankImage);
            }
        }
    }
}