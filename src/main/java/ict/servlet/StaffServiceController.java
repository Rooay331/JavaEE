package ict.servlet;

import ict.bean.ClinicServiceStatus;
import ict.bean.IncidentLog;
import ict.bean.User;
import ict.db.ClinicDB;
import ict.db.NotificationDB;
import ict.db.ServiceDB;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/staff/services/issues"})
public class StaffServiceController extends HttpServlet {

    private ServiceDB serviceDB;
    private ClinicDB clinicDB;
    private NotificationDB notificationDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        serviceDB = new ServiceDB(dbUrl, dbUser, dbPassword);
        clinicDB = new ClinicDB(dbUrl, dbUser, dbPassword);
        notificationDB = new NotificationDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handlePage(request, response, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User staffUser = getLoggedInStaffUser(request, response);
        if (staffUser == null) {
            return;
        }

        String flashMessage;
        String flashType;

        try {
            flashMessage = handleIssueCreate(request, staffUser);
            if (flashMessage == null) {
                flashMessage = "No changes were made.";
                flashType = "error";
            } else {
                flashType = isSuccessMessage(flashMessage) ? "success" : "error";
            }
        } catch (Exception ex) {
            flashMessage = "Unable to update service records from the database.";
            flashType = "error";
            ex.printStackTrace();
        }

        handlePage(request, response, flashMessage, flashType);
    }

    private void handlePage(HttpServletRequest request, HttpServletResponse response, String flashMessage, String flashType)
            throws ServletException, IOException {
        User staffUser = getLoggedInStaffUser(request, response);
        if (staffUser == null) {
            return;
        }

        loadIssuesPage(request, response, staffUser, flashMessage, flashType);
    }

    private void loadIssuesPage(HttpServletRequest request, HttpServletResponse response, User staffUser, String flashMessage, String flashType) {
        List<ClinicServiceStatus> serviceStatuses = Collections.emptyList();
        List<IncidentLog> incidents = Collections.emptyList();
        String errorMessage = null;

        try {
            serviceStatuses = serviceDB.findClinicServiceStatuses(staffUser.getClinicId());
            incidents = serviceDB.findIncidentLogsByClinic(
                    staffUser.getClinicId(),
                    normalize(request.getParameter("severityFilter")),
                    normalize(request.getParameter("statusFilter")),
                    20);
        } catch (Exception ex) {
            errorMessage = "Unable to load incident records from the database.";
            ex.printStackTrace();
        }

        request.setAttribute("serviceStatuses", serviceStatuses);
        request.setAttribute("incidents", incidents);
        request.setAttribute("incidentError", errorMessage);
        request.setAttribute("assignedClinicId", staffUser.getClinicId());
        request.setAttribute("assignedClinicName", findClinicName(staffUser.getClinicId()));
        request.setAttribute("activeStaffPath", "/staff/services/issues");
        if (flashMessage != null) {
            request.setAttribute("flashMessage", flashMessage);
            request.setAttribute("flashType", flashType);
        }
        try {
            request.getRequestDispatcher("/staff/services/issues.jsp").forward(request, response);
        } catch (ServletException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String handleIssueCreate(HttpServletRequest request, User staffUser) throws Exception {
        String serviceIdText = normalize(request.getParameter("serviceId"));
        String severity = normalize(request.getParameter("severity"));
        String title = normalize(request.getParameter("title"));
        String description = normalize(request.getParameter("description"));
        String occurredAtText = normalize(request.getParameter("occurredAt"));

        Integer serviceId = parseInteger(serviceIdText);
        LocalDateTime occurredAt = parseDateTime(occurredAtText);
        if (severity == null || title == null || description == null || occurredAt == null) {
            return "Please complete all incident fields before submitting.";
        }

        Integer incidentId = serviceDB.createIncidentLog(staffUser.getUserId(), staffUser.getClinicId(), serviceId, severity, title, description, occurredAt);
        if (incidentId != null) {
            sendServiceNotification(
                    staffUser,
                    "INCIDENT_REPORTED",
                    "Clinic incident reported - " + title,
                    buildIssueBody(title, description, staffUser.getClinicId()),
                    null,
                    incidentId);
            return "Operational issue submitted successfully.";
        }
        return "Unable to submit the operational issue.";
    }

    private void sendServiceNotification(User staffUser, String notificationType, String title, String body,
            Integer appointmentId, Integer incidentId) {
        if (staffUser == null || staffUser.getUserId() == null) {
            return;
        }

        try {
            notificationDB.createNotification(staffUser.getUserId(), notificationType, title, body, appointmentId, null, incidentId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String buildIssueBody(String title, String description, Integer clinicId) {
        StringBuilder body = new StringBuilder();
        body.append(title).append(" was reported for ").append(findClinicName(clinicId)).append('.');
        if (description != null && !description.trim().isEmpty()) {
            body.append(' ').append(description.trim());
        }
        return body.toString();
    }

    private User getLoggedInStaffUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        Object sessionUser = session.getAttribute("userInfo");
        if (!(sessionUser instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        User staffUser = (User) sessionUser;
        if (!"STAFF".equalsIgnoreCase(staffUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        if (staffUser.getClinicId() == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Staff account is missing an assigned clinic.");
            return null;
        }

        return staffUser;
    }

    private String findClinicName(Integer clinicId) {
        try {
            String clinicName = clinicDB.findClinicNameById(clinicId);
            return clinicName == null ? "Clinic ID " + clinicId : clinicName;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Clinic ID " + clinicId;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }

        try {
            return Integer.valueOf(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private boolean isSuccessMessage(String message) {
        if (message == null) {
            return false;
        }

        String normalized = message.trim().toLowerCase();
        return !(normalized.startsWith("unable") || normalized.startsWith("please") || normalized.startsWith("no changes"));
    }
}