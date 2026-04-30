package ict.db;

import ict.bean.QueueTicket;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class QueueDB {

    private final String url;
    private final String username;
    private final String password;

    public QueueDB(String url, String username, String password) {
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

    public List<QueueTicket> findQueueTicketsByClinic(Integer clinicId, String serviceName, String status, String patientName, LocalDate queueDate)
            throws SQLException {
        List<QueueTicket> queueTickets = new ArrayList<>();
        if (clinicId == null) {
            return queueTickets;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT qt.ticket_id, qt.clinic_service_id, cs.clinic_id, c.clinic_name, cs.service_id, s.service_name, ");
        sql.append("qt.patient_user_id, u.full_name AS patient_name, qt.queue_date, qt.ticket_number, qt.status, ");
        sql.append("qt.estimated_wait_minutes, qt.called_at, qt.skipped_at, qt.served_at, qt.expired_at, qt.cancelled_at, ");
        sql.append("qt.created_at, qt.updated_at ");
        sql.append("FROM queue_tickets qt ");
        sql.append("INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id ");
        sql.append("INNER JOIN clinics c ON cs.clinic_id = c.clinic_id ");
        sql.append("INNER JOIN services s ON cs.service_id = s.service_id ");
        sql.append("INNER JOIN users u ON qt.patient_user_id = u.user_id ");
        sql.append("WHERE cs.clinic_id = ? ");

        if (serviceName != null && !serviceName.trim().isEmpty()) {
            sql.append("AND s.service_name = ? ");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND qt.status = ? ");
        }
        if (patientName != null && !patientName.trim().isEmpty()) {
            sql.append("AND LOWER(u.full_name) LIKE ? ");
        }
        if (queueDate != null) {
            sql.append("AND qt.queue_date = ? ");
        }

        sql.append("ORDER BY qt.queue_date DESC, FIELD(qt.status, 'CALLED', 'WAITING', 'SKIPPED', 'SERVED', 'EXPIRED', 'CANCELLED'), qt.ticket_number ASC");

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            statement.setInt(parameterIndex++, clinicId);
            if (serviceName != null && !serviceName.trim().isEmpty()) {
                statement.setString(parameterIndex++, serviceName.trim());
            }
            if (status != null && !status.trim().isEmpty()) {
                statement.setString(parameterIndex++, status.trim());
            }
            if (patientName != null && !patientName.trim().isEmpty()) {
                statement.setString(parameterIndex++, "%" + patientName.trim().toLowerCase() + "%");
            }
            if (queueDate != null) {
                statement.setDate(parameterIndex++, Date.valueOf(queueDate));
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    queueTickets.add(mapQueueTicket(rs));
                }
            }
        }

        return queueTickets;
    }

    public QueueTicket findActiveQueueTicketByClinicAndPatient(Integer clinicId, Integer patientUserId, LocalDate queueDate)
            throws SQLException {
        if (clinicId == null || patientUserId == null) {
            return null;
        }

        String sql = "SELECT qt.ticket_id, qt.clinic_service_id, cs.clinic_id, c.clinic_name, cs.service_id, s.service_name, "
                + "qt.patient_user_id, u.full_name AS patient_name, qt.queue_date, qt.ticket_number, qt.status, "
                + "qt.estimated_wait_minutes, qt.called_at, qt.skipped_at, qt.served_at, qt.expired_at, qt.cancelled_at, "
                + "qt.created_at, qt.updated_at "
                + "FROM queue_tickets qt "
                + "INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                + "INNER JOIN clinics c ON cs.clinic_id = c.clinic_id "
                + "INNER JOIN services s ON cs.service_id = s.service_id "
                + "INNER JOIN users u ON qt.patient_user_id = u.user_id "
                + "WHERE cs.clinic_id = ? AND qt.patient_user_id = ? AND qt.status IN ('WAITING', 'CALLED') ";

        if (queueDate != null) {
            sql += "AND qt.queue_date = ? ";
        }

        sql += "ORDER BY qt.queue_date DESC, qt.created_at DESC LIMIT 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicId);
            statement.setInt(2, patientUserId);
            if (queueDate != null) {
                statement.setDate(3, Date.valueOf(queueDate));
            }

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapQueueTicket(rs);
                }
            }
        }

        return null;
    }

    public boolean hasActiveQueueTicket(Integer patientUserId) throws SQLException {
        if (patientUserId == null) {
            return false;
        }

        try (Connection connection = getConnection()) {
            return hasActiveQueueTicket(connection, patientUserId);
        }
    }

    public boolean hasActiveQueueTicket(Integer patientUserId, Integer clinicId, LocalDate queueDate) throws SQLException {
        if (patientUserId == null || clinicId == null || queueDate == null) {
            return false;
        }

        try (Connection connection = getConnection()) {
            return hasActiveQueueTicket(connection, patientUserId, clinicId, queueDate);
        }
    }

    public Integer createPatientWalkInQueueTicket(Integer patientUserId, Integer clinicId, Integer clinicServiceId, LocalDate queueDate)
            throws SQLException {
        if (patientUserId == null || clinicId == null || clinicServiceId == null) {
            return null;
        }

        LocalDate normalizedQueueDate = queueDate == null ? LocalDate.now() : queueDate;

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                Integer matchedClinicServiceId = findClinicServiceIdForClinicAndWalkInEnabled(connection, clinicId, clinicServiceId);
                if (matchedClinicServiceId == null || hasActiveQueueTicket(connection, patientUserId, clinicId, normalizedQueueDate)) {
                    connection.rollback();
                    return null;
                }

                int nextTicketNumber = findNextTicketNumber(connection, clinicId, normalizedQueueDate);
                int estimatedWaitMinutes = findEstimatedWaitMinutes(connection, matchedClinicServiceId, normalizedQueueDate);

                String sql = "INSERT INTO queue_tickets (clinic_service_id, patient_user_id, queue_date, ticket_number, status, estimated_wait_minutes) "
                        + "VALUES (?, ?, ?, ?, 'WAITING', ?)";

                try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, matchedClinicServiceId);
                    statement.setInt(2, patientUserId);
                    statement.setDate(3, Date.valueOf(normalizedQueueDate));
                    statement.setInt(4, nextTicketNumber);
                    statement.setInt(5, Math.max(estimatedWaitMinutes, 0));

                    int affected = statement.executeUpdate();
                    if (affected <= 0) {
                        connection.rollback();
                        return null;
                    }

                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            connection.commit();
                            return generatedKeys.getInt(1);
                        }
                    }
                }

                connection.rollback();
                return null;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<QueueTicket> findQueueTicketsByPatient(Integer patientUserId, Integer clinicId, LocalDate queueDate, int limit)
            throws SQLException {
        List<QueueTicket> queueTickets = new ArrayList<>();
        if (patientUserId == null) {
            return queueTickets;
        }

        int safeLimit = limit <= 0 ? 20 : limit;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT qt.ticket_id, qt.clinic_service_id, cs.clinic_id, c.clinic_name, cs.service_id, s.service_name, ");
        sql.append("qt.patient_user_id, u.full_name AS patient_name, qt.queue_date, qt.ticket_number, qt.status, ");
        sql.append("qt.estimated_wait_minutes, qt.called_at, qt.skipped_at, qt.served_at, qt.expired_at, qt.cancelled_at, ");
        sql.append("qt.created_at, qt.updated_at ");
        sql.append("FROM queue_tickets qt ");
        sql.append("INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id ");
        sql.append("INNER JOIN clinics c ON cs.clinic_id = c.clinic_id ");
        sql.append("INNER JOIN services s ON cs.service_id = s.service_id ");
        sql.append("INNER JOIN users u ON qt.patient_user_id = u.user_id ");
        sql.append("WHERE qt.patient_user_id = ? ");

        if (clinicId != null) {
            sql.append("AND cs.clinic_id = ? ");
        }
        if (queueDate != null) {
            sql.append("AND qt.queue_date = ? ");
        }

        sql.append("ORDER BY qt.queue_date DESC, qt.created_at DESC LIMIT ?");

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            statement.setInt(parameterIndex++, patientUserId);
            if (clinicId != null) {
                statement.setInt(parameterIndex++, clinicId);
            }
            if (queueDate != null) {
                statement.setDate(parameterIndex++, Date.valueOf(queueDate));
            }
            statement.setInt(parameterIndex, safeLimit);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    queueTickets.add(mapQueueTicket(rs));
                }
            }
        }

        return queueTickets;
    }

    public Integer callNextWaitingTicket(Integer clinicId, LocalDate queueDate) throws SQLException {
        return callNextWaitingTicket(clinicId, null, queueDate);
    }

    public Integer callNextWaitingTicket(Integer clinicId, String serviceName, LocalDate queueDate) throws SQLException {
        Integer ticketId = findNextWaitingTicketId(clinicId, serviceName, queueDate);
        if (ticketId == null) {
            return null;
        }

        updateTicketStatus(clinicId, ticketId, "CALLED");
        return ticketId;
    }

    public Integer skipCurrentCalledTicket(Integer clinicId, LocalDate queueDate) throws SQLException {
        return skipCurrentCalledTicket(clinicId, null, queueDate);
    }

    public Integer skipCurrentCalledTicket(Integer clinicId, String serviceName, LocalDate queueDate) throws SQLException {
        Integer ticketId = findCurrentCalledTicketId(clinicId, serviceName, queueDate);
        if (ticketId == null) {
            return null;
        }
        return updateTicketStatus(clinicId, ticketId, "SKIPPED") ? ticketId : null;
    }

    public boolean markCurrentCalledTicketServed(Integer clinicId, LocalDate queueDate) throws SQLException {
        return markCurrentCalledTicketServed(clinicId, null, queueDate);
    }

    public boolean markCurrentCalledTicketServed(Integer clinicId, String serviceName, LocalDate queueDate) throws SQLException {
        Integer ticketId = findCurrentCalledTicketId(clinicId, serviceName, queueDate);
        if (ticketId == null) {
            return false;
        }
        return updateTicketStatus(clinicId, ticketId, "SERVED");
    }

    public Integer expireCurrentCalledTicket(Integer clinicId, LocalDate queueDate) throws SQLException {
        return expireCurrentCalledTicket(clinicId, null, queueDate);
    }

    public Integer expireCurrentCalledTicket(Integer clinicId, String serviceName, LocalDate queueDate) throws SQLException {
        Integer ticketId = findCurrentCalledTicketId(clinicId, serviceName, queueDate);
        if (ticketId == null) {
            return null;
        }
        return updateTicketStatus(clinicId, ticketId, "EXPIRED") ? ticketId : null;
    }

    public boolean updateTicketStatus(Integer clinicId, Integer ticketId, String newStatus) throws SQLException {
        if (clinicId == null || ticketId == null || newStatus == null) {
            return false;
        }

        String normalizedStatus = newStatus.trim().toUpperCase();
        String sql;
        switch (normalizedStatus) {
            case "CALLED":
                sql = "UPDATE queue_tickets qt INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                        + "SET qt.status = 'CALLED', qt.called_at = ?, qt.updated_at = CURRENT_TIMESTAMP "
                        + "WHERE qt.ticket_id = ? AND cs.clinic_id = ?";
                return executeQueueUpdate(sql, new Timestamp(System.currentTimeMillis()), ticketId, clinicId);
            case "SKIPPED":
                sql = "UPDATE queue_tickets qt INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                        + "SET qt.status = 'SKIPPED', qt.skipped_at = ?, qt.updated_at = CURRENT_TIMESTAMP "
                        + "WHERE qt.ticket_id = ? AND cs.clinic_id = ?";
                return executeQueueUpdate(sql, new Timestamp(System.currentTimeMillis()), ticketId, clinicId);
            case "SERVED":
                sql = "UPDATE queue_tickets qt INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                        + "SET qt.status = 'SERVED', qt.served_at = ?, qt.updated_at = CURRENT_TIMESTAMP "
                        + "WHERE qt.ticket_id = ? AND cs.clinic_id = ?";
                return executeQueueUpdate(sql, new Timestamp(System.currentTimeMillis()), ticketId, clinicId);
            case "EXPIRED":
                sql = "UPDATE queue_tickets qt INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                        + "SET qt.status = 'EXPIRED', qt.expired_at = ?, qt.updated_at = CURRENT_TIMESTAMP "
                        + "WHERE qt.ticket_id = ? AND cs.clinic_id = ?";
                return executeQueueUpdate(sql, new Timestamp(System.currentTimeMillis()), ticketId, clinicId);
            case "CANCELLED":
                sql = "UPDATE queue_tickets qt INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                        + "SET qt.status = 'CANCELLED', qt.cancelled_at = ?, qt.updated_at = CURRENT_TIMESTAMP "
                        + "WHERE qt.ticket_id = ? AND cs.clinic_id = ?";
                return executeQueueUpdate(sql, new Timestamp(System.currentTimeMillis()), ticketId, clinicId);
            default:
                return false;
        }
    }

    private Integer findNextWaitingTicketId(Integer clinicId, String serviceName, LocalDate queueDate) throws SQLException {
        String sql = "SELECT qt.ticket_id FROM queue_tickets qt INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                + "INNER JOIN services s ON cs.service_id = s.service_id "
                + "WHERE cs.clinic_id = ? AND qt.status = 'WAITING' ";
        if (serviceName != null && !serviceName.trim().isEmpty()) {
            sql += "AND s.service_name = ? ";
        }
        if (queueDate != null) {
            sql += "AND qt.queue_date = ? ";
        }
        sql += "ORDER BY qt.queue_date ASC, qt.ticket_number ASC LIMIT 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            statement.setInt(parameterIndex++, clinicId);
            if (serviceName != null && !serviceName.trim().isEmpty()) {
                statement.setString(parameterIndex++, serviceName.trim());
            }
            if (queueDate != null) {
                statement.setDate(parameterIndex++, Date.valueOf(queueDate));
            }

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ticket_id");
                }
            }
        }

        return null;
    }

    private Integer findCurrentCalledTicketId(Integer clinicId, String serviceName, LocalDate queueDate) throws SQLException {
        String sql = "SELECT qt.ticket_id FROM queue_tickets qt INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                + "INNER JOIN services s ON cs.service_id = s.service_id "
                + "WHERE cs.clinic_id = ? AND qt.status = 'CALLED' ";
        if (serviceName != null && !serviceName.trim().isEmpty()) {
            sql += "AND s.service_name = ? ";
        }
        if (queueDate != null) {
            sql += "AND qt.queue_date = ? ";
        }
        sql += "ORDER BY qt.queue_date ASC, qt.called_at ASC, qt.ticket_number ASC LIMIT 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            statement.setInt(parameterIndex++, clinicId);
            if (serviceName != null && !serviceName.trim().isEmpty()) {
                statement.setString(parameterIndex++, serviceName.trim());
            }
            if (queueDate != null) {
                statement.setDate(parameterIndex++, Date.valueOf(queueDate));
            }

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ticket_id");
                }
            }
        }

        return null;
    }

    private boolean hasActiveQueueTicket(Connection connection, Integer patientUserId) throws SQLException {
        String sql = "SELECT 1 FROM queue_tickets WHERE patient_user_id = ? AND status IN ('WAITING', 'CALLED') LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientUserId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasActiveQueueTicket(Connection connection, Integer patientUserId, Integer clinicId, LocalDate queueDate)
            throws SQLException {
        String sql = "SELECT 1 FROM queue_tickets qt "
                + "INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                + "WHERE qt.patient_user_id = ? AND cs.clinic_id = ? AND qt.queue_date = ? "
                + "AND qt.status IN ('WAITING', 'CALLED') LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientUserId);
            statement.setInt(2, clinicId);
            statement.setDate(3, Date.valueOf(queueDate));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Integer findClinicServiceIdForClinicAndWalkInEnabled(Connection connection, Integer clinicId, Integer clinicServiceId)
            throws SQLException {
        String sql = "SELECT cs.clinic_service_id FROM clinic_services cs "
                + "INNER JOIN services s ON cs.service_id = s.service_id "
                + "WHERE cs.clinic_service_id = ? AND cs.clinic_id = ? "
                + "AND cs.is_active = 1 AND s.is_active = 1 AND s.is_walkin_enabled = 1 LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicServiceId);
            statement.setInt(2, clinicId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("clinic_service_id");
                }
            }
        }

        return null;
    }

    private int findNextTicketNumber(Connection connection, Integer clinicId, LocalDate queueDate) throws SQLException {
        String sql = "SELECT COALESCE(MAX(qt.ticket_number), 0) + 1 AS next_ticket_number "
                + "FROM queue_tickets qt "
                + "INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                + "WHERE cs.clinic_id = ? AND qt.queue_date = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicId);
            statement.setDate(2, Date.valueOf(queueDate));

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int nextTicketNumber = rs.getInt("next_ticket_number");
                    return nextTicketNumber <= 0 ? 1 : nextTicketNumber;
                }
            }
        }

        return 1;
    }

    private int findEstimatedWaitMinutes(Connection connection, Integer clinicServiceId, LocalDate queueDate) throws SQLException {
        String sql = "SELECT COUNT(*) AS active_ticket_count, COALESCE(MAX(s.avg_service_minutes), 0) AS avg_service_minutes "
                + "FROM queue_tickets qt "
                + "INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id "
                + "INNER JOIN services s ON cs.service_id = s.service_id "
                + "WHERE qt.clinic_service_id = ? AND qt.queue_date = ? AND qt.status IN ('WAITING', 'CALLED')";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicServiceId);
            statement.setDate(2, Date.valueOf(queueDate));

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int activeTicketCount = rs.getInt("active_ticket_count");
                    int avgServiceMinutes = rs.getInt("avg_service_minutes");
                    if (avgServiceMinutes <= 0) {
                        return 0;
                    }
                    return Math.max(activeTicketCount, 0) * avgServiceMinutes;
                }
            }
        }

        return 0;
    }

    private boolean executeQueueUpdate(String sql, Timestamp timestamp, Integer ticketId, Integer clinicId) throws SQLException {
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, timestamp);
            statement.setInt(2, ticketId);
            statement.setInt(3, clinicId);
            return statement.executeUpdate() > 0;
        }
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