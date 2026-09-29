package com.wethinkcode.bloodbroker.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class BloodInventoryStatus {
    private String bankName;
    private String bloodType;
    private int unitsAvailable;
    private boolean emergencyShortage;

    /** No-arg constructor — used by tests and the router aggregator. */
    public BloodInventoryStatus() {
        this.bankName = "Aggregated";
        this.bloodType = "UNKNOWN";
        this.unitsAvailable = 0;
        this.emergencyShortage = false;
    }

    public BloodInventoryStatus(String bankName, String bloodType, int unitsAvailable, boolean emergencyShortage) {
        this.bankName = bankName;
        this.bloodType = bloodType;
        this.unitsAvailable = unitsAvailable;
        this.emergencyShortage = emergencyShortage;
    }

    @DynamoDbPartitionKey
    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    @DynamoDbSortKey
    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public int getUnitsAvailable() {
        return unitsAvailable;
    }

    public void setUnitsAvailable(int unitsAvailable) {
        this.unitsAvailable = unitsAvailable;
    }

    public boolean isEmergencyShortage() {
        return this.unitsAvailable == 0;
    }

    public void setEmergencyShortage(boolean emergencyShortage) {
        this.emergencyShortage = emergencyShortage;
    }
}
