package ict.db;

import ict.bean.Appointment;
import ict.bean.AppointmentBookingSlot;
import ict.bean.StaffDashboardStats;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppointmentDB {

    private final String url;
    private final String username;
    private final String password;

    public AppointmentDB(String url, String username, String password) {
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

    public List<Appointment> findAppointmentsByClinic(Integer clinicId, String serviceName, String status, String patientName, LocalDate slotDate)
            throws SQLException {
        if (slotDate == null) {
            return findAppointmentsByClinic(clinicId, serviceName, status, patientName, (LocalDate) null, (LocalDate) null);
        }

        return findAppointmentsByClinic(clinicId, serviceName, status, patientName, slotDate, slotDate);
    }

    public List<Appointment> findAppointmentsByClinic(Integer clinicId, String serviceName, String status, String patientName, LocalDate fromDate, LocalDate toDate)
            throws SQLException {
        List<Appointment> appointments = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("a.appointment_id, ");
        sql.append("u.full_name AS patient_name, ");
        sql.append("c.clinic_name, ");
        sql.append("s.service_name, ");
        sql.append("a.appointment_date AS slot_date, ");
        sql.append("a.start_time, ");
        sql.append("a.end_time, ");
        sql.append("a.status, ");
        sql.append("ru.full_name AS reviewed_by_name, ");
        sql.append("a.reviewed_at, ");
        sql.append("a.review_reason, ");
        sql.append("a.checked_in_at, ");
        sql.append("a.completed_at, ");
        sql.append("a.cancelled_at, ");
        sql.append("a.cancellation_reason, ");
        sql.append("a.outcome_notes ");
        sql.append("FROM appointments a ");
        sql.append("INNER JOIN users u ON a.patient_user_id = u.user_id ");
        sql.append("INNER JOIN clinics c ON a.clinic_id = c.clinic_id ");
        sql.append("INNER JOIN services s ON a.service_id = s.service_id ");
        sql.append("LEFT JOIN users ru ON a.reviewed_by_user_id = ru.user_id ");
        sql.append("WHERE 1=1 ");

        if (clinicId != null && clinicId > 0) {
            sql.append("AND a.clinic_id = ? ");
        }
        if (serviceName != null && !serviceName.trim().isEmpty()) {
            sql.append("AND s.service_name = ? ");
        }
        boolean cancelledSummaryFilter = isCancelledSummaryFilter(status);
        if (status != null && !status.trim().isEmpty() && !cancelledSummaryFilter) {
            sql.append("AND a.status = ? ");
        } else if (cancelledSummaryFilter) {
            sql.append("AND a.status IN ('CANCELLED_BY_PATIENT', 'CANCELLED_BY_CLINIC', 'REJECTED_BY_CLINIC') ");
        }
        if (patientName != null && !patientName.trim().isEmpty()) {
            sql.append("AND LOWER(u.full_name) LIKE ? ");
        }
        if (fromDate != null) {
            sql.append("AND a.appointment_date >= ? ");
        }
        if (toDate != null) {
            sql.append("AND a.appointment_date <= ? ");
        }

        sql.append("ORDER BY a.appointment_date DESC, a.start_time DESC");

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            if (clinicId != null && clinicId > 0) {
                statement.setInt(parameterIndex++, clinicId);
            }
            if (serviceName != null && !serviceName.trim().isEmpty()) {
                statement.setString(parameterIndex++, serviceName.trim());
            }
            if (status != null && !status.trim().isEmpty() && !cancelledSummaryFilter) {
                statement.setString(parameterIndex++, status.trim());
            }
            if (patientName != null && !patientName.trim().isEmpty()) {
                statement.setString(parameterIndex++, "%" + patientName.trim().toLowerCase() + "%");
            }
            if (fromDate != null) {
                statement.setDate(parameterIndex++, Date.valueOf(fromDate));
            }
            if (toDate != null) {
                statement.setDate(parameterIndex++, Date.valueOf(toDate));
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapAppointment(rs));
                }
            }
        }

        return appointments;
    }

    private boolean isCancelledSummaryFilter(String status) {
        if (status == null || status.trim().isEmpty()) {
            return false;
        }

        return "CANCELLED".equalsIgnoreCase(status.trim());
    }

    public List<String> findServiceNamesByClinic(Integer clinicId) throws SQLException {
        List<String> services = new ArrayList<>();
        if (clinicId == null) {
            return services;
        }

        String sql = "SELECT DISTINCT s.service_name "
                + "FROM clinic_services cs "
                + "INNER JOIN services s ON cs.service_id = s.service_id "
                + "WHERE cs.clinic_id = ? AND cs.is_active = 1 AND s.is_active = 1 "
                + "ORDER BY s.service_name";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    services.add(rs.getString("service_name"));
                }
            }
        }

        return services;
    }

    public List<Appointment> findAppointmentsByPatient(Integer patientUserId, Integer clinicId, int limit) throws SQLException {
        List<Appointment> appointments = new ArrayList<>();
        if (patientUserId == null || clinicId == null) {
            return appointments;
        }

        String sql = "SELECT a.appointment_id, u.full_name AS patient_name, c.clinic_name, s.service_name, a.appointment_date AS slot_date, a.start_time, a.end_time, "
                + "a.status, ru.full_name AS reviewed_by_name, a.reviewed_at, a.review_reason, a.checked_in_at, a.completed_at, a.cancelled_at, "
                + "a.cancellation_reason, a.outcome_notes "
                + "FROM appointments a "
                + "INNER JOIN users u ON a.patient_user_id = u.user_id "
                + "INNER JOIN clinics c ON a.clinic_id = c.clinic_id "
                + "INNER JOIN services s ON a.service_id = s.service_id "
                + "LEFT JOIN users ru ON a.reviewed_by_user_id = ru.user_id "
                + "WHERE a.patient_user_id = ? AND a.clinic_id = ? "
            + "ORDER BY a.appointment_date DESC, a.start_time DESC, a.appointment_id DESC LIMIT ?";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientUserId);
            statement.setInt(2, clinicId);
            statement.setInt(3, Math.max(limit, 1));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapAppointment(rs));
                }
            }
        }

        return appointments;
    }

    public List<Appointment> findAppointmentsByPatient(Integer patientUserId, int limit) throws SQLException {
        List<Appointment> appointments = new ArrayList<>();
        if (patientUserId == null) {
            return appointments;
        }

        String sql = "SELECT a.appointment_id, u.full_name AS patient_name, c.clinic_name, s.service_name, a.appointment_date AS slot_date, a.start_time, a.end_time, "
                + "a.status, ru.full_name AS reviewed_by_name, a.reviewed_at, a.review_reason, a.checked_in_at, a.completed_at, a.cancelled_at, "
                + "a.cancellation_reason, a.outcome_notes "
                + "FROM appointments a "
                + "INNER JOIN users u ON a.patient_user_id = u.user_id "
                + "INNER JOIN clinics c ON a.clinic_id = c.clinic_id "
                + "INNER JOIN services s ON a.service_id = s.service_id "
                + "LEFT JOIN users ru ON a.reviewed_by_user_id = ru.user_id "
                + "WHERE a.patient_user_id = ? "
            + "ORDER BY a.appointment_date DESC, a.start_time DESC, a.appointment_id DESC LIMIT ?";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientUserId);
            statement.setInt(2, Math.max(limit, 1));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapAppointment(rs));
                }
            }
        }

        return appointments;
    }

    public List<Appointment> findPatientAppointments(Integer patientUserId, int limit) throws SQLException {
        return findAppointmentsByPatient(patientUserId, limit);
    }

    public int countActiveAppointments(Integer patientUserId) throws SQLException {
        if (patientUserId == null) {
            return 0;
        }

        String sql = "SELECT COUNT(*) AS total FROM appointments WHERE patient_user_id = ? AND status IN ('PENDING', 'CONFIRMED', 'ARRIVED')";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientUserId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }

    public Appointment findPatientAppointmentById(Integer patientUserId, Integer appointmentId) throws SQLException {
        if (patientUserId == null || appointmentId == null) {
            return null;
        }

        String sql = "SELECT a.appointment_id, u.full_name AS patient_name, c.clinic_name, s.service_name, a.appointment_date AS slot_date, a.start_time, a.end_time, "
                + "a.status, ru.full_name AS reviewed_by_name, a.reviewed_at, a.review_reason, a.checked_in_at, a.completed_at, a.cancelled_at, "
                + "a.cancellation_reason, a.outcome_notes "
                + "FROM appointments a "
                + "INNER JOIN users u ON a.patient_user_id = u.user_id "
                + "INNER JOIN clinics c ON a.clinic_id = c.clinic_id "
                + "INNER JOIN services s ON a.service_id = s.service_id "
                + "LEFT JOIN users ru ON a.reviewed_by_user_id = ru.user_id "
                + "WHERE a.patient_user_id = ? AND a.appointment_id = ? LIMIT 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientUserId);
            statement.setInt(2, appointmentId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapAppointment(rs);
                }
            }
        }

        return null;
    }

    public Map<LocalDate, Integer> findAvailableBookingDateCounts(LocalDate fromDate, LocalDate toDate) throws SQLException {
        return findAvailableBookingDateCounts(fromDate, toDate, null, null);
    }

    public Map<LocalDate, Integer> findAvailableBookingDateCounts(LocalDate fromDate, LocalDate toDate,
            Integer clinicId, Integer serviceId) throws SQLException {
        Map<LocalDate, Integer> bookingCounts = new LinkedHashMap<>();
        if (fromDate == null || toDate == null) {
            return bookingCounts;
        }

        LocalDate currentDate = fromDate;
        while (!currentDate.isAfter(toDate)) {
            int availableSlots = 0;
            for (AppointmentBookingSlot slot : findBookingSlots(currentDate, clinicId, serviceId)) {
                if (slot != null && slot.isAvailable()) {
                    availableSlots++;
                }
            }
            bookingCounts.put(currentDate, availableSlots);
            currentDate = currentDate.plusDays(1);
        }

        return bookingCounts;
    }

    public List<AppointmentBookingSlot> findBookingSlots(LocalDate slotDate) throws SQLException {
        return findBookingSlots(slotDate, null, null);
    }

    public List<AppointmentBookingSlot> findBookingSlots(LocalDate slotDate, Integer clinicId, Integer serviceId) throws SQLException {
        List<AppointmentBookingSlot> slots = new ArrayList<>();
        if (slotDate == null) {
            return slots;
        }

        List<BookingSlotTemplate> templates = loadBookingSlotTemplates(slotDate, clinicId, serviceId);
        Map<String, Integer> bookedCounts = loadBookedSlotCounts(slotDate, clinicId, serviceId);

        for (BookingSlotTemplate template : templates) {
            if (template == null) {
                continue;
            }

            int slotMinutes = Math.max(template.avgServiceMinutes, 1);
            LocalTime startTime = template.openTime;
            while (!startTime.plusMinutes(slotMinutes).isAfter(template.closeTime)) {
                LocalTime endTime = startTime.plusMinutes(slotMinutes);
                int bookedCount = bookedCounts.getOrDefault(buildBookingSlotKey(template.clinicId, template.serviceId, slotDate, startTime), 0);
                slots.add(buildBookingSlot(template, slotDate, startTime, endTime, bookedCount));
                startTime = startTime.plusMinutes(slotMinutes);
            }
        }

        return slots;
    }

    public AppointmentBookingSlot findBookingSlot(LocalDate slotDate, Integer clinicId, Integer serviceId, LocalTime startTime, LocalTime endTime) throws SQLException {
        if (slotDate == null || startTime == null) {
            return null;
        }

        for (AppointmentBookingSlot slot : findBookingSlots(slotDate, clinicId, serviceId)) {
            if (slot == null) {
                continue;
            }

            boolean matchesStart = startTime.equals(slot.getStartTime());
            boolean matchesEnd = endTime == null || endTime.equals(slot.getEndTime());
            if (matchesStart && matchesEnd) {
                return slot;
            }
        }

        return null;
    }

    public boolean hasPatientAppointmentConflict(Integer patientUserId, LocalDate appointmentDate, LocalTime startTime, LocalTime endTime) throws SQLException {
        if (patientUserId == null || appointmentDate == null || startTime == null || endTime == null) {
            return false;
        }

        String sql = "SELECT COUNT(*) AS total FROM appointments "
                + "WHERE patient_user_id = ? AND appointment_date = ? AND status IN ('PENDING', 'CONFIRMED', 'ARRIVED') "
                + "AND NOT (end_time <= ? OR start_time >= ?)";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientUserId);
            statement.setDate(2, Date.valueOf(appointmentDate));
            statement.setTime(3, Time.valueOf(startTime));
            statement.setTime(4, Time.valueOf(endTime));

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        }

        return false;
    }

    public Integer createPatientAppointment(Integer patientUserId, Integer clinicId, Integer serviceId, LocalDate appointmentDate,
            LocalTime startTime, LocalTime endTime, String status, String outcomeNotes) throws SQLException {
        if (patientUserId == null || clinicId == null || serviceId == null || appointmentDate == null || startTime == null || endTime == null || status == null) {
            return null;
        }

        String sql = "INSERT INTO appointments (patient_user_id, clinic_id, service_id, appointment_date, start_time, end_time, status, outcome_notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, patientUserId);
            statement.setInt(2, clinicId);
            statement.setInt(3, serviceId);
            statement.setDate(4, Date.valueOf(appointmentDate));
            statement.setTime(5, Time.valueOf(startTime));
            statement.setTime(6, Time.valueOf(endTime));
            statement.setString(7, status.trim().toUpperCase());
            if (outcomeNotes == null || outcomeNotes.trim().isEmpty()) {
                statement.setNull(8, java.sql.Types.VARCHAR);
            } else {
                statement.setString(8, outcomeNotes.trim());
            }

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

    public StaffDashboardStats findDashboardStatsByClinic(Integer clinicId) throws SQLException {
        if (clinicId == null) {
            return new StaffDashboardStats();
        }

        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM appointments a WHERE a.clinic_id = ? AND a.appointment_date = CURRENT_DATE()) AS today_appointments, "
                + "(SELECT COUNT(*) FROM queue_tickets qt INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id WHERE cs.clinic_id = ? AND qt.queue_date = CURRENT_DATE() AND qt.status = 'WAITING') AS waiting_queue_tickets, "
                + "(SELECT COUNT(*) FROM appointments a WHERE a.clinic_id = ? AND a.status = 'PENDING') AS pending_approvals, "
                + "(SELECT COUNT(*) FROM incident_logs i WHERE i.clinic_id = ? AND i.status IN ('OPEN', 'IN_PROGRESS')) AS open_service_issues";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicId);
            statement.setInt(2, clinicId);
            statement.setInt(3, clinicId);
            statement.setInt(4, clinicId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new StaffDashboardStats(
                            rs.getInt("today_appointments"),
                            rs.getInt("waiting_queue_tickets"),
                            rs.getInt("pending_approvals"),
                            rs.getInt("open_service_issues"));
                }
            }
        }

        return new StaffDashboardStats();
    }

    private List<BookingSlotTemplate> loadBookingSlotTemplates(LocalDate slotDate, Integer clinicId, Integer serviceId) throws SQLException {
        List<BookingSlotTemplate> templates = new ArrayList<>();
        int dayOfWeek = slotDate.getDayOfWeek().getValue() - 1;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cs.clinic_service_id, cs.clinic_id, c.clinic_name, cs.service_id, s.service_name, s.description, ");
        sql.append("s.requires_approval, s.is_walkin_enabled, s.avg_service_minutes, cs.default_capacity, oh.open_time, oh.close_time ");
        sql.append("FROM clinic_services cs ");
        sql.append("INNER JOIN clinics c ON cs.clinic_id = c.clinic_id ");
        sql.append("INNER JOIN services s ON cs.service_id = s.service_id ");
        sql.append("INNER JOIN opening_hours oh ON oh.service_id = cs.service_id AND oh.day_of_week = ? ");
        sql.append("WHERE cs.is_active = 1 AND s.is_active = 1 ");
        if (clinicId != null) {
            sql.append("AND cs.clinic_id = ? ");
        }
        if (serviceId != null) {
            sql.append("AND cs.service_id = ? ");
        }
        sql.append("ORDER BY c.clinic_name, s.service_name, oh.open_time");

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            statement.setInt(parameterIndex++, dayOfWeek);
            if (clinicId != null) {
                statement.setInt(parameterIndex++, clinicId);
            }
            if (serviceId != null) {
                statement.setInt(parameterIndex++, serviceId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    BookingSlotTemplate template = new BookingSlotTemplate();
                    template.clinicServiceId = rs.getInt("clinic_service_id");

                    int resolvedClinicId = rs.getInt("clinic_id");
                    if (!rs.wasNull()) {
                        template.clinicId = resolvedClinicId;
                    }
                    template.clinicName = rs.getString("clinic_name");

                    int resolvedServiceId = rs.getInt("service_id");
                    if (!rs.wasNull()) {
                        template.serviceId = resolvedServiceId;
                    }
                    template.serviceName = rs.getString("service_name");
                    template.serviceDescription = rs.getString("description");
                    template.requiresApproval = rs.getInt("requires_approval") == 1;
                    template.walkInEnabled = rs.getInt("is_walkin_enabled") == 1;

                    int avgServiceMinutes = rs.getInt("avg_service_minutes");
                    template.avgServiceMinutes = avgServiceMinutes <= 0 ? 1 : avgServiceMinutes;

                    int defaultCapacity = rs.getInt("default_capacity");
                    template.capacity = rs.wasNull() || defaultCapacity < 0 ? 1 : defaultCapacity;

                    Time openTime = rs.getTime("open_time");
                    Time closeTime = rs.getTime("close_time");
                    if (openTime == null || closeTime == null) {
                        continue;
                    }

                    template.openTime = openTime.toLocalTime();
                    template.closeTime = closeTime.toLocalTime();
                    templates.add(template);
                }
            }
        }

        return templates;
    }

    private Map<String, Integer> loadBookedSlotCounts(LocalDate slotDate, Integer clinicId, Integer serviceId) throws SQLException {
        Map<String, Integer> bookedCounts = new LinkedHashMap<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT a.clinic_id, a.service_id, a.start_time, COUNT(*) AS booked_count ");
        sql.append("FROM appointments a ");
        sql.append("WHERE a.appointment_date = ? AND a.status IN ('PENDING', 'CONFIRMED', 'ARRIVED') ");
        if (clinicId != null) {
            sql.append("AND a.clinic_id = ? ");
        }
        if (serviceId != null) {
            sql.append("AND a.service_id = ? ");
        }
        sql.append("GROUP BY a.clinic_id, a.service_id, a.start_time");

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            statement.setDate(parameterIndex++, Date.valueOf(slotDate));
            if (clinicId != null) {
                statement.setInt(parameterIndex++, clinicId);
            }
            if (serviceId != null) {
                statement.setInt(parameterIndex++, serviceId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int resolvedClinicId = rs.getInt("clinic_id");
                    int resolvedServiceId = rs.getInt("service_id");
                    Time startTime = rs.getTime("start_time");
                    if (startTime == null) {
                        continue;
                    }

                    bookedCounts.put(buildBookingSlotKey(resolvedClinicId, resolvedServiceId, slotDate, startTime.toLocalTime()), rs.getInt("booked_count"));
                }
            }
        }

        return bookedCounts;
    }

    private AppointmentBookingSlot buildBookingSlot(BookingSlotTemplate template, LocalDate slotDate, LocalTime startTime,
            LocalTime endTime, int bookedCount) {
        AppointmentBookingSlot slot = new AppointmentBookingSlot();
        slot.setClinicServiceId(template.clinicServiceId);
        slot.setClinicId(template.clinicId);
        slot.setClinicName(template.clinicName);
        slot.setServiceId(template.serviceId);
        slot.setServiceName(template.serviceName);
        slot.setServiceDescription(template.serviceDescription);
        slot.setRequiresApproval(template.requiresApproval);
        slot.setWalkInEnabled(template.walkInEnabled);
        slot.setSlotDate(slotDate);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setCapacity(template.capacity);
        slot.setBookedCount(bookedCount);
        slot.setSlotStatus("OPEN");
        return slot;
    }

    private String buildBookingSlotKey(Integer clinicId, Integer serviceId, LocalDate slotDate, LocalTime startTime) {
        String clinicPart = clinicId == null ? "" : clinicId.toString();
        String servicePart = serviceId == null ? "" : serviceId.toString();
        String datePart = slotDate == null ? "" : slotDate.toString();
        String timePart = startTime == null ? "" : startTime.toString();
        return clinicPart + "|" + servicePart + "|" + datePart + "|" + timePart;
    }

    private static class BookingSlotTemplate {
        private Integer clinicServiceId;
        private Integer clinicId;
        private String clinicName;
        private Integer serviceId;
        private String serviceName;
        private String serviceDescription;
        private boolean requiresApproval;
        private boolean walkInEnabled;
        private int avgServiceMinutes;
        private int capacity;
        private LocalTime openTime;
        private LocalTime closeTime;
    }

    public boolean updateAppointmentForClinic(Integer clinicId, Integer reviewerUserId, Integer appointmentId,
            String updateAction, String remark) throws SQLException {
        if (clinicId == null || reviewerUserId == null || appointmentId == null) {
            throw new IllegalArgumentException("Clinic id, reviewer id, and appointment id are required.");
        }

        String normalizedAction = updateAction == null ? "" : updateAction.trim().toUpperCase();
        String reason = hasText(remark) ? remark.trim() : defaultReason(normalizedAction);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        String sql;
        switch (normalizedAction) {
            case "APPROVE":
                sql = "UPDATE appointments SET status = 'CONFIRMED', reviewed_by_user_id = ?, reviewed_at = ?, review_reason = ?, checked_in_at = NULL, completed_at = NULL, cancelled_at = NULL, cancellation_reason = NULL, outcome_notes = ? WHERE appointment_id = ? AND clinic_id = ?";
                return executeAppointmentUpdate(sql, reviewerUserId, now, reason, reason, appointmentId, clinicId);
            case "REJECT_BY_CLINIC":
                sql = "UPDATE appointments SET status = 'REJECTED_BY_CLINIC', reviewed_by_user_id = ?, reviewed_at = ?, review_reason = ?, checked_in_at = NULL, completed_at = NULL, cancelled_at = ?, cancellation_reason = ?, outcome_notes = ? WHERE appointment_id = ? AND clinic_id = ?";
                return executeAppointmentUpdate(sql, reviewerUserId, now, reason, now, reason, reason, appointmentId, clinicId);
            case "CHECK_IN":
                sql = "UPDATE appointments SET status = 'ARRIVED', reviewed_by_user_id = ?, reviewed_at = ?, review_reason = ?, checked_in_at = ?, completed_at = NULL, cancelled_at = NULL, cancellation_reason = NULL, outcome_notes = ? WHERE appointment_id = ? AND clinic_id = ?";
                return executeAppointmentUpdate(sql, reviewerUserId, now, reason, now, reason, appointmentId, clinicId);
            case "COMPLETE":
                sql = "UPDATE appointments SET status = 'COMPLETED', reviewed_by_user_id = ?, reviewed_at = ?, review_reason = ?, completed_at = ?, cancelled_at = NULL, cancellation_reason = NULL, outcome_notes = ? WHERE appointment_id = ? AND clinic_id = ?";
                return executeAppointmentUpdate(sql, reviewerUserId, now, reason, now, reason, appointmentId, clinicId);
            case "NO_SHOW":
                sql = "UPDATE appointments SET status = 'NO_SHOW', reviewed_by_user_id = ?, reviewed_at = ?, review_reason = ?, checked_in_at = NULL, completed_at = NULL, cancelled_at = NULL, cancellation_reason = NULL, outcome_notes = ? WHERE appointment_id = ? AND clinic_id = ?";
                return executeAppointmentUpdate(sql, reviewerUserId, now, reason, reason, appointmentId, clinicId);
            case "CANCEL_BY_CLINIC":
                sql = "UPDATE appointments SET status = 'CANCELLED_BY_CLINIC', reviewed_by_user_id = ?, reviewed_at = ?, review_reason = ?, checked_in_at = NULL, completed_at = NULL, cancelled_at = ?, cancellation_reason = ?, outcome_notes = ? WHERE appointment_id = ? AND clinic_id = ?";
                return executeAppointmentUpdate(sql, reviewerUserId, now, reason, now, reason, reason, appointmentId, clinicId);
            default:
                throw new IllegalArgumentException("Unsupported update action: " + updateAction);
        }
    }

    private boolean executeAppointmentUpdate(String sql, Object... parameters) throws SQLException {
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                Object parameter = parameters[index];
                int parameterIndex = index + 1;
                if (parameter instanceof Integer) {
                    statement.setInt(parameterIndex, (Integer) parameter);
                } else if (parameter instanceof Timestamp) {
                    statement.setTimestamp(parameterIndex, (Timestamp) parameter);
                } else if (parameter instanceof String) {
                    statement.setString(parameterIndex, (String) parameter);
                } else if (parameter == null) {
                    statement.setObject(parameterIndex, null);
                } else {
                    statement.setObject(parameterIndex, parameter);
                }
            }
            return statement.executeUpdate() > 0;
        }
    }

    private Appointment mapAppointment(ResultSet rs) throws SQLException {
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(rs.getInt("appointment_id"));
        appointment.setPatientName(rs.getString("patient_name"));
        appointment.setClinicName(rs.getString("clinic_name"));
        appointment.setServiceName(rs.getString("service_name"));

        Date slotDate = rs.getDate("slot_date");
        Time startTime = rs.getTime("start_time");
        Time endTime = rs.getTime("end_time");
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        Timestamp checkedInAt = rs.getTimestamp("checked_in_at");
        Timestamp completedAt = rs.getTimestamp("completed_at");
        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");

        if (slotDate != null) {
            appointment.setSlotDate(slotDate.toLocalDate());
        }
        if (startTime != null) {
            appointment.setStartTime(startTime.toLocalTime());
        }
        if (endTime != null) {
            appointment.setEndTime(endTime.toLocalTime());
        }

        appointment.setStatus(rs.getString("status"));
        appointment.setReviewedBy(rs.getString("reviewed_by_name"));
        appointment.setReviewedAt(toLocalDateTime(reviewedAt));
        appointment.setReviewReason(rs.getString("review_reason"));
        appointment.setCheckedInAt(toLocalDateTime(checkedInAt));
        appointment.setCompletedAt(toLocalDateTime(completedAt));
        appointment.setCancelledAt(toLocalDateTime(cancelledAt));
        appointment.setCancellationReason(rs.getString("cancellation_reason"));
        appointment.setOutcomeNotes(rs.getString("outcome_notes"));
        appointment.setActionSummary(deriveActionSummary(appointment));
        return appointment;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String deriveActionSummary(Appointment appointment) {
        String status = appointment.getStatus();
        if (status == null) {
            return "View";
        }

        switch (status) {
            case "PENDING":
                return "Approve | Reject";
            case "CONFIRMED":
                return "Check-in | Cancel";
            case "ARRIVED":
                return "Complete | No-show";
            case "COMPLETED":
                return hasText(appointment.getOutcomeNotes()) ? appointment.getOutcomeNotes() : "View outcome";
            case "REJECTED_BY_CLINIC":
                return hasText(appointment.getReviewReason()) ? appointment.getReviewReason() : "Review rejection";
            case "CANCELLED_BY_PATIENT":
                return hasText(appointment.getCancellationReason()) ? appointment.getCancellationReason() : "Rebook";
            case "CANCELLED_BY_CLINIC":
                return hasText(appointment.getCancellationReason()) ? appointment.getCancellationReason() : "Review cancellation";
            case "NO_SHOW":
                return "Create follow-up";
            default:
                return "View";
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultReason(String normalizedAction) {
        switch (normalizedAction) {
            case "APPROVE":
                return "Approved by staff";
            case "REJECT_BY_CLINIC":
                return "Rejected by clinic";
            case "CHECK_IN":
                return "Checked in by staff";
            case "COMPLETE":
                return "Completed by staff";
            case "NO_SHOW":
                return "Marked no-show by staff";
            case "CANCEL_BY_CLINIC":
                return "Cancelled by clinic";
            default:
                return "Updated by staff";
        }
    }
}