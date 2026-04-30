package ict.db;

import ict.bean.Appointment;
import ict.bean.PatientSearchResult;
import ict.bean.QueueTicket;
import ict.bean.User;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PatientDB {

    private final String url;
    private final String username;
    private final String password;

    public PatientDB(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
        }
        return DriverManager.getConnection(url, username, password);
    }

    public List<PatientSearchResult> searchPatients(Integer clinicId, String keyword, Integer patientId, String statusFilter)
            throws SQLException {
        List<PatientSearchResult> results = new ArrayList<>();
        if (clinicId == null) {
            return results;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT user_id, full_name, email, phone, date_of_birth, gender ");
        sql.append("FROM users ");
        sql.append("WHERE role = 'PATIENT' AND is_active = 1 ");
        if (patientId != null) {
            sql.append("AND user_id = ? ");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (full_name LIKE ? OR email LIKE ? OR phone LIKE ?) ");
        }
        sql.append("ORDER BY full_name");

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            if (patientId != null) {
                statement.setInt(parameterIndex++, patientId);
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim() + "%";
                statement.setString(parameterIndex++, pattern);
                statement.setString(parameterIndex++, pattern);
                statement.setString(parameterIndex++, pattern);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    PatientSearchResult result = new PatientSearchResult();
                    int userId = rs.getInt("user_id");
                    result.setUserId(userId);
                    result.setFullName(rs.getString("full_name"));
                    result.setEmail(rs.getString("email"));
                    result.setPhone(rs.getString("phone"));

                    Date dob = rs.getDate("date_of_birth");
                    if (dob != null) {
                        result.setDateOfBirth(dob.toLocalDate());
                    }
                    result.setGender(rs.getString("gender"));

                    Appointment activeAppointment = findLatestActiveAppointment(connection, clinicId, userId);
                    if (activeAppointment != null) {
                        result.setActiveAppointmentId(activeAppointment.getAppointmentId());
                        result.setActiveAppointmentStatus(activeAppointment.getStatus());
                        result.setActiveAppointmentClinicName(activeAppointment.getClinicName());
                        result.setActiveAppointmentServiceName(activeAppointment.getServiceName());
                        if (activeAppointment.getSlotDate() != null && activeAppointment.getStartTime() != null) {
                            result.setActiveAppointmentTime(LocalDateTime.of(activeAppointment.getSlotDate(), activeAppointment.getStartTime()));
                        }
                    }

                    QueueTicket queueTicket = findActiveQueueTicket(connection, clinicId, userId);
                    if (queueTicket != null) {
                        result.setQueueTicketId(queueTicket.getTicketId());
                        result.setQueueTicketNumber(queueTicket.getTicketNumber());
                        result.setQueueStatus(queueTicket.getStatus());
                        result.setQueueClinicName(queueTicket.getClinicName());
                        result.setQueueServiceName(queueTicket.getServiceName());
                        result.setQueueDate(queueTicket.getQueueDate());
                        result.setEstimatedWaitMinutes(queueTicket.getEstimatedWaitMinutes());
                    }

                    if (matchesStatusFilter(result, statusFilter)) {
                        results.add(result);
                    }
                }
            }
        }

        return results;
    }

    public User findPatientById(Integer patientId) throws SQLException {
        if (patientId == null) {
            return null;
        }

        String sql = "SELECT user_id, role, clinic_id, full_name, email, phone, date_of_birth, gender, password, is_active, last_login_at, created_at, updated_at "
                + "FROM users WHERE user_id = ? AND role = 'PATIENT' AND is_active = 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUser(rs);
            }
        }
    }

    public QueueTicket findActiveQueueTicket(Integer clinicId, Integer patientId) throws SQLException {
        if (clinicId == null || patientId == null) {
            return null;
        }
        try (Connection connection = getConnection()) {
            return findActiveQueueTicket(connection, clinicId, patientId);
        }
    }

    public QueueTicket findActiveQueueTicket(Integer patientId) throws SQLException {
        if (patientId == null) {
            return null;
        }

        String sql = "SELECT qt.ticket_id, qt.clinic_service_id, cs.clinic_id, c.clinic_name, cs.service_id, s.service_name, "
                + "qt.patient_user_id, u.full_name AS patient_name, qt.queue_date, qt.ticket_number, qt.status, qt.estimated_wait_minutes, "
                + "qt.called_at, qt.skipped_at, qt.served_at, qt.expired_at, qt.cancelled_at, qt.created_at, qt.updated_at "
                + "FROM queue_tickets qt "
                + "INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                + "INNER JOIN clinics c ON cs.clinic_id = c.clinic_id "
                + "INNER JOIN services s ON cs.service_id = s.service_id "
                + "INNER JOIN users u ON qt.patient_user_id = u.user_id "
                + "WHERE qt.patient_user_id = ? AND qt.status IN ('WAITING', 'CALLED') "
                + "ORDER BY qt.queue_date DESC, qt.created_at DESC LIMIT 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapQueueTicket(rs);
                }
            }
        }

        return null;
    }

    public boolean updatePatientPhone(Integer patientId, String phone) throws SQLException {
        if (patientId == null) {
            return false;
        }

        String normalizedPhone = phone == null ? null : phone.trim();
        if (normalizedPhone != null && normalizedPhone.isEmpty()) {
            normalizedPhone = null;
        }

        String sql = "UPDATE users SET phone = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ? AND role = 'PATIENT' AND is_active = 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (normalizedPhone == null) {
                statement.setNull(1, Types.VARCHAR);
            } else {
                statement.setString(1, normalizedPhone);
            }
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;
        }
    }

    public QueueTicket findAnyActiveQueueTicketByPatient(Integer patientId) throws SQLException {
        return findActiveQueueTicket(patientId);
    }

    private QueueTicket findActiveQueueTicket(Connection connection, Integer clinicId, Integer patientId) throws SQLException {
        String sql = "SELECT qt.ticket_id, qt.clinic_service_id, cs.clinic_id, c.clinic_name, cs.service_id, s.service_name, "
                + "qt.patient_user_id, u.full_name AS patient_name, qt.queue_date, qt.ticket_number, qt.status, qt.estimated_wait_minutes, "
                + "qt.called_at, qt.skipped_at, qt.served_at, qt.expired_at, qt.cancelled_at, qt.created_at, qt.updated_at "
                + "FROM queue_tickets qt "
                + "INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                + "INNER JOIN clinics c ON cs.clinic_id = c.clinic_id "
                + "INNER JOIN services s ON cs.service_id = s.service_id "
                + "INNER JOIN users u ON qt.patient_user_id = u.user_id "
                + "WHERE cs.clinic_id = ? AND qt.patient_user_id = ? AND qt.status IN ('WAITING', 'CALLED') "
                + "ORDER BY qt.queue_date DESC, qt.created_at DESC LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicId);
            statement.setInt(2, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapQueueTicket(rs);
                }
            }
        }

        return null;
    }

    private Appointment findLatestActiveAppointment(Connection connection, Integer clinicId, Integer patientId) throws SQLException {
        String sql = "SELECT a.appointment_id, u.full_name AS patient_name, c.clinic_name, s.service_name, a.appointment_date AS slot_date, a.start_time, a.end_time, "
                + "a.status, ru.full_name AS reviewed_by_name, a.reviewed_at, a.review_reason, a.checked_in_at, a.completed_at, a.cancelled_at, "
                + "a.cancellation_reason, a.outcome_notes "
                + "FROM appointments a "
                + "INNER JOIN users u ON a.patient_user_id = u.user_id "
                + "INNER JOIN clinics c ON a.clinic_id = c.clinic_id "
                + "INNER JOIN services s ON a.service_id = s.service_id "
                + "LEFT JOIN users ru ON a.reviewed_by_user_id = ru.user_id "
                + "WHERE a.patient_user_id = ? AND a.clinic_id = ? AND a.status IN ('PENDING', 'CONFIRMED', 'ARRIVED') "
            + "ORDER BY a.appointment_date DESC, a.start_time DESC, a.appointment_id DESC LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setInt(2, clinicId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapAppointment(rs);
                }
            }
        }

        return null;
    }

    private boolean matchesStatusFilter(PatientSearchResult result, String statusFilter) {
        if (statusFilter == null || statusFilter.trim().isEmpty()) {
            return true;
        }

        String normalized = statusFilter.trim();
        if ("Has Active Appointment".equalsIgnoreCase(normalized)) {
            return result.getActiveAppointmentStatus() != null;
        }
        if ("In Queue".equalsIgnoreCase(normalized)) {
            return result.getQueueStatus() != null;
        }
        if ("No Active Booking".equalsIgnoreCase(normalized)) {
            return result.getActiveAppointmentStatus() == null && result.getQueueStatus() == null;
        }

        return true;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User bean = new User();
        bean.setUserId(rs.getInt("user_id"));
        bean.setRole(rs.getString("role"));

        int clinicId = rs.getInt("clinic_id");
        if (!rs.wasNull()) {
            bean.setClinicId(clinicId);
        }

        bean.setFullName(rs.getString("full_name"));
        bean.setEmail(rs.getString("email"));
        bean.setPhone(rs.getString("phone"));

        Date dob = rs.getDate("date_of_birth");
        if (dob != null) {
            bean.setDateOfBirth(dob.toLocalDate());
        }

        bean.setGender(rs.getString("gender"));
        bean.setPassword(rs.getString("password"));
        bean.setIsActive(rs.getInt("is_active"));

        Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
        if (lastLoginAt != null) {
            bean.setLastLoginAt(lastLoginAt.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            bean.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            bean.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return bean;
    }

    private Appointment mapAppointment(ResultSet rs) throws SQLException {
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(rs.getInt("appointment_id"));
        appointment.setPatientName(rs.getString("patient_name"));
        appointment.setClinicName(rs.getString("clinic_name"));
        appointment.setServiceName(rs.getString("service_name"));

        Date slotDate = rs.getDate("slot_date");
        if (slotDate != null) {
            appointment.setSlotDate(slotDate.toLocalDate());
        }

        java.sql.Time startTime = rs.getTime("start_time");
        if (startTime != null) {
            appointment.setStartTime(startTime.toLocalTime());
        }

        java.sql.Time endTime = rs.getTime("end_time");
        if (endTime != null) {
            appointment.setEndTime(endTime.toLocalTime());
        }

        appointment.setStatus(rs.getString("status"));
        appointment.setReviewedBy(rs.getString("reviewed_by_name"));

        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        if (reviewedAt != null) {
            appointment.setReviewedAt(reviewedAt.toLocalDateTime());
        }

        appointment.setReviewReason(rs.getString("review_reason"));

        Timestamp checkedInAt = rs.getTimestamp("checked_in_at");
        if (checkedInAt != null) {
            appointment.setCheckedInAt(checkedInAt.toLocalDateTime());
        }

        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            appointment.setCompletedAt(completedAt.toLocalDateTime());
        }

        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
        if (cancelledAt != null) {
            appointment.setCancelledAt(cancelledAt.toLocalDateTime());
        }

        appointment.setCancellationReason(rs.getString("cancellation_reason"));
        appointment.setOutcomeNotes(rs.getString("outcome_notes"));
        appointment.setActionSummary(appointment.getSuggestedUpdateAction());
        return appointment;
    }

    private QueueTicket mapQueueTicket(ResultSet rs) throws SQLException {
        QueueTicket ticket = new QueueTicket();
        ticket.setTicketId(rs.getInt("ticket_id"));
        ticket.setClinicServiceId(rs.getInt("clinic_service_id"));

        int clinicId = rs.getInt("clinic_id");
        if (!rs.wasNull()) {
            ticket.setClinicId(clinicId);
        }
        ticket.setClinicName(rs.getString("clinic_name"));

        int serviceId = rs.getInt("service_id");
        if (!rs.wasNull()) {
            ticket.setServiceId(serviceId);
        }
        ticket.setServiceName(rs.getString("service_name"));

        int patientUserId = rs.getInt("patient_user_id");
        if (!rs.wasNull()) {
            ticket.setPatientUserId(patientUserId);
        }
        ticket.setPatientName(rs.getString("patient_name"));

        Date queueDate = rs.getDate("queue_date");
        if (queueDate != null) {
            ticket.setQueueDate(queueDate.toLocalDate());
        }

        int ticketNumber = rs.getInt("ticket_number");
        if (!rs.wasNull()) {
            ticket.setTicketNumber(ticketNumber);
        }
        ticket.setStatus(rs.getString("status"));

        int estimatedWaitMinutes = rs.getInt("estimated_wait_minutes");
        if (!rs.wasNull()) {
            ticket.setEstimatedWaitMinutes(estimatedWaitMinutes);
        }

        Timestamp calledAt = rs.getTimestamp("called_at");
        if (calledAt != null) {
            ticket.setCalledAt(calledAt.toLocalDateTime());
        }

        Timestamp skippedAt = rs.getTimestamp("skipped_at");
        if (skippedAt != null) {
            ticket.setSkippedAt(skippedAt.toLocalDateTime());
        }

        Timestamp servedAt = rs.getTimestamp("served_at");
        if (servedAt != null) {
            ticket.setServedAt(servedAt.toLocalDateTime());
        }

        Timestamp expiredAt = rs.getTimestamp("expired_at");
        if (expiredAt != null) {
            ticket.setExpiredAt(expiredAt.toLocalDateTime());
        }

        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
        if (cancelledAt != null) {
            ticket.setCancelledAt(cancelledAt.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            ticket.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            ticket.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return ticket;
    }
}