/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
 * Copyright (c) 2019, Jordan Atwood <nightfirecat@protonmail.com>
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

import com.dmsafe.DMSafeConfig;
import com.dmsafe.DMSafePlugin;

import com.dmsafe.web.Deathmatcher;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

import javax.inject.Inject;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.util.Objects;

import static com.dmsafe.DMSafePlugin.DMER_NAME;

public class PlayerNameOverlay extends Overlay {
    private final PlayerRankImage playerRankImage;
    private final DMSafePlugin plugin;
    private final PlayerNameService playerIndicatorsService;
    private final DMSafeConfig config;
    private static final int PLAYER_OVERHEAD_TEXT_MARGIN = 40;

    @Inject
    private PlayerNameOverlay(DMSafePlugin plugin, DMSafeConfig config, PlayerNameService playerIndicatorsService, ChatIconManager chatIconManager) {
        this.plugin = plugin;
        this.config = config;
        this.playerIndicatorsService = playerIndicatorsService;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.MED);
        playerRankImage = new PlayerRankImage(chatIconManager);
    }
    @Override
    public Dimension render(Graphics2D graphics) {
        // Draw names above head
        playerIndicatorsService.forEachPlayer((player, decorations) -> renderPlayerOverlay(graphics, player, decorations));

        return null;
    }

    private void renderPlayerOverlay(Graphics2D graphics, Player player, PlayerNameService.Decorations decorations) {
        if (player.getName() != null) {
            String playerName = Text.toJagexName(player.getName());
            Deathmatcher dmer = plugin.getLocalDeathmatchers().getOrDefault(Text.toJagexName(player.getName()), new Deathmatcher("n/a", "n/a", playerName, DMER_NAME, DMER_NAME));
            String localRSNsRank =dmer.getRank();
            boolean showAboveHead = localRSNsRank != null && plugin.getData().showRankAboveHead(localRSNsRank);
            if (config.drawOverheadNames() && (plugin.getData().showAboveHead(player.getName()) || showAboveHead)) {
                final int zOffset = player.getLogicalHeight() + PLAYER_OVERHEAD_TEXT_MARGIN;
                final String name = Text.sanitize(Objects.requireNonNull(player.getName()));
                Point textLocation = player.getCanvasTextLocation(graphics, name, zOffset);

                if (textLocation == null) {
                    return;
                }

                BufferedImage rankImage = playerRankImage.getRankImage(plugin, player.getName());

                if (rankImage != null) {
                    final int imageWidth = rankImage.getWidth();
                    final int imageTextMargin = imageWidth / 2;
                    final int imageNegativeMargin = imageWidth / 2;

                    final int textHeight = graphics.getFontMetrics().getHeight() - graphics.getFontMetrics().getMaxDescent();
                    final Point imageLocation = new Point(textLocation.getX() - imageNegativeMargin - 1, textLocation.getY() - textHeight / 2 - rankImage.getHeight() / 2);
                    OverlayUtil.renderImageLocation(graphics, imageLocation, rankImage);

                    // move text
                    textLocation = new Point(textLocation.getX() + imageTextMargin, textLocation.getY());
                }
                OverlayUtil.renderTextLocation(graphics, textLocation, name, decorations.getColor());
            }
        }
    }
}
