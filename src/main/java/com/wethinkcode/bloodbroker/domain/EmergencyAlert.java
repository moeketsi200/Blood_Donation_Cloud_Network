package com.wethinkcode.bloodbroker.domain;

public class EmergencyAlert {
    private String bloodType;
    private String hospitalId;
    private String timestamp;
    private int unitsRequired;

    public EmergencyAlert(String bloodType,String hospitalId,String timestamp,int unitsRequired){
        this.bloodType = bloodType;
        this.hospitalId = hospitalId;
        this.timestamp = timestamp;
        this.unitsRequired = unitsRequired;
    }

    public String getBloodType() {
        return bloodType;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getUnitsRequired() {
        return unitsRequired;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setUnitsRequired(int unitsRequired) {
        this.unitsRequired = unitsRequired;
    }

    
}
