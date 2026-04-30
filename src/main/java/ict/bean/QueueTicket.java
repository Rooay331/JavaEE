package ict.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class QueueTicket implements Serializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d-M-yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d-M-yyyy H:mm");

    private Integer ticketId;
    private Integer clinicServiceId;
    private Integer clinicId;
    private String clinicName;
    private Integer serviceId;
    private String serviceName;
    private Integer patientUserId;
    private String patientName;
    private LocalDate queueDate;
    private Integer ticketNumber;
    private String status;
    private Integer estimatedWaitMinutes;
    private LocalDateTime calledAt;
    private LocalDateTime skippedAt;
    private LocalDateTime servedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getTicketId() {
        return ticketId;
    }

    public void setTicketId(Integer ticketId) {
        this.ticketId = ticketId;
    }

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

    public Integer getPatientUserId() {
        return patientUserId;
    }

    public void setPatientUserId(Integer patientUserId) {
        this.patientUserId = patientUserId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(LocalDate queueDate) {
        this.queueDate = queueDate;
    }

    public Integer getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(Integer ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(LocalDateTime calledAt) {
        this.calledAt = calledAt;
    }

    public LocalDateTime getSkippedAt() {
        return skippedAt;
    }

    public void setSkippedAt(LocalDateTime skippedAt) {
        this.skippedAt = skippedAt;
    }

    public LocalDateTime getServedAt() {
        return servedAt;
    }

    public void setServedAt(LocalDateTime servedAt) {
        this.servedAt = servedAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
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
        if (ticketNumber != null) {
            return "Q-" + ticketNumber;
        }
        return ticketId == null ? "" : "Q-" + ticketId;
    }

    public String getQueueLabel() {
        if (queueDate == null) {
            return "";
        }
        return queueDate.format(DATE_FORMATTER);
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

    public String getIssuedAtLabel() {
        if (createdAt != null) {
            return createdAt.format(DATE_TIME_FORMATTER);
        }
        return getQueueLabel();
    }

    public String getStateTimestampLabel() {
        if (calledAt != null) {
            return calledAt.format(DATE_TIME_FORMATTER);
        }
        if (servedAt != null) {
            return servedAt.format(DATE_TIME_FORMATTER);
        }
        if (skippedAt != null) {
            return skippedAt.format(DATE_TIME_FORMATTER);
        }
        if (expiredAt != null) {
            return expiredAt.format(DATE_TIME_FORMATTER);
        }
        if (cancelledAt != null) {
            return cancelledAt.format(DATE_TIME_FORMATTER);
        }
        return "";
    }

    public String getEstimatedWaitLabel() {
        return estimatedWaitMinutes == null ? "-" : estimatedWaitMinutes + " mins";
    }

    public String getStatusChipClass() {
        if (status == null) {
            return "pill-neutral";
        }
        switch (status) {
            case "WAITING":
                return "status-waiting";
            case "CALLED":
                return "status-called";
            case "SKIPPED":
                return "status-skipped";
            case "SERVED":
                return "status-served";
            case "EXPIRED":
            case "CANCELLED":
                return "status-cancelled";
            default:
                return "pill-neutral";
        }
    }
}