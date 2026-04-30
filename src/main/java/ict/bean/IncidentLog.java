package ict.bean;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IncidentLog implements Serializable {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d-M-yyyy H:mm");

    private Integer incidentId;
    private Integer reportedByUserId;
    private String reporterName;
    private Integer clinicId;
    private String clinicName;
    private Integer serviceId;
    private String serviceName;
    private String severity;
    private String status;
    private String title;
    private String description;
    private LocalDateTime occurredAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Integer incidentId) {
        this.incidentId = incidentId;
    }

    public Integer getReportedByUserId() {
        return reportedByUserId;
    }

    public void setReportedByUserId(Integer reportedByUserId) {
        this.reportedByUserId = reportedByUserId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
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

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
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
        return incidentId == null ? "" : "I-" + incidentId;
    }

    public String getSeverityChipClass() {
        if (severity == null) {
            return "pill-neutral";
        }
        switch (severity) {
            case "CRITICAL":
            case "HIGH":
                return "status-critical";
            case "MEDIUM":
                return "status-medium";
            case "LOW":
                return "status-low";
            default:
                return "pill-neutral";
        }
    }

    public String getStatusChipClass() {
        if (status == null) {
            return "pill-neutral";
        }
        switch (status) {
            case "OPEN":
                return "status-pending";
            case "IN_PROGRESS":
                return "status-in-progress";
            case "RESOLVED":
                return "status-completed";
            case "CLOSED":
                return "pill-neutral";
            default:
                return "pill-neutral";
        }
    }

    public String getOccurredAtLabel() {
        return occurredAt == null ? "" : occurredAt.format(DATE_TIME_FORMATTER);
    }

    public String getResolvedAtLabel() {
        return resolvedAt == null ? "" : resolvedAt.format(DATE_TIME_FORMATTER);
    }

    public String getContextLabel() {
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

    public String getReporterLabel() {
        return reporterName == null || reporterName.trim().isEmpty() ? "Unknown staff" : reporterName;
    }
}