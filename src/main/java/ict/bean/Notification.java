package ict.bean;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Notification implements Serializable {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d-M-yyyy H:mm");

    private Integer notificationId;
    private Integer recipientUserId;
    private String recipientName;
    private Integer clinicId;
    private String clinicName;
    private String type;
    private String title;
    private String body;
    private Integer relatedAppointmentId;
    private Integer relatedTicketId;
    private Integer relatedIncidentId;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public Integer getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Integer recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Integer getRelatedAppointmentId() {
        return relatedAppointmentId;
    }

    public void setRelatedAppointmentId(Integer relatedAppointmentId) {
        this.relatedAppointmentId = relatedAppointmentId;
    }

    public Integer getRelatedTicketId() {
        return relatedTicketId;
    }

    public void setRelatedTicketId(Integer relatedTicketId) {
        this.relatedTicketId = relatedTicketId;
    }

    public Integer getRelatedIncidentId() {
        return relatedIncidentId;
    }

    public void setRelatedIncidentId(Integer relatedIncidentId) {
        this.relatedIncidentId = relatedIncidentId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDisplayCode() {
        return notificationId == null ? "" : "N-" + notificationId;
    }

    public String getTypeLabel() {
        if (type == null || type.trim().isEmpty()) {
            return "Notification";
        }

        String[] parts = type.toLowerCase().split("_");
        StringBuilder label = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return label.toString();
    }

    public String getCreatedAtLabel() {
        return createdAt == null ? "" : createdAt.format(DATE_TIME_FORMATTER);
    }

    public String getReadAtLabel() {
        return readAt == null ? "" : readAt.format(DATE_TIME_FORMATTER);
    }

    public String getReadStateLabel() {
        return read ? "Read" : "Unread";
    }

    public String getReadStateClass() {
        return read ? "pill-neutral" : "status-pending";
    }

    public String getTypeBadgeClass() {
        if (type == null) {
            return "pill-neutral";
        }

        switch (type) {
            case "APPOINTMENT_CONFIRMED":
                return "status-confirmed";
            case "APPOINTMENT_REJECTED":
            case "APPOINTMENT_CANCELLED":
                return "status-cancelled";
            case "APPOINTMENT_UPDATED":
                return "status-low";
            case "APPOINTMENT_REMINDER":
                return "status-medium";
            case "SERVICE_SUSPENDED":
            case "INCIDENT_REPORTED":
            case "ACCOUNT_ALERT":
                return "status-critical";
            case "NO_SHOW_ALERT":
            case "PENDING_APPROVAL":
                return "status-medium";
            case "QUEUE_CALLED":
            case "QUEUE_SKIPPED":
                return "status-low";
            case "QUEUE_EXPIRED":
                return "status-cancelled";
            default:
                return "pill-neutral";
        }
    }

    public String getRelatedContextLabel() {
        List<String> labels = new ArrayList<>();
        if (relatedAppointmentId != null) {
            labels.add("Appointment A-" + relatedAppointmentId);
        }
        if (relatedTicketId != null) {
            labels.add("Queue Ticket #" + relatedTicketId);
        }
        if (relatedIncidentId != null) {
            labels.add("Incident #" + relatedIncidentId);
        }
        return String.join(" | ", labels);
    }
}