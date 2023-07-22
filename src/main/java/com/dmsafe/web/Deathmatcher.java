package com.dmsafe.web;

public class Deathmatcher {
    private final String accountID;
    private final String hardwareID;
    private final String currentRSN;
    private final String rank;
    private final String information;

    public Deathmatcher(String accountID, String hardwareID, String currentRSN, String rank, String information) {
        this.accountID = accountID;
        this.hardwareID = hardwareID;
        this.currentRSN = currentRSN;
        this.rank = rank;
        this.information = information;
    }

    @Override
    public String toString()
    {
        return "RSN: " + currentRSN + "; rank : " + rank;
    }
    public String getAccountID() {
        return accountID;
    }

    public String getHWID() {
        return hardwareID;
    }

    public String getRSN() {
        return currentRSN;
    }

    public String getRank() {
        return rank;
    }

    public String getInformation() {
        return information;
    }
}
