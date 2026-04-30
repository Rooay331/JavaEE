package ict.bean;

import java.io.Serializable;

public class NoShowSummaryRow implements Serializable {

    private String clinicName;
    private String serviceName;
    private String monthLabel;
    private int noShowCount;

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

    public String getMonthLabel() {
        return monthLabel;
    }

    public void setMonthLabel(String monthLabel) {
        this.monthLabel = monthLabel;
    }

    public int getNoShowCount() {
        return noShowCount;
    }

    public void setNoShowCount(int noShowCount) {
        this.noShowCount = noShowCount;
    }

    public String getDisplayLabel() {
        String clinicLabel = clinicName == null || clinicName.trim().isEmpty() ? "Unknown clinic" : clinicName.trim();
        String serviceLabel = serviceName == null || serviceName.trim().isEmpty() ? "Unknown service" : serviceName.trim();
        return clinicLabel + " / " + serviceLabel;
    }

    public String getNoShowLabel() {
        return noShowCount + (noShowCount == 1 ? " no-show" : " no-shows");
    }
}