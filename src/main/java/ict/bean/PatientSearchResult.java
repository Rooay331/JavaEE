package ict.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PatientSearchResult implements Serializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d-M-yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d-M-yyyy H:mm");

    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private Integer activeAppointmentId;
    private String activeAppointmentStatus;
    private String activeAppointmentClinicName;
    private String activeAppointmentServiceName;
    private LocalDateTime activeAppointmentTime;
    private Integer queueTicketId;
    private Integer queueTicketNumber;
    private String queueStatus;
    private String queueClinicName;
    private String queueServiceName;
    private LocalDate queueDate;
    private Integer estimatedWaitMinutes;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getActiveAppointmentId() {
        return activeAppointmentId;
    }

    public void setActiveAppointmentId(Integer activeAppointmentId) {
        this.activeAppointmentId = activeAppointmentId;
    }

    public String getActiveAppointmentStatus() {
        return activeAppointmentStatus;
    }

    public void setActiveAppointmentStatus(String activeAppointmentStatus) {
        this.activeAppointmentStatus = activeAppointmentStatus;
    }

    public String getActiveAppointmentClinicName() {
        return activeAppointmentClinicName;
    }

    public void setActiveAppointmentClinicName(String activeAppointmentClinicName) {
        this.activeAppointmentClinicName = activeAppointmentClinicName;
    }

    public String getActiveAppointmentServiceName() {
        return activeAppointmentServiceName;
    }

    public void setActiveAppointmentServiceName(String activeAppointmentServiceName) {
        this.activeAppointmentServiceName = activeAppointmentServiceName;
    }

    public LocalDateTime getActiveAppointmentTime() {
        return activeAppointmentTime;
    }

    public void setActiveAppointmentTime(LocalDateTime activeAppointmentTime) {
        this.activeAppointmentTime = activeAppointmentTime;
    }

    public Integer getQueueTicketId() {
        return queueTicketId;
    }

    public void setQueueTicketId(Integer queueTicketId) {
        this.queueTicketId = queueTicketId;
    }

    public Integer getQueueTicketNumber() {
        return queueTicketNumber;
    }

    public void setQueueTicketNumber(Integer queueTicketNumber) {
        this.queueTicketNumber = queueTicketNumber;
    }

    public String getQueueStatus() {
        return queueStatus;
    }

    public void setQueueStatus(String queueStatus) {
        this.queueStatus = queueStatus;
    }

    public String getQueueClinicName() {
        return queueClinicName;
    }

    public void setQueueClinicName(String queueClinicName) {
        this.queueClinicName = queueClinicName;
    }

    public String getQueueServiceName() {
        return queueServiceName;
    }

    public void setQueueServiceName(String queueServiceName) {
        this.queueServiceName = queueServiceName;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public void setQueueDate(LocalDate queueDate) {
        this.queueDate = queueDate;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public String getPatientCode() {
        return userId == null ? "" : "P-" + userId;
    }

    public String getDobLabel() {
        return dateOfBirth == null ? "-" : dateOfBirth.format(DATE_FORMATTER);
    }

    public String getActiveAppointmentLabel() {
        String clinic = activeAppointmentClinicName == null ? "" : activeAppointmentClinicName.trim();
        String service = activeAppointmentServiceName == null ? "" : activeAppointmentServiceName.trim();
        String time = activeAppointmentTime == null ? "" : activeAppointmentTime.format(DATE_TIME_FORMATTER);
        String value = clinic;
        if (!service.isEmpty()) {
            value = value.isEmpty() ? service : value + " / " + service;
        }
        if (!time.isEmpty()) {
            value = value.isEmpty() ? time : value + " @ " + time;
        }
        return value.isEmpty() ? "-" : value;
    }

    public String getQueueLabel() {
        if (queueTicketNumber == null) {
            return "-";
        }
        return "Q-" + queueTicketNumber;
    }

    public String getQueueContextLabel() {
        String clinic = queueClinicName == null ? "" : queueClinicName.trim();
        String service = queueServiceName == null ? "" : queueServiceName.trim();
        if (clinic.isEmpty()) {
            return service.isEmpty() ? "-" : service;
        }
        if (service.isEmpty()) {
            return clinic;
        }
        return clinic + " / " + service;
    }

    public String getQueueDateLabel() {
        return queueDate == null ? "-" : queueDate.format(DATE_FORMATTER);
    }

    public String getAppointmentStatusChipClass() {
        if (activeAppointmentStatus == null) {
            return "pill-neutral";
        }
        switch (activeAppointmentStatus) {
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

    public String getQueueStatusChipClass() {
        if (queueStatus == null) {
            return "pill-neutral";
        }
        switch (queueStatus) {
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

    public String getEstimatedWaitLabel() {
        return estimatedWaitMinutes == null ? "-" : estimatedWaitMinutes + " mins";
    }
}