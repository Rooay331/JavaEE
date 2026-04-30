package ict.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ict.bean.ClinicServiceStatus;
import ict.bean.ReportSummary;
import ict.bean.ServiceUtilization;

public class ReportDB {

    private final String url;
    private final String username;
    private final String password;

    public ReportDB(String url, String username, String password) {
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

    public ReportSummary findSummary(Integer clinicId, LocalDate fromDate, LocalDate toDate) throws SQLException {
        ReportSummary summary = new ReportSummary();
        summary.setFromDate(fromDate);
        summary.setToDate(toDate);
        summary.setPeriodLabel(fromDate != null && toDate != null && fromDate.equals(toDate) ? "Daily Summary" : "Range Summary");

        if (fromDate == null || toDate == null) {
            return summary;
        }

        summary.setDailyCompletedAppointments(countAppointmentsByStatus(clinicId, fromDate, toDate, "COMPLETED"));
        summary.setDailyNoShowCount(countAppointmentsByStatus(clinicId, fromDate, toDate, "NO_SHOW"));
        summary.setWeeklyServedQueueTickets(countQueueTicketsByStatus(clinicId, fromDate, toDate, "SERVED"));

        List<ServiceUtilization> serviceUtilization = findServiceUtilization(clinicId, fromDate, toDate);
        int totalCapacity = 0;
        int totalBooked = 0;
        for (ServiceUtilization row : serviceUtilization) {
            totalCapacity += row.getCapacity();
            totalBooked += row.getBooked();
        }
        summary.setServiceUtilizationAverage(totalCapacity <= 0 ? 0 : (int) Math.ceil((totalBooked * 100.0) / totalCapacity));

        return summary;
    }

    public List<ServiceUtilization> findServiceUtilization(Integer clinicId, LocalDate fromDate, LocalDate toDate)
            throws SQLException {
        List<ServiceUtilization> utilisation = new ArrayList<>();
        if (fromDate == null || toDate == null) {
            return utilisation;
        }

        ServiceDB serviceDB = new ServiceDB(url, username, password);
        List<ClinicServiceStatus> services = (clinicId == null || clinicId <= 0) 
                ? findAllClinicServiceStatuses() 
                : serviceDB.findClinicServiceStatuses(clinicId);
        
        Map<String, Integer> bookedCounts = loadBookedAppointmentCounts(clinicId, fromDate, toDate);

        for (ClinicServiceStatus service : services) {
            if (service == null || !service.isActive()) {
                continue;
            }

            Map<Integer, List<OpeningHoursWindow>> openingHours = loadOpeningHours(service.getServiceId());
            int capacity = 0;
            int serviceMinutes = Math.max(service.getAvgServiceMinutes(), 1);
            int slotCapacity = service.getDefaultCapacity() == null ? 1 : Math.max(service.getDefaultCapacity(), 0);

            LocalDate currentDate = fromDate;
            while (!currentDate.isAfter(toDate)) {
                List<OpeningHoursWindow> hoursForDay = openingHours.get(currentDate.getDayOfWeek().getValue() - 1);
                if (hoursForDay != null) {
                    for (OpeningHoursWindow hours : hoursForDay) {
                        if (hours == null || hours.openTime == null || hours.closeTime == null) {
                            continue;
                        }

                        long windowMinutes = Duration.between(hours.openTime, hours.closeTime).toMinutes();
                        int slotsPerDay = (int) Math.max(windowMinutes / serviceMinutes, 0);
                        capacity += slotsPerDay * slotCapacity;
                    }
                }
                currentDate = currentDate.plusDays(1);
            }

            String key = service.getClinicId() + "-" + service.getServiceId();
            int booked = bookedCounts.getOrDefault(key, 0);

            ServiceUtilization row = new ServiceUtilization();
            row.setClinicName(service.getClinicName());
            row.setServiceName(service.getServiceName());
            row.setCapacity(capacity);
            row.setBooked(booked);
            int utilisationPercent = capacity <= 0 ? 0 : (int) Math.ceil((booked * 100.0) / capacity);
            row.setUtilisationPercent(utilisationPercent);
            utilisation.add(row);
        }

        return utilisation;
    }

    private List<ClinicServiceStatus> findAllClinicServiceStatuses() throws SQLException {
        ServiceDB serviceDB = new ServiceDB(url, username, password);
        return serviceDB.findAllClinicServiceStatuses();
    }

    private int countAppointmentsByStatus(Integer clinicId, LocalDate fromDate, LocalDate toDate, String status)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total FROM appointments a WHERE a.appointment_date BETWEEN ? AND ? AND a.status = ?");
        if (clinicId != null && clinicId > 0) {
            sql.append(" AND a.clinic_id = ?");
        }

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setDate(1, Date.valueOf(fromDate));
            statement.setDate(2, Date.valueOf(toDate));
            statement.setString(3, status);
            if (clinicId != null && clinicId > 0) {
                statement.setInt(4, clinicId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }

    private int countQueueTicketsByStatus(Integer clinicId, LocalDate fromDate, LocalDate toDate, String status)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total FROM queue_tickets qt INNER JOIN clinic_services cs ON qt.clinic_service_id = cs.clinic_service_id ");
        sql.append("WHERE qt.queue_date BETWEEN ? AND ? AND qt.status = ?");
        if (clinicId != null && clinicId > 0) {
            sql.append(" AND cs.clinic_id = ?");
        }

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setDate(1, Date.valueOf(fromDate));
            statement.setDate(2, Date.valueOf(toDate));
            statement.setString(3, status);
            if (clinicId != null && clinicId > 0) {
                statement.setInt(4, clinicId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }

    private Map<Integer, List<OpeningHoursWindow>> loadOpeningHours(Integer serviceId) throws SQLException {
        Map<Integer, List<OpeningHoursWindow>> openingHours = new LinkedHashMap<>();
        String sql = "SELECT day_of_week, open_time, close_time FROM opening_hours WHERE service_id = ? ORDER BY day_of_week, open_time";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, serviceId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    OpeningHoursWindow window = new OpeningHoursWindow();
                    Time openTime = rs.getTime("open_time");
                    Time closeTime = rs.getTime("close_time");
                    if (openTime != null) {
                        window.openTime = openTime.toLocalTime();
                    }
                    if (closeTime != null) {
                        window.closeTime = closeTime.toLocalTime();
                    }

                    int dayOfWeek = rs.getInt("day_of_week");
                    List<OpeningHoursWindow> dayWindows = openingHours.get(dayOfWeek);
                    if (dayWindows == null) {
                        dayWindows = new ArrayList<>();
                        openingHours.put(dayOfWeek, dayWindows);
                    }
                    dayWindows.add(window);
                }
            }
        }

        return openingHours;
    }

    private Map<String, Integer> loadBookedAppointmentCounts(Integer clinicId, LocalDate fromDate, LocalDate toDate)
            throws SQLException {
        Map<String, Integer> bookedCounts = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("SELECT a.clinic_id, a.service_id, COUNT(*) AS booked FROM appointments a ");
        sql.append("WHERE a.appointment_date BETWEEN ? AND ? AND a.status IN ('PENDING', 'CONFIRMED', 'ARRIVED', 'COMPLETED') ");
        if (clinicId != null && clinicId > 0) {
            sql.append("AND a.clinic_id = ? ");
        }
        sql.append("GROUP BY a.clinic_id, a.service_id");

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setDate(1, Date.valueOf(fromDate));
            statement.setDate(2, Date.valueOf(toDate));
            if (clinicId != null && clinicId > 0) {
                statement.setInt(3, clinicId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getInt("clinic_id") + "-" + rs.getInt("service_id");
                    bookedCounts.put(key, rs.getInt("booked"));
                }
            }
        }

        return bookedCounts;
    }

    private static class OpeningHoursWindow {
        private LocalTime openTime;
        private LocalTime closeTime;
    }
}