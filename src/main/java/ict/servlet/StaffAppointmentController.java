package ict.servlet;

import ict.bean.Appointment;
import ict.bean.User;
import ict.bean.StaffDashboardStats;
import ict.db.ClinicDB;
import ict.db.AppointmentDB;
import ict.db.NotificationDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebServlet(urlPatterns = {"/staff/dashboard", "/staff/appointments/manage"})
public class StaffAppointmentController extends HttpServlet {

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("d-M-yyyy");

    private AppointmentDB appointmentDB;
    private ClinicDB clinicDB;
    private NotificationDB notificationDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        appointmentDB = new AppointmentDB(dbUrl, dbUser, dbPassword);
        clinicDB = new ClinicDB(dbUrl, dbUser, dbPassword);
        notificationDB = new NotificationDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleListRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleUpdateRequest(request, response);
    }

    private void handleListRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User staffUser = getLoggedInStaffUser(request, response);
        if (staffUser == null) {
            return;
        }

        String servletPath = request.getServletPath();

        if ("/staff/dashboard".equals(servletPath)) {
            request.setAttribute("dashboardStats", buildDashboardStats(staffUser.getClinicId()));
            request.setAttribute("assignedClinicId", staffUser.getClinicId());
            request.setAttribute("assignedClinicName", findClinicName(staffUser.getClinicId()));
            request.setAttribute("activeStaffPath", "/staff/dashboard");
            request.getRequestDispatcher("/staff/dashboard.jsp").forward(request, response);
            return;
        }

        if ("/staff/appointments/manage".equals(servletPath)) {
            String selectedService = normalize(request.getParameter("service"));
            String selectedStatus = normalize(request.getParameter("status"));
            String selectedPatientNameFilter = normalize(request.getParameter("patientName"));
            String selectedDate = normalize(request.getParameter("date"));

            LocalDate dateFilter = parseDate(selectedDate);
            populateAppointmentView(request, staffUser, selectedService, selectedStatus, selectedPatientNameFilter, selectedDate, dateFilter, null, null, false);
            request.getRequestDispatcher("/staff/appointments.jsp").forward(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleUpdateRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User staffUser = getLoggedInStaffUser(request, response);
        if (staffUser == null) {
            return;
        }

        String selectedService = normalize(request.getParameter("service"));
        String selectedStatus = normalize(request.getParameter("status"));
        String selectedPatientNameFilter = normalize(request.getParameter("patientName"));
        String selectedDate = normalize(request.getParameter("date"));
        String appointmentIdText = normalize(request.getParameter("appointmentId"));
        String updateAction = normalize(request.getParameter("updateAction"));
        String remark = normalize(request.getParameter("remark"));
        String selectedAppointmentCode = normalize(request.getParameter("selectedAppointmentCode"));
        String selectedPatientName = normalize(request.getParameter("selectedPatientName"));
        String selectedServiceName = normalize(request.getParameter("selectedServiceName"));
        String selectedScheduleLabel = normalize(request.getParameter("selectedScheduleLabel"));

        LocalDate dateFilter = parseDate(selectedDate);
        Integer appointmentId = parseAppointmentId(appointmentIdText);

        String updateMessage;
        String updateMessageType;
        boolean updatePanelOpen = false;

        if (appointmentId == null) {
            updateMessage = "Please select an appointment before submitting the update.";
            updateMessageType = "error";
            updatePanelOpen = true;
        } else {
            try {
                boolean updated = appointmentDB.updateAppointmentForClinic(
                        staffUser.getClinicId(),
                        staffUser.getUserId(),
                        appointmentId,
                        updateAction,
                        remark);

                if (updated) {
                    sendAppointmentNotification(
                            staffUser,
                            appointmentId,
                            updateAction,
                            selectedAppointmentCode,
                            selectedPatientName,
                            selectedServiceName,
                            selectedScheduleLabel,
                            remark);
                    updateMessage = "Appointment updated successfully.";
                    updateMessageType = "success";
                } else {
                    updateMessage = "No appointment was updated. Check that the appointment belongs to your clinic.";
                    updateMessageType = "error";
                    updatePanelOpen = true;
                }
            } catch (Exception ex) {
                updateMessage = "Unable to update the appointment record.";
                updateMessageType = "error";
                updatePanelOpen = true;
                ex.printStackTrace();
            }
        }

        populateAppointmentView(request, staffUser, selectedService, selectedStatus, selectedPatientNameFilter, selectedDate, dateFilter, updateMessage, updateMessageType, updatePanelOpen);
        request.getRequestDispatcher("/staff/appointments.jsp").forward(request, response);
    }

    private void populateAppointmentView(HttpServletRequest request, User staffUser, String selectedService,
            String selectedStatus, String selectedPatientNameFilter, String selectedDate, LocalDate dateFilter, String updateMessage,
            String updateMessageType, boolean updatePanelOpen) {
        List<Appointment> appointments = Collections.emptyList();
        List<String> availableServices = new ArrayList<>();
        String appointmentsError = null;

        try {
            appointments = appointmentDB.findAppointmentsByClinic(staffUser.getClinicId(), selectedService, selectedStatus, selectedPatientNameFilter, dateFilter);
            availableServices = appointmentDB.findServiceNamesByClinic(staffUser.getClinicId());
        } catch (Exception ex) {
            appointmentsError = "Unable to load appointment records from the database.";
            ex.printStackTrace();
        }

        request.setAttribute("appointments", appointments);
        request.setAttribute("availableServices", availableServices);
        request.setAttribute("appointmentsError", appointmentsError);
        request.setAttribute("selectedService", selectedService);
        request.setAttribute("selectedStatus", selectedStatus);
        request.setAttribute("selectedPatientNameFilter", selectedPatientNameFilter);
        request.setAttribute("selectedDate", selectedDate);
        request.setAttribute("assignedClinicId", staffUser.getClinicId());
        request.setAttribute("assignedClinicName", findClinicName(staffUser.getClinicId()));
        request.setAttribute("updateMessage", updateMessage);
        request.setAttribute("updateMessageType", updateMessageType);
        request.setAttribute("updatePanelOpen", updatePanelOpen);
        request.setAttribute("activeStaffPath", "/staff/appointments/manage");
    }

    private void sendAppointmentNotification(User staffUser, Integer appointmentId, String updateAction,
            String appointmentCode, String patientName, String serviceName, String scheduleLabel, String remark) {
        if (staffUser == null || staffUser.getUserId() == null || appointmentId == null) {
            return;
        }

        String normalizedAction = updateAction == null ? "" : updateAction.trim().toUpperCase();
        String notificationType = mapAppointmentNotificationType(normalizedAction);
        String displayCode = appointmentCode == null || appointmentCode.trim().isEmpty() ? "A-" + appointmentId : appointmentCode.trim();
        String patientLabel = patientName == null || patientName.trim().isEmpty() ? "the patient" : patientName.trim();
        String serviceLabel = serviceName == null || serviceName.trim().isEmpty() ? "the selected service" : serviceName.trim();
        String scheduleText = scheduleLabel == null || scheduleLabel.trim().isEmpty() ? "" : " on " + scheduleLabel.trim();

        String title;
        String body;
        switch (notificationType) {
            case "APPOINTMENT_CONFIRMED":
                title = "Appointment confirmed - " + displayCode;
                body = patientLabel + "'s " + serviceLabel + " appointment" + scheduleText + " was confirmed.";
                break;
            case "APPOINTMENT_REJECTED":
                title = "Appointment rejected - " + displayCode;
                body = patientLabel + "'s " + serviceLabel + " appointment" + scheduleText + " was rejected by the clinic.";
                break;
            case "APPOINTMENT_CANCELLED":
                title = "Appointment cancelled - " + displayCode;
                body = patientLabel + "'s " + serviceLabel + " appointment" + scheduleText + " was cancelled by the clinic.";
                break;
            case "NO_SHOW_ALERT":
                title = "No-show alert - " + displayCode;
                body = patientLabel + "'s " + serviceLabel + " appointment" + scheduleText + " was marked as no-show.";
                break;
            case "APPOINTMENT_UPDATED":
                if ("CHECK_IN".equals(normalizedAction)) {
                    title = "Appointment checked in - " + displayCode;
                    body = patientLabel + "'s " + serviceLabel + " appointment" + scheduleText + " was marked as arrived.";
                } else if ("COMPLETE".equals(normalizedAction)) {
                    title = "Appointment completed - " + displayCode;
                    body = patientLabel + "'s " + serviceLabel + " appointment" + scheduleText + " was marked as completed.";
                } else {
                    title = "Appointment updated - " + displayCode;
                    body = patientLabel + "'s " + serviceLabel + " appointment" + scheduleText + " was updated.";
                }
                break;
            default:
                title = "Appointment updated - " + displayCode;
                body = patientLabel + "'s " + serviceLabel + " appointment" + scheduleText + " was updated.";
                break;
        }

        if (remark != null && !remark.trim().isEmpty()) {
            body = body + " Note: " + remark.trim();
        }

        try {
            notificationDB.createNotification(staffUser.getUserId(), notificationType, title, body, appointmentId, null, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String mapAppointmentNotificationType(String updateAction) {
        switch (updateAction) {
            case "APPROVE":
                return "APPOINTMENT_CONFIRMED";
            case "REJECT_BY_CLINIC":
                return "APPOINTMENT_REJECTED";
            case "CHECK_IN":
                return "APPOINTMENT_UPDATED";
            case "COMPLETE":
                return "APPOINTMENT_UPDATED";
            case "NO_SHOW":
                return "NO_SHOW_ALERT";
            case "CANCEL_BY_CLINIC":
                return "APPOINTMENT_CANCELLED";
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

    private StaffDashboardStats buildDashboardStats(Integer clinicId) {
        try {
            return appointmentDB.findDashboardStatsByClinic(clinicId);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new StaffDashboardStats();
        }
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

    private Integer parseAppointmentId(String value) {
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

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(value.trim(), DISPLAY_DATE_FORMAT);
            } catch (DateTimeParseException secondEx) {
                return null;
            }
        }
    }
}
