package ict.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import ict.bean.ClinicServiceStatus;
import ict.bean.IncidentLog;

public class ServiceDB {

    private final String url;
    private final String username;
    private final String password;

    public ServiceDB(String url, String username, String password) {
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

    public List<ClinicServiceStatus> findAllClinicServiceStatuses() throws SQLException {
        List<ClinicServiceStatus> services = new ArrayList<>();

        String sql = "SELECT cs.clinic_service_id, cs.clinic_id, c.clinic_name, s.service_id, s.service_name, s.description, "
                + "s.requires_approval, s.is_walkin_enabled, s.avg_service_minutes, cs.is_active, cs.default_capacity, "
                + "cs.created_at, cs.updated_at "
                + "FROM clinic_services cs "
                + "INNER JOIN clinics c ON cs.clinic_id = c.clinic_id "
                + "INNER JOIN services s ON cs.service_id = s.service_id "
                + "ORDER BY c.clinic_name, s.service_name";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                services.add(mapClinicServiceStatus(rs));
            }
        }

        return services;
    }

    public List<ClinicServiceStatus> findClinicServiceStatuses(Integer clinicId) throws SQLException {
        List<ClinicServiceStatus> services = new ArrayList<>();
        if (clinicId == null) {
            return services;
        }

        String sql = "SELECT cs.clinic_service_id, cs.clinic_id, c.clinic_name, s.service_id, s.service_name, s.description, "
                + "s.requires_approval, s.is_walkin_enabled, s.avg_service_minutes, cs.is_active, cs.default_capacity, "
                + "cs.created_at, cs.updated_at "
                + "FROM clinic_services cs "
                + "INNER JOIN clinics c ON cs.clinic_id = c.clinic_id "
                + "INNER JOIN services s ON cs.service_id = s.service_id "
                + "WHERE cs.clinic_id = ? "
                + "ORDER BY s.service_name";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    services.add(mapClinicServiceStatus(rs));
                }
            }
        }

        return services;
    }

    public List<ClinicServiceStatus> findActiveServices() throws SQLException {
        List<ClinicServiceStatus> services = new ArrayList<>();

        String sql = "SELECT service_id, service_name, description, requires_approval, is_walkin_enabled, avg_service_minutes, is_active "
                + "FROM services WHERE is_active = 1 ORDER BY service_name";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                services.add(mapActiveService(rs));
            }
        }

        return services;
    }

    public List<ClinicServiceStatus> findServiceCatalog() throws SQLException {
        List<ClinicServiceStatus> services = new ArrayList<>();

        String sql = "SELECT service_id, service_name, description, requires_approval, is_walkin_enabled, avg_service_minutes, is_active, created_at, updated_at "
                + "FROM services ORDER BY is_active DESC, service_name";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                services.add(mapServiceRecord(rs));
            }
        }

        return services;
    }

    public ClinicServiceStatus findServiceRecordById(Integer serviceId) throws SQLException {
        if (serviceId == null) {
            return null;
        }

        String sql = "SELECT service_id, service_name, description, requires_approval, is_walkin_enabled, avg_service_minutes, is_active, created_at, updated_at "
                + "FROM services WHERE service_id = ? LIMIT 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, serviceId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapServiceRecord(rs);
                }
            }
        }

        return null;
    }

    public Integer findServiceIdByName(String serviceName) throws SQLException {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT service_id FROM services WHERE LOWER(service_name) = LOWER(?) LIMIT 1";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serviceName.trim());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("service_id");
                }
            }
        }

        return null;
    }

    public Integer saveServiceRecord(Integer serviceId, String serviceName, String description, Integer avgMinutes,
            boolean walkInEnabled, boolean requiresApproval, boolean active) throws SQLException {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return null;
        }

        Integer resolvedServiceId = serviceId;
        if (resolvedServiceId == null) {
            resolvedServiceId = findServiceIdByName(serviceName);
        }

        int normalizedAvgMinutes = avgMinutes == null || avgMinutes <= 0 ? 30 : avgMinutes;

        if (resolvedServiceId != null) {
            String updateSql = "UPDATE services SET service_name = ?, description = ?, requires_approval = ?, is_walkin_enabled = ?, avg_service_minutes = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE service_id = ?";
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setString(1, serviceName.trim());
                if (description == null || description.trim().isEmpty()) {
                    statement.setNull(2, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(2, description.trim());
                }
                statement.setInt(3, requiresApproval ? 1 : 0);
                statement.setInt(4, walkInEnabled ? 1 : 0);
                statement.setInt(5, normalizedAvgMinutes);
                statement.setInt(6, active ? 1 : 0);
                statement.setInt(7, resolvedServiceId);

                if (statement.executeUpdate() > 0) {
                    return resolvedServiceId;
                }
            }
        }

        String insertSql = resolvedServiceId == null
                ? "INSERT INTO services (service_name, description, requires_approval, is_walkin_enabled, avg_service_minutes, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
                : "INSERT INTO services (service_id, service_name, description, requires_approval, is_walkin_enabled, avg_service_minutes, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int parameterIndex = 1;
            if (resolvedServiceId != null) {
                statement.setInt(parameterIndex++, resolvedServiceId);
            }
            statement.setString(parameterIndex++, serviceName.trim());
            if (description == null || description.trim().isEmpty()) {
                statement.setNull(parameterIndex++, java.sql.Types.VARCHAR);
            } else {
                statement.setString(parameterIndex++, description.trim());
            }
            statement.setInt(parameterIndex++, requiresApproval ? 1 : 0);
            statement.setInt(parameterIndex++, walkInEnabled ? 1 : 0);
            statement.setInt(parameterIndex++, normalizedAvgMinutes);
            statement.setInt(parameterIndex, active ? 1 : 0);

            int affectedRows = statement.executeUpdate();
            if (affectedRows <= 0) {
                return null;
            }

            if (resolvedServiceId != null) {
                return resolvedServiceId;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        return null;
    }

    public boolean upsertClinicServiceCapacity(Integer clinicId, Integer serviceId, Integer capacity) throws SQLException {
        if (clinicId == null || serviceId == null) {
            return false;
        }

        String sql = "INSERT INTO clinic_services (clinic_id, service_id, is_active, default_capacity, created_at, updated_at) "
                + "VALUES (?, ?, 1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                + "ON DUPLICATE KEY UPDATE is_active = 1, default_capacity = VALUES(default_capacity), updated_at = CURRENT_TIMESTAMP";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicId);
            statement.setInt(2, serviceId);
            if (capacity == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, capacity);
            }
            return statement.executeUpdate() > 0;
        }
    }

    public boolean upsertOpeningHour(Integer serviceId, int dayOfWeek, LocalTime openTime, LocalTime closeTime) throws SQLException {
        if (serviceId == null || openTime == null || closeTime == null) {
            return false;
        }

        String sql = "INSERT INTO opening_hours (service_id, day_of_week, open_time, close_time) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE open_time = VALUES(open_time), close_time = VALUES(close_time)";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, serviceId);
            statement.setInt(2, dayOfWeek);
            statement.setTime(3, Time.valueOf(openTime));
            statement.setTime(4, Time.valueOf(closeTime));
            return statement.executeUpdate() > 0;
        }
    }

    public boolean setServiceActive(Integer serviceId, boolean active) throws SQLException {
        if (serviceId == null) {
            return false;
        }

        String sql = "UPDATE services SET is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE service_id = ?";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, active ? 1 : 0);
            statement.setInt(2, serviceId);
            return statement.executeUpdate() > 0;
        }
    }

    public List<IncidentLog> findIncidentLogsByClinic(Integer clinicId, String severity, String status, int limit)
            throws SQLException {
        List<IncidentLog> incidents = new ArrayList<>();
        if (clinicId == null) {
            return incidents;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT i.incident_id, i.reported_by_user_id, u.full_name AS reporter_name, i.clinic_id, c.clinic_name, ");
        sql.append("i.service_id, s.service_name, i.severity, i.status, i.title, i.description, i.occurred_at, i.resolved_at, ");
        sql.append("i.created_at, i.updated_at ");
        sql.append("FROM incident_logs i ");
        sql.append("INNER JOIN users u ON i.reported_by_user_id = u.user_id ");
        sql.append("INNER JOIN clinics c ON i.clinic_id = c.clinic_id ");
        sql.append("LEFT JOIN services s ON i.service_id = s.service_id ");
        sql.append("WHERE i.clinic_id = ? ");

        if (severity != null && !severity.trim().isEmpty()) {
            sql.append("AND i.severity = ? ");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND i.status = ? ");
        }

        sql.append("ORDER BY i.occurred_at DESC, i.incident_id DESC LIMIT ?");

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            statement.setInt(parameterIndex++, clinicId);
            if (severity != null && !severity.trim().isEmpty()) {
                statement.setString(parameterIndex++, severity.trim());
            }
            if (status != null && !status.trim().isEmpty()) {
                statement.setString(parameterIndex++, status.trim());
            }
            statement.setInt(parameterIndex, Math.max(limit, 1));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    incidents.add(mapIncidentLog(rs));
                }
            }
        }

        return incidents;
    }

    public Integer createIncidentLog(Integer reporterUserId, Integer clinicId, Integer serviceId, String severity,
            String title, String description, LocalDateTime occurredAt) throws SQLException {
        if (reporterUserId == null || clinicId == null || severity == null || title == null || description == null || occurredAt == null) {
            return null;
        }

        String sql = "INSERT INTO incident_logs (reported_by_user_id, clinic_id, service_id, severity, status, title, description, occurred_at, resolved_at) "
                + "VALUES (?, ?, ?, ?, 'OPEN', ?, ?, ?, NULL)";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, reporterUserId);
            statement.setInt(2, clinicId);
            if (serviceId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, serviceId);
            }
            statement.setString(4, severity.trim().toUpperCase());
            statement.setString(5, title.trim());
            statement.setString(6, description.trim());
            statement.setTimestamp(7, Timestamp.valueOf(occurredAt));

            int affectedRows = statement.executeUpdate();
            if (affectedRows <= 0) {
                return null;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        return null;
    }

    private ClinicServiceStatus mapClinicServiceStatus(ResultSet rs) throws SQLException {
        ClinicServiceStatus service = new ClinicServiceStatus();
        service.setClinicServiceId(rs.getInt("clinic_service_id"));

        int clinicId = rs.getInt("clinic_id");
        if (!rs.wasNull()) {
            service.setClinicId(clinicId);
        }
        service.setClinicName(rs.getString("clinic_name"));

        int serviceId = rs.getInt("service_id");
        if (!rs.wasNull()) {
            service.setServiceId(serviceId);
        }
        service.setServiceName(rs.getString("service_name"));
        service.setServiceDescription(rs.getString("description"));
        service.setRequiresApproval(rs.getInt("requires_approval") == 1);
        service.setWalkInEnabled(rs.getInt("is_walkin_enabled") == 1);
        service.setAvgServiceMinutes(rs.getInt("avg_service_minutes"));
        service.setActive(rs.getInt("is_active") == 1);

        int defaultCapacity = rs.getInt("default_capacity");
        if (!rs.wasNull()) {
            service.setDefaultCapacity(defaultCapacity);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            service.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            service.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return service;
    }

    private ClinicServiceStatus mapActiveService(ResultSet rs) throws SQLException {
        ClinicServiceStatus service = new ClinicServiceStatus();
        service.setServiceId(rs.getInt("service_id"));
        service.setServiceName(rs.getString("service_name"));
        service.setServiceDescription(rs.getString("description"));
        service.setRequiresApproval(rs.getInt("requires_approval") == 1);
        service.setWalkInEnabled(rs.getInt("is_walkin_enabled") == 1);
        service.setAvgServiceMinutes(rs.getInt("avg_service_minutes"));
        service.setActive(rs.getInt("is_active") == 1);
        return service;
    }

    private ClinicServiceStatus mapServiceRecord(ResultSet rs) throws SQLException {
        ClinicServiceStatus service = new ClinicServiceStatus();
        service.setServiceId(rs.getInt("service_id"));
        service.setServiceName(rs.getString("service_name"));
        service.setServiceDescription(rs.getString("description"));
        service.setRequiresApproval(rs.getInt("requires_approval") == 1);
        service.setWalkInEnabled(rs.getInt("is_walkin_enabled") == 1);
        service.setAvgServiceMinutes(rs.getInt("avg_service_minutes"));
        service.setActive(rs.getInt("is_active") == 1);

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            service.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            service.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return service;
    }

    private IncidentLog mapIncidentLog(ResultSet rs) throws SQLException {
        IncidentLog incident = new IncidentLog();
        incident.setIncidentId(rs.getInt("incident_id"));

        int reporterUserId = rs.getInt("reported_by_user_id");
        if (!rs.wasNull()) {
            incident.setReportedByUserId(reporterUserId);
        }
        incident.setReporterName(rs.getString("reporter_name"));

        int clinicId = rs.getInt("clinic_id");
        if (!rs.wasNull()) {
            incident.setClinicId(clinicId);
        }
        incident.setClinicName(rs.getString("clinic_name"));

        int serviceId = rs.getInt("service_id");
        if (!rs.wasNull()) {
            incident.setServiceId(serviceId);
        }
        incident.setServiceName(rs.getString("service_name"));
        incident.setSeverity(rs.getString("severity"));
        incident.setStatus(rs.getString("status"));
        incident.setTitle(rs.getString("title"));
        incident.setDescription(rs.getString("description"));

        Timestamp occurredAt = rs.getTimestamp("occurred_at");
        if (occurredAt != null) {
            incident.setOccurredAt(occurredAt.toLocalDateTime());
        }

        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        if (resolvedAt != null) {
            incident.setResolvedAt(resolvedAt.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            incident.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            incident.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return incident;
    }
}