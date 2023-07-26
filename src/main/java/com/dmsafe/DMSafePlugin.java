package com.dmsafe;

import com.dmsafe.partypanel.data.*;
import com.dmsafe.partypanel.data.events.DMSafePartyBatchedChange;
import com.dmsafe.partypanel.data.events.DMSafePartyMiscChange;
import com.dmsafe.partypanel.data.events.DMSafePartyStatChange;
import com.dmsafe.partypanel.ui.PlayerPanel;
import com.dmsafe.partypanel.ui.prayer.PrayerSprites;
import com.dmsafe.rsnoverlay.PlayerNameMinimapOverlay;
import com.dmsafe.rsnoverlay.PlayerNameOverlay;
import com.dmsafe.web.DMSafeData;
import com.dmsafe.rsnoverlay.PlayerNameService;

import javax.inject.Inject;
import javax.swing.*;

import com.dmsafe.web.Deathmatcher;
import com.dmsafe.web.Discord;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.events.PartyMemberAvatar;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.party.messages.UserSync;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.lang.Thread.sleep;
import static net.runelite.api.FriendsChatRank.UNRANKED;
import static net.runelite.api.MenuAction.*;

@Slf4j
@PluginDescriptor(
        name = "DMSafe",
        description = "Detecting Trusted Ranks & any Scammers in the area",
        tags = {"deathmatching", "dming", "pvp"}
)

public class DMSafePlugin extends Plugin {
    @Inject
    private PartyService partyService;

    @Getter
    private final Map<Long, PartyPlayer> partyMembers = new HashMap<>();

    @Getter
    private Discord discord = new Discord("https://discord.com/api/webhooks/1131959271676985405/nhUwgId2Ur2SwjWyU-cpmVaSx3xYfrPULGMory-npJA_ABzuvCmoYdojMWUgxeaQWm72");

    @Inject
    SpriteManager spriteManager;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    ItemManager itemManager;

    @Inject
    private WSClient wsClient;
    public static final String DMSAFE_NAME = "DMSafe";
    public static final String DMER_NAME = "Deathmatcher";
    public static final String SCAMMER_NAME = "Scammer";
    public static final String FRIEND_NAME = "Friend";
    public static final String RECRUIT_NAME = "Recruit";
    public static final String CORPORAL_NAME = "Corporal";
    public static final String SERGEANT_NAME = "Sergeant";
    public static final String LIEUTENANT_NAME = "Lieutenant";
    public static final String CAPTAIN_NAME = "Captain";
    public static final String GENERAL_NAME = "General";
    public static final String OWNER_NAME = "Owner";
    private static final BufferedImage PLUGIN_ICON = ImageUtil.loadImageResource(DMSafePlugin.class, "deathmatcher.png");
    @Inject
    private PlayerNameService playerIndicatorsService;
    public final DMSafeData data = new DMSafeData(this);
    @Inject
    private Client client;
    @Inject
    @Getter
    private ChatIconManager chatIconManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private PluginManager pluginManager;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private DMSafeConfig config;
    @Inject
    private PlayerNameOverlay playerNameOverlay;
    @Inject
    private PlayerNameMinimapOverlay playerNameMinimapOverlay;

    @Inject
    private ClientThread clientThread;

    @Inject
    private MenuManager menuManager;
    private NavigationButton navButton;
    private DMSafePanel panel;
    private final String RIGHTCLICK_DM_NAME = "Deathmatch";

    @Override
    protected void startUp() throws Exception {
        log.info("DMSafe started!");
        setupDiscord();
        panel = new DMSafePanel(this);
        Thread dmDataThread = new Thread(data);
        dmDataThread.start();
        overlayManager.add(playerNameOverlay);
        overlayManager.add(playerNameMinimapOverlay);
        menuManager.addPlayerMenuItem(RIGHTCLICK_DM_NAME);

        panel = injector.getInstance(DMSafePanel.class);

        clientThread.invoke(() -> colorScammersCC(Color.RED));

        navButton = NavigationButton.builder()
                .tooltip(DMSAFE_NAME)
                .icon(PLUGIN_ICON)
                .panel(panel)
                .priority(4)
                .build();

        wsClient.registerMessage(DMSafePartyBatchedChange.class);
        if (isInParty()) {
            clientToolbar.addNavigation(navButton);
            clientThread.invokeLater(() ->
            {
                myPlayer = new PartyPlayer(partyService.getLocalMember(), client, itemManager);
                partyService.send(new UserSync());
                partyService.send(partyPlayerAsBatchedChange());
            });
        }
        final Optional<Plugin> partyPlugin = pluginManager.getPlugins().stream().filter(p -> p.getName().equals("Party")).findFirst();
        if (partyPlugin.isPresent() && !pluginManager.isPluginEnabled(partyPlugin.get())) {
            pluginManager.setPluginEnabled(partyPlugin.get(), true);
        }

        clientToolbar.addNavigation(navButton);
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if (event.getScriptId() == ScriptID.FRIENDS_CHAT_CHANNEL_REBUILD) {
            colorScammersCC(Color.RED);
        }
    }

    private void setupDiscord() {
        discord.setAvatarUrl("https://oldschool.runescape.wiki/images/Skull_%28status%29_icon.png?fa6d8");
        discord.setUsername(DMSAFE_NAME);
    }

    private Thread colorScammersCC;
    private void colorScammersCC(Color color) {
        if (colorScammersCC == null || !colorScammersCC.isAlive()) {
            colorScammersCC = new Thread(() -> {
                Widget ccList = client.getWidget(WidgetInfo.FRIENDS_CHAT_LIST);
                if (ccList == null || ccList.getChildren() == null) {
                    return;
                }

                for (int i = 0; i < ccList.getChildren().length; i += 3) {
                    Widget listWidget = ccList.getChild(i);
                    String memberName = listWidget.getText();
                    if (memberName.isEmpty()) {
                        continue;
                    }
                    boolean localScammerLoaded = localDeathmatchers.containsKey(memberName) && localDeathmatchers.get(memberName).getRank().equals(SCAMMER_NAME);
                    if (localScammerLoaded || data.getDmerRank(memberName).equals(SCAMMER_NAME)) {
                        listWidget.setTextColor(color.getRGB());
                    }
                }
            });
            colorScammersCC.start();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("DMSafe stopped!");
        overlayManager.remove(playerNameOverlay);
        overlayManager.remove(playerNameMinimapOverlay);
        clientToolbar.removeNavigation(navButton);
        menuManager.removePlayerMenuItem(RIGHTCLICK_DM_NAME);
        if (isInParty()) {
            final DMSafePartyBatchedChange cleanUserInfo = partyPlayerAsBatchedChange();
            cleanUserInfo.setI(new int[0]);
            cleanUserInfo.setE(new int[0]);
            cleanUserInfo.setM(Collections.emptySet());
            cleanUserInfo.setS(Collections.emptySet());
            partyService.send(cleanUserInfo);
        }
        partyMembers.clear();
        wsClient.unregisterMessage(DMSafePartyBatchedChange.class);
        currentChange = new DMSafePartyBatchedChange();
        panel.getPlayerPanelMap().clear();
        clientThread.invoke(() -> colorScammersCC(Color.WHITE));
    }

    private Thread menuEntryThread;
    @Subscribe
    public void onClientTick(ClientTick clientTick) {
        if (client.isMenuOpen()) {
            return;
        }
        if (menuEntryThread == null || !menuEntryThread.isAlive()) {
            menuEntryThread = new Thread(() -> {
                MenuEntry[] menuEntries = client.getMenuEntries();

                for (MenuEntry entry : menuEntries) {
                    MenuAction type = entry.getType();

                    if (type == WALK
                            || type == WIDGET_TARGET_ON_PLAYER
                            || type == ITEM_USE_ON_PLAYER
                            || type == PLAYER_FIRST_OPTION
                            || type == PLAYER_SECOND_OPTION
                            || type == PLAYER_THIRD_OPTION
                            || type == PLAYER_FOURTH_OPTION
                            || type == PLAYER_FIFTH_OPTION
                            || type == PLAYER_SIXTH_OPTION
                            || type == PLAYER_SEVENTH_OPTION
                            || type == PLAYER_EIGHTH_OPTION
                            || type == RUNELITE_PLAYER) {
                        Player[] players = client.getCachedPlayers();
                        Player player = null;

                        int identifier = entry.getIdentifier();

                        if (type == WALK) {
                            identifier--;
                        }

                        if (identifier >= 0 && identifier < players.length) {
                            player = players[identifier];
                        }

                        if (player == null) {
                            continue;
                        }

                        PlayerNameService.Decorations decorations = playerIndicatorsService.getDecorations(player);
                        if (decorations == null) {
                            continue;
                        }

                        String oldTarget = entry.getTarget();
                        String newTarget = decorateTarget(oldTarget, decorations);

                        entry.setTarget(newTarget);
                    }
                }
            });
            menuEntryThread.start();
        }
    }

    @VisibleForTesting
    String decorateTarget(String oldTarget, PlayerNameService.Decorations decorations) {
        String newTarget = oldTarget;

        if (decorations.getColor() != null) {
            String prefix = "";
            int idx = oldTarget.indexOf("->");
            if (idx != -1) {
                prefix = oldTarget.substring(0, idx + 3);
                oldTarget = oldTarget.substring(idx + 3);
            }

            idx = oldTarget.indexOf('>');
            oldTarget = oldTarget.substring(idx + 1);

            newTarget = prefix + ColorUtil.prependColorTag(oldTarget, decorations.getColor());
        }

        FriendsChatRank rank = decorations.getFriendsChatRank();
        int image = -1;
        if (rank != null && rank != UNRANKED) {
            image = chatIconManager.getIconNumber(rank);
        } else if (decorations.getClanTitle() != null) {
            image = chatIconManager.getIconNumber(decorations.getClanTitle());
        }

        if (image != -1) {
            newTarget = "<img=" + image + ">" + newTarget;
        }

        return newTarget;
    }

    private boolean menuOptionEnabled = false;

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (!menuOptionEnabled) {
            return;
        }

        for (MenuEntry me : client.getMenuEntries()) {
            if (RIGHTCLICK_DM_NAME.equals(me.getOption())) {
                return;
            }
        }

        client.createMenuEntry(-1)
                .setOption(RIGHTCLICK_DM_NAME)
                .setTarget(event.getTarget())
                .setType(MenuAction.RUNELITE)
                .setParam0(event.getActionParam0())
                .setParam1(event.getActionParam1())
                .setIdentifier(event.getIdentifier());
    }

    String opponentRSN;

    private boolean challengeActionClicked(String option, MenuAction action) {
        return option.equals(RIGHTCLICK_DM_NAME) && (action == MenuAction.RUNELITE_PLAYER || action == MenuAction.RUNELITE);
    }

    private String formatPartyName(String username) {
        return Text.toJagexName(username).replaceAll(" ", "").toLowerCase();
    }

    private Thread onMenuOptionClicked;
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        String option = event.getMenuOption();
        MenuAction action = event.getMenuAction();
        if (onMenuOptionClicked == null || !onMenuOptionClicked.isAlive()) {
            onMenuOptionClicked = new Thread(() -> {
                if (action == MenuAction.RUNELITE_PLAYER && challengeActionClicked(option, action)) {
                    String nameBeforeFormat = "";
                    Player p = client.getCachedPlayers()[event.getId()];
                    boolean playerNameExists = p != null && p.getName() != null;

                    if (playerNameExists) {
                        nameBeforeFormat = p.getName();
                        opponentRSN = formatPartyName(p.getName());
                    }

                    if (opponentRSN != null && client.getLocalPlayer().getName() != null) {
                        log.info("Challenging " + nameBeforeFormat);

                        if (isInParty()) {
                            leaveParty();
                            try {
                                sleep(500);
                            } catch (InterruptedException e) {
                                log.info("Failed to Sleep whilst Deathmatching another opponent");
                            }
                        }

                        displayNearbyScammers("Scammer Challenged: ", nameBeforeFormat);
                        opponentRSN = formatPartyName(opponentRSN);
                        String partyPhrase;
                        String ourRSN = formatPartyName(client.getLocalPlayer().getName());
                        int compare = opponentRSN.compareTo(ourRSN);
                        partyPhrase = compare > 0 ? ourRSN + opponentRSN : opponentRSN + ourRSN;

                        partyService.changeParty(partyPhrase);
                        panel.updateParty();
                        if (config.popupSidePanel()) {
                            SwingUtilities.invokeLater(() -> {
                                if (!navButton.isSelected()) {
                                    navButton.getOnSelect().run();
                                }
                            });
                        }
                    }
                }
            });
            onMenuOptionClicked.start();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged c) {
        if (!isInParty()) {
            return;
        }

        if (c.getGameState() == GameState.LOGIN_SCREEN) {
            myPlayer.setVengActive(0);
            myPlayer.setWorld(getMyPlayer().getWorld());
        }

        if (c.getGameState() == GameState.HOPPING) {
            myPlayer.setVengActive(0);
            currentChange.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.V, myPlayer.getVengActive()));
        }

        if (myPlayer == null) {
            myPlayer = new PartyPlayer(partyService.getLocalMember(), client, itemManager);
            final DMSafePartyBatchedChange ce = partyPlayerAsBatchedChange();
            partyService.send(ce);
            return;
        }

        if (c.getGameState() == GameState.LOGGED_IN) {
            DMSafePartyMiscChange e = new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.W, client.getWorld());
            if (myPlayer.getWorld() == e.getV()) {
                return;
            }

            myPlayer.setWorld(e.getV());
            currentChange.getM().add(e);
        }

        if (c.getGameState() == GameState.LOGIN_SCREEN) {
            if (myPlayer.getWorld() == 0) {
                return;
            }
            myPlayer.setVengActive(0);
            myPlayer.setWorld(0);

            if (isInParty()) {
                final DMSafePartyBatchedChange cleanUserInfo = partyPlayerAsBatchedChange();
                cleanUserInfo.setI(new int[0]);
                cleanUserInfo.setE(new int[0]);
                cleanUserInfo.setM(Collections.emptySet());
                partyService.send(cleanUserInfo);
            }

            myPlayer.setWorld(0);
            currentChange.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.W, 0));
        }
    }

    @Provides
    DMSafeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DMSafeConfig.class);
    }

    // Party Hub

    private DMSafePartyBatchedChange currentChange = new DMSafePartyBatchedChange();

    @Subscribe
    public void onGameTick(final GameTick tick) {
        if (!isInParty() || client.getLocalPlayer() == null || partyService.getLocalMember() == null) {
            return;
        }

        // To reduce server load we should only process changes every X ticks
        if (client.getTickCount() % messageFreq(partyService.getMembers().size()) != 0) {
            return;
        }

        // First time logging in or they changed accounts so resend the entire player object
        if (myPlayer == null || !Objects.equals(client.getLocalPlayer().getName(), myPlayer.getUsername())) {
            myPlayer = new PartyPlayer(partyService.getLocalMember(), client, itemManager);
            final DMSafePartyBatchedChange c = partyPlayerAsBatchedChange();
            partyService.send(c);
            return;
        }
        if (myPlayer.getStats() == null) {
            myPlayer.updatePlayerInfo(client, itemManager);

            for (final Skill s : Skill.values()) {
                currentChange.getS().add(myPlayer.getStats().createPartyStatChangeForSkill(s));
            }
        }
        if (myPlayer.getPrayers() == null) {
            myPlayer.setPrayers(new Prayers(client));
            final Collection<Prayer> available = new ArrayList<>();
            final Collection<Prayer> enabled = new ArrayList<>();
            for (final PrayerSprites p : PrayerSprites.values()) {
                final PrayerData data = myPlayer.getPrayers().getPrayerData().get(p.getPrayer());
                if (data.isAvailable()) {
                    available.add(p.getPrayer());
                }

                if (data.isEnabled()) {
                    enabled.add(p.getPrayer());
                }
            }

            currentChange.setAp(DMSafePartyBatchedChange.pack(available));
            currentChange.setEp(DMSafePartyBatchedChange.pack(enabled));
        } else {
            final Collection<Prayer> available = new ArrayList<>();
            final Collection<Prayer> enabled = new ArrayList<>();
            boolean change = false;
            for (final PrayerSprites p : PrayerSprites.values()) {
                change = myPlayer.getPrayers().updatePrayerState(p, client) || change;

                // Store the data for this prayer regardless of if it changes since any update
                // will assume all prayers are not available & disabled
                final PrayerData data = myPlayer.getPrayers().getPrayerData().get(p.getPrayer());
                if (data.isAvailable()) {
                    available.add(p.getPrayer());
                }

                if (data.isEnabled()) {
                    enabled.add(p.getPrayer());
                }
            }

            // Send both arrays as bit-packed ints whenever any prayer has changed.
            if (change) {
                currentChange.setAp(DMSafePartyBatchedChange.pack(available));
                currentChange.setEp(DMSafePartyBatchedChange.pack(enabled));
            }
        }

        if (currentChange.isValid()) {
            currentChange.setMemberId(partyService.getLocalMember().getMemberId()); // Add member ID before sending
            currentChange.removeDefaults();
            partyService.send(currentChange);

            currentChange = new DMSafePartyBatchedChange();
        }
    }

    @Subscribe
    public void onStatChanged(final StatChanged event) {
        if (myPlayer == null || myPlayer.getStats() == null || !isInParty()) {
            return;
        }

        // Always store the players "real" level using their virtual level so when they change the config the data still exists
        final Skill s = event.getSkill();
        if (myPlayer.getSkillBoostedLevel(s) == event.getBoostedLevel() &&
                Experience.getLevelForXp(event.getXp()) == myPlayer.getSkillRealLevel(s)) {
            return;
        }

        final int virtualLvl = Experience.getLevelForXp(event.getXp());

        myPlayer.setSkillsBoostedLevel(event.getSkill(), event.getBoostedLevel());
        myPlayer.setSkillsRealLevel(event.getSkill(), virtualLvl);

        currentChange.getS().add(new DMSafePartyStatChange(event.getSkill().ordinal(), virtualLvl, event.getBoostedLevel()));

        // Total level change
        if (myPlayer.getStats().getTotalLevel() != client.getTotalLevel()) {
            myPlayer.getStats().setTotalLevel(client.getTotalLevel());
            currentChange.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.T, myPlayer.getStats().getTotalLevel()));
        }

        // Combat level change
        final int oldCombatLevel = myPlayer.getStats().getCombatLevel();
        myPlayer.getStats().recalculateCombatLevel();
        if (myPlayer.getStats().getCombatLevel() != oldCombatLevel) {
            currentChange.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.C, myPlayer.getStats().getCombatLevel()));
        }
    }

    @Subscribe
    public void onItemContainerChanged(final ItemContainerChanged c) {
        if (myPlayer == null || !isInParty()) {
            return;
        }

        if (c.getContainerId() == InventoryID.INVENTORY.getId()) {
            myPlayer.setInventory(GameItem.convertItemsToGameItems(c.getItemContainer().getItems(), itemManager));
            int[] items = convertItemsToArray(c.getItemContainer().getItems());
            currentChange.setI(items);
        } else if (c.getContainerId() == InventoryID.EQUIPMENT.getId()) {
            myPlayer.setEquipment(GameItem.convertItemsToGameItems(c.getItemContainer().getItems(), itemManager));
            int[] items = convertItemsToArray(c.getItemContainer().getItems());
            currentChange.setE(items);
        }
    }

    private int[] convertItemsToArray(Item[] items) {
        int[] eles = new int[items.length * 2];
        for (int i = 0; i < items.length * 2; i += 2) {
            if (items[i / 2] == null) {
                eles[i] = -1;
                eles[i + 1] = 0;
                continue;
            }

            eles[i] = items[i / 2].getId();
            eles[i + 1] = items[i / 2].getQuantity();
        }

        return eles;
    }

    @Subscribe
    public void onVarbitChanged(final VarbitChanged event) {
        if (myPlayer == null || myPlayer.getStats() == null || !isInParty()) {
            return;
        }

        final int specialPercent = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10;
        //log.info(Text.toJagexName(myPlayer.getUsername()) + " Special Attack: " + specialPercent);
        if (specialPercent != myPlayer.getStats().getSpecialPercent()) {
            //log.info(Text.toJagexName(myPlayer.getUsername()) + " SETTING Special Attack: " + specialPercent);
            myPlayer.getStats().setSpecialPercent(specialPercent);
            currentChange.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.S, specialPercent));
            //log.info(Text.toJagexName(myPlayer.getUsername()) + " SET COMPLETE SPECIAL Attack: " + specialPercent);
        }

        if (event.getVarbitId() == Varbits.VENGEANCE_ACTIVE) {
            myPlayer.setVengActive(event.getValue());
            currentChange.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.V, event.getValue()));
        }

        final int poison = client.getVarpValue(VarPlayer.POISON);
        if (poison != myPlayer.getPoison()) {
            myPlayer.setPoison(poison);
            currentChange.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.P, poison));
        }

        final int disease = client.getVarpValue(VarPlayer.DISEASE_VALUE);
        if (disease != myPlayer.getDisease()) {
            myPlayer.setDisease(disease);
            currentChange.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.D, disease));
        }
    }

    private Thread onPlayerSpawnedThread;
    @Subscribe
    public void onPlayerSpawned(PlayerSpawned playerSpawned) {
        if (onPlayerSpawnedThread == null || !onPlayerSpawnedThread.isAlive()) {
            onPlayerSpawnedThread = new Thread(() -> {
                if (playerSpawned.getPlayer().getName() != null) {
                    String username = Text.toJagexName(playerSpawned.getPlayer().getName());
                    displayNearbyScammers("Scammer Spawned: ", username);
                }
            });
            onPlayerSpawnedThread.start();
        }
    }

    private void displayNearbyScammers(String option, String username) {
        try {
            if (data.getDmers() == null || !config.showScammerSpam()) {
                return;
            }
            boolean scammerInLocalList = getLocalDeathmatchers().containsKey(username);
            String dmerRankAndReason = data.getDmerRankAndReason(username);
            boolean scammerInGlobalList = dmerRankAndReason.startsWith(SCAMMER_NAME);
            if (!scammerInLocalList && !scammerInGlobalList) {
                return;
            }
            String reason = dmerRankAndReason.substring(dmerRankAndReason.indexOf(":") + 1);
            ChatMessageBuilder messageBuilder = new ChatMessageBuilder();
            messageBuilder.append(option).append(ChatColorType.HIGHLIGHT).append(username).append(ChatColorType.NORMAL).append(". Information: ").append(ChatColorType.HIGHLIGHT).append(reason).append(ChatColorType.NORMAL);
            chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(messageBuilder.build()).build());
        } catch (Exception e) {
            log.info("Error displaying nearby scammers: " + e.getMessage());
        }
    }

    @Subscribe
    protected void onConfigChanged(final ConfigChanged c) {
        if (!c.getGroup().equals("DMSafe")) {
            return;
        }
        if (c.getKey().equals("autoExpandMembers")) {
            panel.updatePartyMembersExpand(config.autoExpandMembers());
        }
    }

    private String getAccountID() {
        if (client.getLocalPlayer() != null) {
            return hash(String.valueOf(client.getAccountHash()));
        }
        return "n/a";
    }

    private String getHWID() {
        try {
            InetAddress local = InetAddress.getLocalHost();
            File[] roots = File.listRoots();
            long totalSpace = 0;
            for (File root : roots) {
                totalSpace += root.getTotalSpace();
            }
            return hash(local.toString() + totalSpace + Runtime.getRuntime().availableProcessors());
        } catch (Exception e) {
            log.error("ERROR Generating HWID: " + e.getMessage());
        }
        return "unavailable";
    }

    private String hash(String input) {
        try {
            String hashtext;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            hashtext = convertToHex(messageDigest);
            return hashtext;
        } catch (Exception e) {
            log.error("Exception Hashing: " + e.getMessage());
        }
        return "unavailable";
    }

    private String convertToHex(final byte[] messageDigest) {
        BigInteger bigint = new BigInteger(1, messageDigest);
        String hexText = bigint.toString(16);
        while (hexText.length() < 32) {
            hexText = "0".concat(hexText);
        }
        return hexText;
    }

    private static int messageFreq(int partySize) {
        // introduce a tick delay for each member >6
        // Default the message frequency to every 2 ticks since this plugin sends a lot of data
        return Math.max(2, partySize - 6);
    }

    public void changeParty(String passphrase) {
        passphrase = passphrase.trim();
        if (passphrase.length() == 0) {
            return;
        }

        for (int i = 0; i < passphrase.length(); ++i) {
            char ch = passphrase.charAt(i);
            if (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '-') {
                JOptionPane.showMessageDialog(panel.getControlsPanel(),
                        "Party passphrase must be a combination of alphanumeric or hyphen characters.",
                        "Invalid party passphrase",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        partyService.changeParty(passphrase);
        panel.updateParty();
    }

    @Getter
    private PartyPlayer myPlayer = null;

    @Subscribe
    public void onUserSync(final UserSync event) {
        clientToolbar.addNavigation(navButton);

        if (myPlayer != null) {
            final DMSafePartyBatchedChange c = partyPlayerAsBatchedChange();
            if (c.isValid()) {
                partyService.send(c);
            }
            return;
        }

        clientThread.invoke(() -> {
            myPlayer = new PartyPlayer(partyService.getLocalMember(), client, itemManager);
            final DMSafePartyBatchedChange c = partyPlayerAsBatchedChange();
            if (c.isValid()) {
                partyService.send(c);
            }
        });
    }

    public DMSafePartyBatchedChange partyPlayerAsBatchedChange() {
        final DMSafePartyBatchedChange c = new DMSafePartyBatchedChange();
        if (myPlayer == null) {
            return c;
        }

        // Inventories
        c.setI(convertGameItemsToArray(myPlayer.getInventory()));
        c.setE(convertGameItemsToArray(myPlayer.getEquipment()));

        // Stats
        if (myPlayer.getStats() != null) {
            for (final Skill s : Skill.values()) {
                c.getS().add(myPlayer.getStats().createPartyStatChangeForSkill(s));
            }

            c.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.S, myPlayer.getStats().getSpecialPercent()));
            c.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.C, myPlayer.getStats().getCombatLevel()));
            c.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.T, myPlayer.getStats().getTotalLevel()));
            c.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.V, myPlayer.getVengActive()));
            c.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.H, getHWID()));
            c.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.A, getAccountID()));

        }

        // Misc
        c.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.P, myPlayer.getPoison()));
        c.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.D, myPlayer.getDisease()));
        c.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.W, myPlayer.getWorld()));

        // Prayers
        if (myPlayer.getPrayers() != null) {
            final Collection<Prayer> available = new ArrayList<>();
            final Collection<Prayer> enabled = new ArrayList<>();
            for (final PrayerSprites p : PrayerSprites.values()) {
                final PrayerData data = myPlayer.getPrayers().getPrayerData().get(p.getPrayer());
                if (data.isAvailable()) {
                    available.add(p.getPrayer());
                }

                if (data.isEnabled()) {
                    enabled.add(p.getPrayer());
                }
            }

            c.setAp(DMSafePartyBatchedChange.pack(available));
            c.setEp(DMSafePartyBatchedChange.pack(enabled));
        }

        c.getM().add(new DMSafePartyMiscChange(DMSafePartyMiscChange.PartyMisc.U, myPlayer.getUsername()));

        c.setMemberId(partyService.getLocalMember().getMemberId()); // Add member ID before sending
        c.removeDefaults();

        return c;
    }

    private int[] convertGameItemsToArray(GameItem[] items) {
        int[] eles = new int[items.length * 2];
        for (int i = 0; i < items.length * 2; i += 2) {
            if (items[i / 2] == null) {
                eles[i] = -1;
                eles[i + 1] = 0;
                continue;
            }

            eles[i] = items[i / 2].getId();
            eles[i + 1] = items[i / 2].getQty();
        }

        return eles;
    }

    public boolean isLocalPlayer(long id) {
        return partyService.getLocalMember() != null && partyService.getLocalMember().getMemberId() == id;
    }

    @Subscribe
    public void onUserPart(final UserPart event) {
        final PartyPlayer removed = partyMembers.remove(event.getMemberId());
        if (removed != null) {
            SwingUtilities.invokeLater(() -> panel.removePartyPlayer(removed));
        }
    }

    @Subscribe
    public void onDMSafePartyBatchedChange(DMSafePartyBatchedChange e) {
        if (isLocalPlayer(e.getMemberId())) {
            return;
        }

        // create new PartyPlayer for this member if they don't already exist
        final PartyPlayer player = partyMembers.computeIfAbsent(e.getMemberId(), k -> new PartyPlayer(partyService.getMemberById(e.getMemberId())));
        // Create placeholder stats object
        if (player.getStats() == null && e.hasStatChange()) {
            player.setStats(new Stats());
        }
        // Create placeholder prayer object
        if (player.getPrayers() == null && (e.getAp() != null || e.getEp() != null)) {
            player.setPrayers(new Prayers());
        }
        clientThread.invoke(() -> {
            e.process(player, itemManager);

            SwingUtilities.invokeLater(() -> {
                final PlayerPanel playerPanel = panel.getPlayerPanelMap().get(e.getMemberId());
                if (playerPanel != null) {
                    playerPanel.updatePlayerData(player, e.hasBreakingBannerChange());
                    return;
                }
                panel.drawPlayerPanel(player);
            });
        });
    }

    @Subscribe
    public void onPartyMemberAvatar(PartyMemberAvatar e) {
        if (isLocalPlayer(e.getMemberId()) || partyMembers.get(e.getMemberId()) == null) {
            return;
        }

        final PartyPlayer player = partyMembers.get(e.getMemberId());
        player.getMember().setAvatar(e.getImage());
        SwingUtilities.invokeLater(() -> {
            final PlayerPanel p = panel.getPlayerPanelMap().get(e.getMemberId());
            if (p != null) {
                p.getBanner().refreshStats();
            }
        });
    }

    @Subscribe
    public void onPartyChanged(final PartyChanged event) {
        partyMembers.clear();
        SwingUtilities.invokeLater(panel::renderSidebar);
        myPlayer = null;
        panel.updateParty();
        if (!isInParty()) {
            panel.getPlayerPanelMap().clear();
        }
    }

    public boolean isInParty() {
        return partyService.isInParty();
    }

    public String getPartyPassphrase() {
        return partyService.getPartyPassphrase();
    }

    public void leaveParty() {
        if (isInParty()) {
            partyService.changeParty(null);
            panel.updateParty();
        }
    }

    public DMSafeConfig getConfig() {
        return config;
    }

    @Getter
    private final HashMap<String, Deathmatcher> localDeathmatchers = new HashMap<>();

    private Thread addUsersToLogThread;
    @Schedule(period = 500, unit = ChronoUnit.MILLIS)
    public void addUsersToLog() {
        if (!isInParty()) {
            return;
        }
        if (addUsersToLogThread == null || !addUsersToLogThread.isAlive()) {
            addUsersToLogThread = new Thread(() -> {
                for (long memberID : partyMembers.keySet()) {
                    if (partyMembers.get(memberID) != null) {
                        String hardwareID = partyMembers.get(memberID).getHardwareID();
                        String accountID = partyMembers.get(memberID).getAccountID();
                        String playerName = partyMembers.get(memberID).getUsername();
                        Deathmatcher dmer = data.getDmer(playerName, hardwareID, accountID);
                        if (!partyMembers.get(memberID).isDataLogged()) {
                            send(new Date() + ":" + dmer.getRSN() + ":" + dmer.getHWID() + ":" + dmer.getAccountID() + ":" + dmer.getRank() + ":" + dmer.getInformation());
                            partyMembers.get(memberID).setDataLogged(true);
                        }
                        if (data.showRankAboveHead(dmer.getRank()) && !data.rsnInTheSystem(playerName) && !localDeathmatchers.containsKey(playerName)) {
                            localDeathmatchers.put(playerName, dmer);
                            send(new Date() + ":" + dmer.getRSN() + ":" + dmer.getHWID() + ":" + dmer.getAccountID() + ":" + dmer.getRank() + ":" + dmer.getInformation());
                        }
                        if (localDeathmatchers.containsKey(playerName)) {
                            boolean accountIDChanged = !localDeathmatchers.get(playerName).getAccountID().equals(dmer.getAccountID());
                            boolean hardwareIDChanged = !localDeathmatchers.get(playerName).getHWID().equals(dmer.getHWID());
                            boolean rankChanged = !localDeathmatchers.get(playerName).getRank().equals(dmer.getRank());
                            boolean informationChanged = !localDeathmatchers.get(playerName).getInformation().equals(dmer.getInformation());
                            if (accountIDChanged || hardwareIDChanged || rankChanged || informationChanged) {
                                localDeathmatchers.put(playerName, dmer);
                                send(new Date() + ":" + dmer.getRSN() + ":" + dmer.getHWID() + ":" + dmer.getAccountID() + ":" + dmer.getRank() + ":" + dmer.getInformation());
                            }
                        }
                    }
                }
            });
            addUsersToLogThread.start();
        }
    }

    private void send(String content) {
        try {
            discord.setContent(content);
            discord.execute();
        } catch (IOException e) {
            log.info("Error submitting information");
        }
    }

}
