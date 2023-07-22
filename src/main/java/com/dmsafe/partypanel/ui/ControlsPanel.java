package com.dmsafe.partypanel.ui;

/*
 * Copyright (c) 2021, Jonathan Rousseau <https://github.com/JoRouss>
 * Copyright (c) 2022, TheStonedTurtle <https://github.com/TheStonedTurtle>
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

import com.dmsafe.DMSafePlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Date;
import javax.swing.*;

import static com.dmsafe.DMSafePlugin.DMSAFE_NAME;

public class ControlsPanel extends JPanel {
    private final JButton joinPartyButton = new JButton();
    private final JButton ccButton = new JButton();

    private final JButton joinDMSafePartyButton = new JButton();
    private final JButton discordButton = new JButton();

    private final JButton leavePartyButton = new JButton();
    private final DMSafePlugin plugin;
    private final JButton dmSafeDirButton = new JButton();
    private static final BufferedImage CC_ICON = ImageUtil.loadImageResource(DMSafePlugin.class, "cc.png");
    private static final BufferedImage DISCORD_ICON = ImageUtil.loadImageResource(DMSafePlugin.class, "discord.png");
    private static final BufferedImage LOGS_ICON = ImageUtil.loadImageResource(DMSafePlugin.class, "logs.png");
    private static final BufferedImage JOIN_ICON = ImageUtil.loadImageResource(DMSafePlugin.class, "join.png");
    private static final BufferedImage LEAVE_ICON = ImageUtil.loadImageResource(DMSafePlugin.class, "leave.png");

    private static final String DISCORD_URL = "https://discord.gg/DMSafe";
    private static final Logger log = LoggerFactory.getLogger(DMSafePlugin.class);

    public ControlsPanel(DMSafePlugin plugin) {
        this.plugin = plugin;
        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        add(ccButton, c);

        c.gridx = 0;
        c.gridy = 1;
        add(dmSafeDirButton, c);

        c.gridx = 1;
        c.gridy = 0;
        add(discordButton, c);

        c.gridx = 1;
        c.gridy = 1;
        add(joinDMSafePartyButton, c);

        c.gridx = 0;
        c.gridy = 2;
        add(joinPartyButton, c);

        c.gridx = 1;
        c.gridy = 2;
        add(leavePartyButton, c);

        final JLabel joinLabel = new JLabel("Join Party");
        joinLabel.setIcon(new ImageIcon(JOIN_ICON));
        joinPartyButton.add(joinLabel);
        joinPartyButton.setFocusable(false);
        joinPartyButton.setHorizontalTextPosition(JLabel.LEFT);

        final JLabel joinDMSafeLabel = new JLabel("DMSafe Party");
        joinDMSafeLabel.setIcon(new ImageIcon(JOIN_ICON));
        joinDMSafePartyButton.add(joinDMSafeLabel);
        joinDMSafePartyButton.setFocusable(false);
        joinDMSafePartyButton.setHorizontalTextPosition(JLabel.LEFT);

        final JLabel leaveLabel = new JLabel("Leave Party");
        leaveLabel.setIcon(new ImageIcon(LEAVE_ICON));
        leavePartyButton.add(leaveLabel);
        leavePartyButton.setFocusable(false);
        leavePartyButton.setHorizontalTextPosition(JLabel.LEFT);

        final JLabel ccLabel = new JLabel(DMSAFE_NAME);
        ccLabel.setIcon(new ImageIcon(CC_ICON));
        ccButton.add(ccLabel);
        ccButton.setEnabled(false);
        ccButton.setToolTipText("Join the in-game Clan Chat: DMSafe");
        ccButton.setHorizontalTextPosition(JLabel.LEFT);

        final JLabel discordLabel = new JLabel("Discord");
        discordLabel.setIcon(new ImageIcon(DISCORD_ICON));
        discordButton.add(discordLabel);
        discordButton.addActionListener(e -> joinDiscord());
        discordButton.setToolTipText("Click me to Join the Discord");
        discordButton.setHorizontalTextPosition(JLabel.LEFT);

        final JLabel dmsafeDataLabel = new JLabel("Logs");
        dmsafeDataLabel.setIcon(new ImageIcon(LOGS_ICON));
        dmSafeDirButton.add(dmsafeDataLabel);
        dmSafeDirButton.addActionListener(e -> openDMSafeDirectory());
        dmSafeDirButton.setToolTipText("Click me to Open the Runelite Logs folder");
        dmSafeDirButton.setHorizontalTextPosition(JLabel.LEFT);

        joinPartyButton.addActionListener(e -> {
            if (!plugin.isInParty()) {
                String s = (String) JOptionPane.showInputDialog(
                        joinPartyButton,
                        "Enter a passphrase:",
                        "Passphrase",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        "");

                if (s == null) {
                    return;
                }

                plugin.changeParty(s);
            }
        });

        joinDMSafePartyButton.addActionListener(e -> {
            if (!plugin.isInParty()) {
                plugin.changeParty(DMSAFE_NAME.toLowerCase());
            }
        });

        leavePartyButton.addActionListener(e -> {
            if (plugin.isInParty()) {
                plugin.leaveParty();
            }
        });

        updateControls();
    }

    public void updateControls() {
        joinPartyButton.setEnabled(!plugin.isInParty());
        joinDMSafePartyButton.setEnabled(!plugin.isInParty());
        leavePartyButton.setEnabled(plugin.isInParty());
    }

    private void joinDiscord() {
        try {
            URI discordURI = new URI(DISCORD_URL);
            Desktop.getDesktop().browse(discordURI);
        } catch (Exception e) {
            log.error(new Date() + " - Failed to open the Discord URL: " + e.getMessage());
        }
    }

    private void openDMSafeDirectory() {
        dmSafeDirButton.addActionListener(e -> LinkBrowser.open(RuneLite.LOGS_DIR.toString()));
    }

}