package ict.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AppointmentBookingSlot implements Serializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d-M-yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    private Integer clinicServiceId;
    private Integer clinicId;
    private String clinicName;
    private Integer serviceId;
    private String serviceName;
    private String serviceDescription;
    private boolean requiresApproval;
    private boolean walkInEnabled;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int capacity;
    private int bookedCount;
    private String slotStatus;

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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(int bookedCount) {
        this.bookedCount = bookedCount;
    }

    public String getSlotStatus() {
        return slotStatus;
    }

    public void setSlotStatus(String slotStatus) {
        this.slotStatus = slotStatus;
    }

    public String getDisplayCode() {
        String schedule = getScheduleLabel();
        if (!schedule.isEmpty()) {
            return schedule;
        }

        return getClinicServiceLabel();
    }

    public int getRemainingCount() {
        return Math.max(capacity - bookedCount, 0);
    }

    public boolean isAvailable() {
        return "OPEN".equalsIgnoreCase(slotStatus) && getRemainingCount() > 0;
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

    public String getDateLabel() {
        return slotDate == null ? "" : slotDate.format(DATE_FORMATTER);
    }

    public String getTimeRangeLabel() {
        if (startTime == null || endTime == null) {
            return "";
        }
        return startTime.format(TIME_FORMATTER) + " - " + endTime.format(TIME_FORMATTER);
    }

    public String getScheduleLabel() {
        if (slotDate == null) {
            return getTimeRangeLabel();
        }
        if (startTime == null || endTime == null) {
            return getDateLabel();
        }
        return getDateLabel() + " " + getTimeRangeLabel();
    }

    public String getCapacitySummary() {
        return bookedCount + "/" + capacity;
    }

    public String getAvailabilityLabel() {
        if (!"OPEN".equalsIgnoreCase(slotStatus)) {
            return "Closed";
        }
        int remainingCount = getRemainingCount();
        if (remainingCount <= 0) {
            return "Full";
        }
        return remainingCount + " left";
    }

    public String getAvailabilityChipClass() {
        return isAvailable() ? "status-open" : "status-cancelled";
    }

    public String getApprovalLabel() {
        return requiresApproval ? "Approval required" : "Instant confirmation";
    }

    public String getWalkInLabel() {
        return walkInEnabled ? "Walk-in enabled" : "Appointment only";
    }
}