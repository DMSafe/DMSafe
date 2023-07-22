package com.dmsafe.web;

import com.dmsafe.DMSafePlugin;
import com.google.gson.Gson;
import lombok.Getter;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.Date;

import static com.dmsafe.DMSafePlugin.*;

public class DMSafeData implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DMSafePlugin.class);
    private static final String DATA_ENDPOINT = "https://dmsafely.com/api/deathmatchers";
    private long lastConnectionRequest = 0;

    @Getter
    private Deathmatcher[] dmers;

    private boolean isRunning = true;

    private final DMSafePlugin plugin;

    public DMSafeData(DMSafePlugin plugin) {
        this.plugin = plugin;
    }

    public void run() {
        try {
            while (isRunning) {
                if (readyToSendAnotherRequest()) {
                    lastConnectionRequest = System.currentTimeMillis();

                    URL url = new URL(DATA_ENDPOINT);
                    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                    }
                    line = sb.toString();
                    line = "[{" + line.substring(12, line.length() - 1);
                    try {
                        Gson gson = new Gson();
                        dmers = gson.fromJson(line, Deathmatcher[].class);
                    } catch (Exception e) {
                        log.error(new Date() + " - Exception obtaining Deathmatchers: " + e.getMessage());
                    }
                    in.close();
                }
            }
        } catch (Exception e) {
            log.error(new Date() + " - Error Gathering DMSafe Data: " + e.getMessage());
        }
    }

    private boolean readyToSendAnotherRequest() {
        return System.currentTimeMillis() >= lastConnectionRequest + 5000;
    }

    public boolean rsnInTheSystem(String username) {
        String cleanRSN = Text.toJagexName(username);
        if (dmers != null) {
            for (Deathmatcher dmer : dmers) {
                if (dmer.getRSN().equalsIgnoreCase(cleanRSN)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getDmerRank(String username) {
        String cleanRSN = Text.toJagexName(username);
        if (dmers != null) {
            for (Deathmatcher dmer : dmers) {
                if (dmer.getRSN().equalsIgnoreCase(cleanRSN)) {
                    return dmer.getRank();
                }
            }
        }
        if (plugin.getLocalDeathmatchers().containsKey(username)) {
            return plugin.getLocalDeathmatchers().get(username).getRank();
        }
        return DMER_NAME;
    }

    public String getDmerRankAndReason(String username) {
        String cleanRSN = Text.toJagexName(username);
        if (dmers != null) {
            for (Deathmatcher dmer : dmers) {
                if (dmer.getRSN().equalsIgnoreCase(cleanRSN)) {
                    return dmer.getRank() + ":" + dmer.getInformation();
                }
            }
        }
        if (plugin.getLocalDeathmatchers().containsKey(username)) {
            Deathmatcher dmer = plugin.getLocalDeathmatchers().get(username);
            return dmer.getRank() + ":" + dmer.getInformation();
        }
        //log.info("Returning " + DMER_NAME + " when trying to also obtain the reason for " + username);
        return DMER_NAME;
    }

    public Deathmatcher getDmer(String playerName, String hardwareID, String accountID) {
        String cleanRSN = Text.toJagexName(playerName);
        if (dmers != null) {
            for (Deathmatcher dmer : dmers) {
                if (dmer.getHWID() != null) {
                    //log.info("HWID Found: " + dmer.getHWID());
                    if (dmer.getHWID().equals(hardwareID)) {
                        return dmer;
                    }
                }
                if (dmer.getAccountID() != null) {
                    //log.info("Account ID Found: " + dmer.getAccountID());
                    if (dmer.getAccountID().equals(accountID)) {
                        return dmer;
                    }
                }
                if (dmer.getRSN() != null) {
                    if (dmer.getRSN().equalsIgnoreCase(cleanRSN)) {
                        return dmer;
                    }
                }
            }
        }
        //log.info("Returning " + DMER_NAME + " because we didnt find hardware id or software id");
        return new Deathmatcher("n/a", "n/a", playerName, DMER_NAME, DMER_NAME);
    }

    public Color getColor(String username) {
        String cleanRSN = Text.toJagexName(username);
        if (dmers != null) {
            for (Deathmatcher dmer : dmers) {
                if (dmer.getRSN().equalsIgnoreCase(cleanRSN)) {
                    return color(dmer.getRank());
                }
            }
        }
        return plugin.getLocalDeathmatchers().containsKey(username) ? color(plugin.getLocalDeathmatchers().get(username).getRank()) : Color.WHITE;
    }

    public Color color(String rankName) {
        ;
        switch (rankName) {
            case OWNER_NAME:
                return new Color(48, 213, 200);
            case FRIEND_NAME:
            case RECRUIT_NAME:
            case CORPORAL_NAME:
            case SERGEANT_NAME:
            case GENERAL_NAME:
                return Color.YELLOW;
            case LIEUTENANT_NAME:
                return Color.ORANGE;
            case CAPTAIN_NAME:
                return Color.MAGENTA;
            case SCAMMER_NAME:
                return Color.RED;
        }
        return null;
    }

    public boolean showAboveHead(String username) {
        String cleanRSN = Text.toJagexName(username);
        if (dmers != null) {
            for (Deathmatcher dmer : dmers) {
                if (dmer.getRSN().equalsIgnoreCase(cleanRSN)) {
                    return showRankAboveHead(dmer.getRank());
                }
            }
        }
        return false;
    }

    public boolean isTrustedRanked(String username) {
        String cleanRSN = Text.toJagexName(username);
        if (dmers != null) {
            for (Deathmatcher dmer : dmers) {
                if (dmer.getRSN().equalsIgnoreCase(cleanRSN)) {
                    return showRankMinimap(dmer.getRank());
                }
            }
        }
        return false;
    }

    public boolean showRankAboveHead(String rankName) {
        switch (rankName) {
            case FRIEND_NAME:
            case RECRUIT_NAME:
            case CORPORAL_NAME:
            case SERGEANT_NAME:
            case LIEUTENANT_NAME:
            case CAPTAIN_NAME:
            case GENERAL_NAME:
            case OWNER_NAME:
            case SCAMMER_NAME:
                return true;
            default:
                return false;
        }
    }

    public boolean showRankMinimap(String rankName) {
        switch (rankName) {
            case FRIEND_NAME:
            case RECRUIT_NAME:
            case CORPORAL_NAME:
            case SERGEANT_NAME:
            case LIEUTENANT_NAME:
            case CAPTAIN_NAME:
            case GENERAL_NAME:
            case OWNER_NAME:
                return true;
            default:
                return false;
        }
    }
}
