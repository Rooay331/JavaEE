package ict.bean;

import java.io.Serializable;

public class ServiceUtilization implements Serializable {

    private String clinicName;
    private String serviceName;
    private int capacity;
    private int booked;
    private int utilisationPercent;

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getBooked() {
        return booked;
    }

    public void setBooked(int booked) {
        this.booked = booked;
    }

    public int getUtilisationPercent() {
        return utilisationPercent;
    }

    public void setUtilisationPercent(int utilisationPercent) {
        this.utilisationPercent = utilisationPercent;
    }

    public String getUtilisationLabel() {
        return utilisationPercent + "%";
    }
}