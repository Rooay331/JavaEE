package ict.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ict.bean.Appointment;
import ict.bean.ClinicService;
import ict.bean.IncidentLog;
import ict.bean.Notification;
import ict.db.AppointmentDB;
import ict.db.ClinicServiceDB;
import ict.db.NotificationDB;
import ict.db.ServiceDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/admin/audit/users", "/admin/audit/users.html", "/admin/audit/appointments", "/admin/audit/appointments.html", "/admin/audit/config", "/admin/audit/config.html"})
public class AdminAuditController extends AdminControllerSupport {

    private AppointmentDB appointmentDB;
    private NotificationDB notificationDB;
    private ServiceDB serviceDB;
    private ClinicServiceDB clinicServiceDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        appointmentDB = new AppointmentDB(dbUrl, dbUser, dbPassword);
        notificationDB = new NotificationDB(dbUrl, dbUser, dbPassword);
        serviceDB = new ServiceDB(dbUrl, dbUser, dbPassword);
        clinicServiceDB = new ClinicServiceDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (getLoggedInAdminUser(request, response) == null) {
            return;
        }

        String servletPath = request.getServletPath();
        int selectedYear = resolveYear(request.getParameter("year"));
        int selectedMonth = resolveMonth(request.getParameter("month"));
        YearMonth selectedPeriod = YearMonth.of(selectedYear, selectedMonth);
        LocalDate fromDate = selectedPeriod.atDay(1);
        LocalDate toDate = selectedPeriod.atEndOfMonth();
        Integer selectedClinicId = parseInteger(request.getParameter("clinicId"));

        try {
            List<ClinicService> activeClinics = clinicServiceDB.findActiveClinics();
            request.setAttribute("activeClinics", activeClinics);
            request.setAttribute("selectedClinicId", selectedClinicId);
            request.setAttribute("selectedMonth", selectedMonth);
            request.setAttribute("selectedYear", selectedYear);
            request.setAttribute("reportMonthLabel", selectedPeriod.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + selectedYear);

            if (isUsersPath(servletPath)) {
                List<Notification> notifications = loadNotifications(activeClinics, selectedClinicId, selectedYear, selectedMonth);
                request.setAttribute("notificationRecords", notifications);
                request.setAttribute("notificationCount", notifications.size());
                request.setAttribute("unreadNotificationCount", countUnreadNotifications(notifications));
                request.setAttribute("accountAlertCount", countNotificationsByType(notifications, "ACCOUNT_ALERT"));
                request.setAttribute("activeAdminPath", "/admin/audit/users");
                request.getRequestDispatcher("/admin/audit/users.jsp").forward(request, response);
                return;
            }

            if (isAppointmentsPath(servletPath)) {
                List<Appointment> appointments = loadAppointments(activeClinics, selectedClinicId, fromDate, toDate);
                request.setAttribute("appointmentRecords", appointments);
                request.setAttribute("appointmentCount", appointments.size());
                request.setAttribute("completedAppointmentCount", countAppointmentsByStatus(appointments, "COMPLETED"));
                request.setAttribute("noShowAppointmentCount", countAppointmentsByStatus(appointments, "NO_SHOW"));
                request.setAttribute("cancelledAppointmentCount", countCancelledAppointments(appointments));
                request.setAttribute("activeAdminPath", "/admin/audit/appointments");
                request.getRequestDispatcher("/admin/audit/appointments.jsp").forward(request, response);
                return;
            }

            if (isConfigPath(servletPath)) {
                List<IncidentLog> incidents = loadIncidents(activeClinics, selectedClinicId, selectedYear, selectedMonth);
                request.setAttribute("incidentRecords", incidents);
                request.setAttribute("incidentCount", incidents.size());
                request.setAttribute("openIncidentCount", countIncidentsByStatus(incidents, "OPEN") + countIncidentsByStatus(incidents, "IN_PROGRESS"));
                request.setAttribute("resolvedIncidentCount", countIncidentsByStatus(incidents, "RESOLVED") + countIncidentsByStatus(incidents, "CLOSED"));
                request.setAttribute("criticalIncidentCount", countIncidentsBySeverity(incidents, "CRITICAL") + countIncidentsBySeverity(incidents, "HIGH"));
                request.setAttribute("activeAdminPath", "/admin/audit/config");
                request.getRequestDispatcher("/admin/audit/config.jsp").forward(request, response);
                return;
            }

            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException ex) {
            request.setAttribute("reportError", "Unable to load audit data from the database.");
            request.setAttribute("activeAdminPath", servletPath);
            getServletContext().log("Unable to load audit data from the database.", ex);
            if (isUsersPath(servletPath)) {
                request.setAttribute("notificationRecords", Collections.emptyList());
                request.setAttribute("notificationCount", 0);
                request.setAttribute("unreadNotificationCount", 0);
                request.setAttribute("accountAlertCount", 0);
                request.getRequestDispatcher("/admin/audit/users.jsp").forward(request, response);
                return;
            }
            if (isAppointmentsPath(servletPath)) {
                request.setAttribute("appointmentRecords", Collections.emptyList());
                request.setAttribute("appointmentCount", 0);
                request.setAttribute("completedAppointmentCount", 0);
                request.setAttribute("noShowAppointmentCount", 0);
                request.setAttribute("cancelledAppointmentCount", 0);
                request.getRequestDispatcher("/admin/audit/appointments.jsp").forward(request, response);
                return;
            }
            if (isConfigPath(servletPath)) {
                request.setAttribute("incidentRecords", Collections.emptyList());
                request.setAttribute("incidentCount", 0);
                request.setAttribute("openIncidentCount", 0);
                request.setAttribute("resolvedIncidentCount", 0);
                request.setAttribute("criticalIncidentCount", 0);
                request.getRequestDispatcher("/admin/audit/config.jsp").forward(request, response);
            }
        }
    }

    private List<Notification> loadNotifications(List<ClinicService> activeClinics, Integer selectedClinicId, int selectedYear, int selectedMonth)
            throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        for (ClinicService clinic : resolveClinics(activeClinics, selectedClinicId)) {
            if (clinic == null || clinic.getClinicId() == null) {
                continue;
            }
            for (Notification notification : notificationDB.findNotificationsByClinic(clinic.getClinicId())) {
                if (notification != null && matchesPeriod(notification.getCreatedAt(), selectedYear, selectedMonth)) {
                    notifications.add(notification);
                }
            }
        }

        notifications.sort((left, right) -> {
            int createdCompare = compareDateTime(right.getCreatedAt(), left.getCreatedAt());
            if (createdCompare != 0) {
                return createdCompare;
            }
            return compareInteger(right.getNotificationId(), left.getNotificationId());
        });

        return notifications;
    }

    private List<Appointment> loadAppointments(List<ClinicService> activeClinics, Integer selectedClinicId, LocalDate fromDate, LocalDate toDate)
            throws SQLException {
        List<Appointment> appointments = new ArrayList<>();
        for (ClinicService clinic : resolveClinics(activeClinics, selectedClinicId)) {
            if (clinic == null || clinic.getClinicId() == null) {
                continue;
            }
            appointments.addAll(appointmentDB.findAppointmentsByClinic(clinic.getClinicId(), null, null, null, fromDate, toDate));
        }

        appointments.sort((left, right) -> {
            int dateCompare = compareDate(right.getSlotDate(), left.getSlotDate());
            if (dateCompare != 0) {
                return dateCompare;
            }
            int startCompare = compareTime(right.getStartTime(), left.getStartTime());
            if (startCompare != 0) {
                return startCompare;
            }
            return compareInteger(right.getAppointmentId(), left.getAppointmentId());
        });

        return appointments;
    }

    private List<IncidentLog> loadIncidents(List<ClinicService> activeClinics, Integer selectedClinicId, int selectedYear, int selectedMonth)
            throws SQLException {
        List<IncidentLog> incidents = new ArrayList<>();
        for (ClinicService clinic : resolveClinics(activeClinics, selectedClinicId)) {
            if (clinic == null || clinic.getClinicId() == null) {
                continue;
            }
            for (IncidentLog incident : serviceDB.findIncidentLogsByClinic(clinic.getClinicId(), null, null, 100)) {
                if (incident != null && matchesPeriod(incident.getOccurredAt(), selectedYear, selectedMonth)) {
                    incidents.add(incident);
                }
            }
        }

        incidents.sort((left, right) -> {
            int occurredCompare = compareDateTime(right.getOccurredAt(), left.getOccurredAt());
            if (occurredCompare != 0) {
                return occurredCompare;
            }
            return compareInteger(right.getIncidentId(), left.getIncidentId());
        });

        return incidents;
    }

    private List<ClinicService> resolveClinics(List<ClinicService> activeClinics, Integer selectedClinicId) {
        if (selectedClinicId == null || selectedClinicId <= 0) {
            return activeClinics == null ? Collections.emptyList() : activeClinics;
        }

        List<ClinicService> selectedClinics = new ArrayList<>();
        if (activeClinics != null) {
            for (ClinicService clinic : activeClinics) {
                if (clinic != null && selectedClinicId.equals(clinic.getClinicId())) {
                    selectedClinics.add(clinic);
                    return selectedClinics;
                }
            }
        }

        return selectedClinics;
    }

    private boolean matchesPeriod(LocalDateTime timestamp, int selectedYear, int selectedMonth) {
        return timestamp != null && timestamp.getYear() == selectedYear && timestamp.getMonthValue() == selectedMonth;
    }

    private int countUnreadNotifications(List<Notification> notifications) {
        int count = 0;
        if (notifications == null) {
            return count;
        }

        for (Notification notification : notifications) {
            if (notification != null && !notification.isRead()) {
                count++;
            }
        }
        return count;
    }

    private int countNotificationsByType(List<Notification> notifications, String type) {
        int count = 0;
        if (notifications == null || type == null) {
            return count;
        }

        for (Notification notification : notifications) {
            if (notification != null && type.equalsIgnoreCase(notification.getType())) {
                count++;
            }
        }
        return count;
    }

    private int countAppointmentsByStatus(List<Appointment> appointments, String status) {
        int count = 0;
        if (appointments == null || status == null) {
            return count;
        }

        for (Appointment appointment : appointments) {
            if (appointment != null && status.equalsIgnoreCase(appointment.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int countCancelledAppointments(List<Appointment> appointments) {
        int count = 0;
        if (appointments == null) {
            return count;
        }

        for (Appointment appointment : appointments) {
            if (appointment == null || appointment.getStatus() == null) {
                continue;
            }

            String status = appointment.getStatus().trim().toUpperCase(Locale.ENGLISH);
            if (status.startsWith("CANCELLED") || status.startsWith("REJECTED")) {
                count++;
            }
        }
        return count;
    }

    private int countIncidentsByStatus(List<IncidentLog> incidents, String status) {
        int count = 0;
        if (incidents == null || status == null) {
            return count;
        }

        for (IncidentLog incident : incidents) {
            if (incident != null && status.equalsIgnoreCase(incident.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int countIncidentsBySeverity(List<IncidentLog> incidents, String severity) {
        int count = 0;
        if (incidents == null || severity == null) {
            return count;
        }

        for (IncidentLog incident : incidents) {
            if (incident != null && severity.equalsIgnoreCase(incident.getSeverity())) {
                count++;
            }
        }
        return count;
    }

    private int compareInteger(Integer left, Integer right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private int compareDate(LocalDate left, LocalDate right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private int compareTime(java.time.LocalTime left, java.time.LocalTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private int compareDateTime(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private boolean isUsersPath(String servletPath) {
        return "/admin/audit/users".equals(servletPath) || "/admin/audit/users.html".equals(servletPath);
    }

    private boolean isAppointmentsPath(String servletPath) {
        return "/admin/audit/appointments".equals(servletPath) || "/admin/audit/appointments.html".equals(servletPath);
    }

    private boolean isConfigPath(String servletPath) {
        return "/admin/audit/config".equals(servletPath) || "/admin/audit/config.html".equals(servletPath);
    }

    private int resolveMonth(String value) {
        Integer parsed = parseInteger(value);
        if (parsed == null || parsed < 1 || parsed > 12) {
            return LocalDate.now().getMonthValue();
        }
        return parsed;
    }

    private int resolveYear(String value) {
        Integer parsed = parseInteger(value);
        if (parsed == null || parsed < 2000 || parsed > 2100) {
            return LocalDate.now().getYear();
        }
        return parsed;
    }
}