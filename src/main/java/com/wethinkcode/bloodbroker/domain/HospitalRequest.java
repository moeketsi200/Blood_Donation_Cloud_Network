package com.wethinkcode.bloodbroker.domain;

public class HospitalRequest {
    private String bloodType;
    private int unitsRequired;
    private String hospitalId;
    private String timestamp;

    public HospitalRequest() {
        // Default constructor for frameworks
    }

    public HospitalRequest(String bloodType, int unitsRequired, String hospitalId) {
        this.bloodType = bloodType;
        this.unitsRequired = unitsRequired;
        this.hospitalId = hospitalId;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public int getUnitsRequired() {
        return unitsRequired;
    }

    public void setUnitsRequired(int unitsRequired) {
        this.unitsRequired = unitsRequired;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
