package com.dmsafe.partypanel.data.events;

/*
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

import com.dmsafe.partypanel.data.PartyPlayer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

// Used for updating stuff that is just a single integer value and doesn't fit into the other classes
@Data
@Slf4j
public class DMSafePartyMiscChange implements PartyProcess {
    PartyMisc t;
    Integer v;
    String s;

    public enum PartyMisc {
        S, // Special
        V, // VENG
        C, // Combat
        T, // Total
        P, // Poison
        D, // Disease
        W, // World
        U, // Username
        H, // HWID
        A, // ACCOUNT ID
        RANK // Rank
    }

    public DMSafePartyMiscChange(PartyMisc t, Integer v) {
        this.t = t;
        this.v = v;
        this.s = null;
    }

    public DMSafePartyMiscChange(PartyMisc t, String s) {
        this.t = t;
        this.v = null;
        this.s = s;
    }

    @Override
    public void process(PartyPlayer p) {
        try {
            switch (t) {
                case S:
                    p.getStats().setSpecialPercent(v);
                    break;
                case C:
                    p.getStats().setCombatLevel(v);
                    break;
                case T:
                    p.getStats().setTotalLevel(v);
                    break;
                case P:
                    p.setPoison(v);
                    break;
                case D:
                    p.setDisease(v);
                    break;
                case W:
                    p.setWorld(v);
                    break;
                case U:
                    p.setUsername(s);
                    break;
                case H:
                    p.setHardwareID(s);
                    break;
                case A:
                    p.setAccountID(s);
                    break;
                case V:
                    p.setVengActive(v);
                    break;
                case RANK:
                    p.setRank(s);
                    break;
                default:
                    log.warn("Unhandled misc change type for event: {}", this);
                    break;
            }
        } catch (Exception e) {
            log.error(new Date() + " PartyMiscChange Error: " + e.getMessage());
        }
    }
}