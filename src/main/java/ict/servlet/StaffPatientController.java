package ict.servlet;

import ict.bean.Appointment;
import ict.bean.QueueTicket;
import ict.bean.PatientSearchResult;
import ict.bean.User;
import ict.db.AppointmentDB;
import ict.db.ClinicDB;
import ict.db.PatientDB;
import ict.db.NotificationDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(urlPatterns = {"/staff/patients/search", "/staff/patients/view", "/staff/patients/checkin"})
public class StaffPatientController extends HttpServlet {

    private PatientDB patientDB;
    private AppointmentDB appointmentDB;
    private ClinicDB clinicDB;
    private NotificationDB notificationDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        patientDB = new PatientDB(dbUrl, dbUser, dbPassword);
        appointmentDB = new AppointmentDB(dbUrl, dbUser, dbPassword);
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

        String servletPath = request.getServletPath();
        if (!"/staff/patients/checkin".equals(servletPath)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String patientIdText = normalize(request.getParameter("patientId"));
        String appointmentIdText = normalize(request.getParameter("appointmentId"));
        String result = normalize(request.getParameter("result"));
        String note = normalize(request.getParameter("note"));

        Integer appointmentId = parseInteger(appointmentIdText);
        String flashMessage;
        String flashType;

        try {
            if (appointmentId == null || result == null) {
                flashMessage = "Please provide an appointment ID and a result.";
                flashType = "error";
            } else {
                String action = mapCheckInAction(result);
                if (action == null) {
                    flashMessage = "Unsupported check-in result.";
                    flashType = "error";
                } else {
                    boolean updated = appointmentDB.updateAppointmentForClinic(
                            staffUser.getClinicId(),
                            staffUser.getUserId(),
                            appointmentId,
                            action,
                            note);
                    if (updated) {
                        sendCheckInNotification(staffUser, appointmentId, patientIdText, result, note);
                    }
                    flashMessage = updated ? "Appointment status updated successfully." : "Unable to update the appointment status.";
                    flashType = updated ? "success" : "error";
                }
            }
        } catch (Exception ex) {
            flashMessage = "Unable to update the appointment status from the database.";
            flashType = "error";
            ex.printStackTrace();
        }

        request.setAttribute("patientId", patientIdText);
        request.setAttribute("appointmentId", appointmentIdText);
        request.setAttribute("result", result);
        request.setAttribute("note", note);
        handlePage(request, response, flashMessage, flashType);
    }

    private void handlePage(HttpServletRequest request, HttpServletResponse response, String flashMessage, String flashType)
            throws ServletException, IOException {
        User staffUser = getLoggedInStaffUser(request, response);
        if (staffUser == null) {
            return;
        }

        String servletPath = request.getServletPath();
        if ("/staff/patients/search".equals(servletPath)) {
            loadSearchPage(request, response, staffUser, flashMessage, flashType);
        } else if ("/staff/patients/view".equals(servletPath)) {
            loadViewPage(request, response, staffUser, flashMessage, flashType);
        } else if ("/staff/patients/checkin".equals(servletPath)) {
            loadCheckInPage(request, response, staffUser, flashMessage, flashType);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void loadSearchPage(HttpServletRequest request, HttpServletResponse response, User staffUser, String flashMessage, String flashType) {
        String keyword = normalize(request.getParameter("keyword"));
        String patientIdText = normalize(request.getParameter("patientId"));
        String status = normalize(request.getParameter("status"));
        Integer patientId = parseInteger(patientIdText);

        List<PatientSearchResult> searchResults = Collections.emptyList();
        String errorMessage = null;

        try {
            searchResults = patientDB.searchPatients(staffUser.getClinicId(), keyword, patientId, status);
        } catch (Exception ex) {
            errorMessage = "Unable to load patient search results from the database.";
            ex.printStackTrace();
        }

        request.setAttribute("searchResults", searchResults);
        request.setAttribute("searchError", errorMessage);
        request.setAttribute("selectedKeyword", keyword);
        request.setAttribute("selectedPatientId", patientIdText);
        request.setAttribute("selectedStatus", status);
        request.setAttribute("assignedClinicId", staffUser.getClinicId());
        request.setAttribute("assignedClinicName", findClinicName(staffUser.getClinicId()));
        request.setAttribute("activeStaffPath", "/staff/patients/search");
        if (flashMessage != null) {
            request.setAttribute("flashMessage", flashMessage);
            request.setAttribute("flashType", flashType);
        }
        forward(request, response, "/staff/patients/search.jsp");
    }

    private void loadViewPage(HttpServletRequest request, HttpServletResponse response, User staffUser, String flashMessage, String flashType) {
        Integer patientId = parseInteger(normalize(request.getParameter("patientId")));
        if (patientId == null) {
            request.setAttribute("flashMessage", "Please select a patient from the search page first.");
            request.setAttribute("flashType", "error");
            request.setAttribute("activeStaffPath", "/staff/patients/view");
            request.setAttribute("assignedClinicId", staffUser.getClinicId());
            request.setAttribute("assignedClinicName", findClinicName(staffUser.getClinicId()));
            forward(request, response, "/staff/patients/view.jsp");
            return;
        }

        loadPatientDetail(request, staffUser, patientId, flashMessage, flashType, "/staff/patients/view");
        forward(request, response, "/staff/patients/view.jsp");
    }

    private void loadCheckInPage(HttpServletRequest request, HttpServletResponse response, User staffUser, String flashMessage, String flashType) {
        Integer patientId = parseInteger(normalize(request.getParameter("patientId")));
        Integer appointmentId = parseInteger(normalize(request.getParameter("appointmentId")));
        if (patientId != null) {
            loadPatientDetail(request, staffUser, patientId, flashMessage, flashType, "/staff/patients/checkin");
        } else {
            request.setAttribute("assignedClinicId", staffUser.getClinicId());
            request.setAttribute("assignedClinicName", findClinicName(staffUser.getClinicId()));
            request.setAttribute("activeStaffPath", "/staff/patients/checkin");
        }

        request.setAttribute("patientId", patientId == null ? request.getParameter("patientId") : String.valueOf(patientId));
        request.setAttribute("appointmentId", appointmentId == null ? request.getParameter("appointmentId") : String.valueOf(appointmentId));
        if (flashMessage != null) {
            request.setAttribute("flashMessage", flashMessage);
            request.setAttribute("flashType", flashType);
        }
        forward(request, response, "/staff/patients/checkin.jsp");
    }

    private void loadPatientDetail(HttpServletRequest request, User staffUser, Integer patientId,
            String flashMessage, String flashType, String activePath) {
        String errorMessage = null;
        User patientUser = null;
        QueueTicket activeQueueTicket = null;
        List<Appointment> recentAppointments = Collections.emptyList();

        try {
            patientUser = patientDB.findPatientById(patientId);
            if (patientUser == null) {
                errorMessage = "Patient record not found.";
            } else {
                activeQueueTicket = patientDB.findActiveQueueTicket(staffUser.getClinicId(), patientId);
                recentAppointments = appointmentDB.findAppointmentsByPatient(patientId, staffUser.getClinicId(), 5);
            }
        } catch (Exception ex) {
            errorMessage = "Unable to load patient details from the database.";
            ex.printStackTrace();
        }

        request.setAttribute("patientUser", patientUser);
        request.setAttribute("activeQueueTicket", activeQueueTicket);
        request.setAttribute("recentAppointments", recentAppointments);
        request.setAttribute("patientError", errorMessage);
        request.setAttribute("patientId", String.valueOf(patientId));
        request.setAttribute("assignedClinicId", staffUser.getClinicId());
        request.setAttribute("assignedClinicName", findClinicName(staffUser.getClinicId()));
        request.setAttribute("activeStaffPath", activePath);
        if (flashMessage != null) {
            request.setAttribute("flashMessage", flashMessage);
            request.setAttribute("flashType", flashType);
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String jspPath) {
        try {
            request.getRequestDispatcher(jspPath).forward(request, response);
        } catch (ServletException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void sendCheckInNotification(User staffUser, Integer appointmentId, String patientIdText, String result, String note) {
        if (staffUser == null || staffUser.getUserId() == null || appointmentId == null) {
            return;
        }

        String normalizedResult = result == null ? "" : result.trim().toUpperCase();
        String notificationType = mapCheckInNotificationType(normalizedResult);
        String appointmentLabel = "A-" + appointmentId;
        String patientLabel = patientIdText == null || patientIdText.trim().isEmpty() ? "the patient" : "patient " + patientIdText.trim();

        String title;
        String body;
        switch (notificationType) {
            case "NO_SHOW_ALERT":
                title = "No-show alert - " + appointmentLabel;
                body = "Appointment " + appointmentLabel + " for " + patientLabel + " was marked as no-show.";
                break;
            case "APPOINTMENT_UPDATED":
            default:
                title = "Appointment updated - " + appointmentLabel;
                body = "Appointment " + appointmentLabel + " for " + patientLabel + " was marked as " + normalizedResult.toLowerCase() + ".";
                break;
        }

        if (note != null && !note.trim().isEmpty()) {
            body = body + " Note: " + note.trim();
        }

        try {
            notificationDB.createNotification(staffUser.getUserId(), notificationType, title, body, appointmentId, null, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String mapCheckInNotificationType(String result) {
        if (result == null) {
            return "APPOINTMENT_UPDATED";
        }

        switch (result.trim().toUpperCase()) {
            case "NO_SHOW":
                return "NO_SHOW_ALERT";
            case "ARRIVED":
            case "COMPLETED":
                return "APPOINTMENT_UPDATED";
            default:
                return "APPOINTMENT_UPDATED";
        }
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

    private String mapCheckInAction(String result) {
        if (result == null) {
            return null;
        }

        switch (result.trim().toUpperCase()) {
            case "ARRIVED":
                return "CHECK_IN";
            case "NO_SHOW":
                return "NO_SHOW";
            case "COMPLETED":
                return "COMPLETE";
            default:
                return null;
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
}