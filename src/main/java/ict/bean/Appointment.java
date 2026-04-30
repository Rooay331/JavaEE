package ict.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Appointment implements Serializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d-M-yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    private Integer appointmentId;
    private String patientName;
    private String clinicName;
    private String serviceName;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewReason;
    private LocalDateTime checkedInAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private String outcomeNotes;
    private String actionSummary;

    public Appointment() {
    }

    public Appointment(Integer appointmentId, String patientName, String clinicName, String serviceName,
            LocalDate slotDate, LocalTime startTime, LocalTime endTime, String status, String actionSummary) {
        this.appointmentId = appointmentId;
        this.patientName = patientName;
        this.clinicName = clinicName;
        this.serviceName = serviceName;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.actionSummary = actionSummary;
    }

    public Integer getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

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

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public void setReviewReason(String reviewReason) {
        this.reviewReason = reviewReason;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getOutcomeNotes() {
        return outcomeNotes;
    }

    public void setOutcomeNotes(String outcomeNotes) {
        this.outcomeNotes = outcomeNotes;
    }

    public String getActionSummary() {
        return actionSummary;
    }

    public void setActionSummary(String actionSummary) {
        this.actionSummary = actionSummary;
    }

    public String getDisplayCode() {
        return appointmentId == null ? "" : "A-" + appointmentId;
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

    public String getScheduleLabel() {
        if (slotDate == null || startTime == null || endTime == null) {
            return "";
        }
        return slotDate.format(DATE_FORMATTER) + " " + startTime.format(TIME_FORMATTER) + " - " + endTime.format(TIME_FORMATTER);
    }

    public String getDisplayDateLabel() {
        return slotDate == null ? "" : slotDate.format(DATE_FORMATTER);
    }

    public String getTimeRangeLabel() {
        if (startTime == null || endTime == null) {
            return "";
        }
        return startTime.format(TIME_FORMATTER) + " - " + endTime.format(TIME_FORMATTER);
    }

    public String getServiceLabel() {
        return serviceName == null ? "" : serviceName;
    }

    public String getSuggestedUpdateAction() {
        if (status == null) {
            return "CHECK_IN";
        }

        switch (status) {
            case "PENDING":
                return "APPROVE";
            case "REJECTED_BY_CLINIC":
                return "REJECT_BY_CLINIC";
            case "ARRIVED":
                return "COMPLETE";
            case "COMPLETED":
                return "COMPLETE";
            case "NO_SHOW":
                return "NO_SHOW";
            case "CANCELLED_BY_PATIENT":
            case "CANCELLED_BY_CLINIC":
                return "CANCEL_BY_CLINIC";
            default:
                return "CHECK_IN";
        }
    }

    public String getStatusChipClass() {
        if (status == null) {
            return "status-pending";
        }
        switch (status) {
            case "CONFIRMED":
                return "status-confirmed";
            case "ARRIVED":
                return "status-arrived";
            case "COMPLETED":
                return "status-completed";
            case "NO_SHOW":
                return "status-no-show";
            case "REJECTED_BY_CLINIC":
            case "CANCELLED_BY_PATIENT":
            case "CANCELLED_BY_CLINIC":
                return "status-cancelled";
            default:
                return "status-pending";
        }
    }

    public boolean matches(String clinicFilter, String serviceFilter, String statusFilter, LocalDate dateFilter) {
        return matchesText(clinicName, clinicFilter)
                && matchesText(serviceName, serviceFilter)
                && matchesText(status, statusFilter)
                && (dateFilter == null || dateFilter.equals(slotDate));
    }

    private boolean matchesText(String value, String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(filter.trim().toLowerCase(Locale.ROOT));
    }
}