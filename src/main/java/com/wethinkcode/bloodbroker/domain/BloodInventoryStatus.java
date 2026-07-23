package com.wethinkcode.bloodbroker.domain;

public class BloodInventoryStatus {
    private String bankName;
    private int unitsAvailable;
    private boolean emergencyShortage;

    /** No-arg constructor — used by tests and the router aggregator. */
    public BloodInventoryStatus() {
        this.bankName = "Aggregated";
        this.unitsAvailable = 0;
        this.emergencyShortage = false;
    }

    public BloodInventoryStatus(String bankName, int unitsAvailable, boolean emergencyShortage) {
        this.bankName = bankName;
        this.unitsAvailable = unitsAvailable;
        this.emergencyShortage = emergencyShortage;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public int getUnitsAvailable() {
        return unitsAvailable;
    }

    public void setUnitsAvailable(int unitsAvailable) {
        this.unitsAvailable = unitsAvailable;
    }

    public boolean isEmergencyShortage() {
        return this.emergencyShortage;
    }

    public void setEmergencyShortage(boolean emergencyShortage) {
        this.emergencyShortage = emergencyShortage;
    }
}
