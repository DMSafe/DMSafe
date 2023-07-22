package com.dmsafe.rsnoverlay;

import com.dmsafe.DMSafePlugin;
import com.dmsafe.web.Deathmatcher;
import net.runelite.api.FriendsChatRank;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

import static com.dmsafe.DMSafePlugin.*;

public class PlayerRankImage {

    private final ChatIconManager chatIconManager;

    private static final BufferedImage SCAMMER_ICON = ImageUtil.loadImageResource(DMSafePlugin.class, "scammer.png");
    private static final BufferedImage DMER_ICON = ImageUtil.loadImageResource(DMSafePlugin.class, "deathmatcher.png");

    public PlayerRankImage(ChatIconManager chatIconManager) {
        this.chatIconManager = chatIconManager;
    }

    public BufferedImage getRankImage(DMSafePlugin plugin, String playerName) {
        BufferedImage dmerRankImage = getImage(plugin.data.getDmerRank(playerName));

        if (dmerRankImage == null) {
            Deathmatcher dmer = plugin.getLocalDeathmatchers().getOrDefault(playerName, new Deathmatcher("n/a", "n/a", playerName, DMER_NAME, DMER_NAME));
            dmerRankImage = getImage(dmer.getRank());
        }

        if (dmerRankImage != null) {
            return dmerRankImage;
        }
        return DMER_ICON;
    }

    public BufferedImage getImage(String rankName) {
        int rankNumber = -1;
        switch (rankName) {
            case DMER_NAME:
                return DMER_ICON;
            case SCAMMER_NAME:
                return SCAMMER_ICON;
            case FRIEND_NAME:
                rankNumber = 0;
                break;
            case RECRUIT_NAME:
                rankNumber = 1;
                break;
            case CORPORAL_NAME:
                rankNumber = 2;
                break;
            case SERGEANT_NAME:
                rankNumber = 3;
                break;
            case LIEUTENANT_NAME:
                rankNumber = 4;
                break;
            case CAPTAIN_NAME:
                rankNumber = 5;
                break;
            case GENERAL_NAME:
                rankNumber = 6;
                break;
            case OWNER_NAME:
                rankNumber = 7;
                break;
        }
        if (rankNumber >= 0) {
            return chatIconManager.getRankImage(FriendsChatRank.valueOf(rankNumber));
        }
        return null;
    }
}
