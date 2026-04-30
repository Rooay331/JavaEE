package ict.bean;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ClinicServiceStatus implements Serializable {

    private Integer clinicServiceId;
    private Integer clinicId;
    private String clinicName;
    private Integer serviceId;
    private String serviceName;
    private String serviceDescription;
    private boolean requiresApproval;
    private boolean walkInEnabled;
    private int avgServiceMinutes;
    private boolean active;
    private Integer defaultCapacity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getClinicServiceId() {
        return clinicServiceId;
    }

    public void setClinicServiceId(Integer clinicServiceId) {
        this.clinicServiceId = clinicServiceId;
    }

    public Integer getClinicId() {
        return clinicId;
    }

    public void setClinicId(Integer clinicId) {
        this.clinicId = clinicId;
    }

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
    }

    public Integer getServiceId() {
        return serviceId;
    }

    public void setServiceId(Integer serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public boolean isWalkInEnabled() {
        return walkInEnabled;
    }

    public void setWalkInEnabled(boolean walkInEnabled) {
        this.walkInEnabled = walkInEnabled;
    }

    public int getAvgServiceMinutes() {
        return avgServiceMinutes;
    }

    public void setAvgServiceMinutes(int avgServiceMinutes) {
        this.avgServiceMinutes = avgServiceMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getDefaultCapacity() {
        return defaultCapacity;
    }

    public void setDefaultCapacity(Integer defaultCapacity) {
        this.defaultCapacity = defaultCapacity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDisplayCode() {
        return clinicServiceId == null ? "" : "CS-" + clinicServiceId;
    }

    public String getClinicServiceLabel() {
        String clinic = clinicName == null ? "" : clinicName.trim();
        String service = serviceName == null ? "" : serviceName.trim();
        if (clinic.isEmpty()) {
            return service;
        }
        if (service.isEmpty()) {
            return clinic;
        }
        return clinic + " / " + service;
    }

    public String getStatusLabel() {
        if (!active) {
            return "SUSPENDED";
        }
        if (!walkInEnabled || requiresApproval) {
            return "LIMITED";
        }
        return "ACTIVE";
    }

    public String getStatusChipClass() {
        switch (getStatusLabel()) {
            case "ACTIVE":
                return "status-confirmed";
            case "LIMITED":
                return "status-medium";
            case "SUSPENDED":
                return "status-cancelled";
            default:
                return "pill-neutral";
        }
    }

    public String getCapacityLabel() {
        return defaultCapacity == null ? "-" : defaultCapacity.toString();
    }

    public String getApprovalLabel() {
        return requiresApproval ? "Yes" : "No";
    }

    public String getWalkInLabel() {
        return walkInEnabled ? "Enabled" : "Disabled";
    }
}