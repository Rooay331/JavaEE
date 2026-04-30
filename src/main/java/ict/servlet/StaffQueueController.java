package ict.servlet;

import ict.bean.QueueTicket;
import ict.bean.User;
import ict.db.AppointmentDB;
import ict.db.ClinicDB;
import ict.db.QueueDB;
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
import java.util.Collections;
import java.util.List;

@WebServlet(urlPatterns = {"/staff/queue/manage"})
public class StaffQueueController extends HttpServlet {

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("d-M-yyyy");

    private QueueDB queueDB;
    private ClinicDB clinicDB;
    private AppointmentDB appointmentDB;
    private NotificationDB notificationDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        queueDB = new QueueDB(dbUrl, dbUser, dbPassword);
        clinicDB = new ClinicDB(dbUrl, dbUser, dbPassword);
        appointmentDB = new AppointmentDB(dbUrl, dbUser, dbPassword);
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

        String selectedService = normalize(request.getParameter("service"));
        String selectedDate = normalize(request.getParameter("date"));
        LocalDate queueDate = parseDate(selectedDate);
        if (queueDate == null) {
            queueDate = LocalDate.now();
        }

        String action = normalize(request.getParameter("queueAction"));
        String flashMessage;
        String flashType;

        try {
            if ("CALL_NEXT".equalsIgnoreCase(action)) {
                Integer ticketId = queueDB.callNextWaitingTicket(staffUser.getClinicId(), selectedService, queueDate);
                if (ticketId == null) {
                    flashMessage = "No waiting ticket was available to call.";
                    flashType = "error";
                } else {
                    createQueueNotification(staffUser, "QUEUE_CALLED", ticketId, selectedService);
                    flashMessage = "Next queue ticket was called.";
                    flashType = "success";
                }
            } else if ("SKIP_CURRENT".equalsIgnoreCase(action)) {
                Integer skippedTicketId = queueDB.skipCurrentCalledTicket(staffUser.getClinicId(), selectedService, queueDate);
                if (skippedTicketId == null) {
                    flashMessage = "No called ticket was available to skip.";
                    flashType = "error";
                } else {
                    createQueueNotification(staffUser, "QUEUE_SKIPPED", skippedTicketId, selectedService);
                    flashMessage = "Current called ticket was skipped.";
                    flashType = "success";
                }
            } else if ("EXPIRE_CURRENT".equalsIgnoreCase(action)) {
                Integer expiredTicketId = queueDB.expireCurrentCalledTicket(staffUser.getClinicId(), selectedService, queueDate);
                if (expiredTicketId == null) {
                    flashMessage = "No called ticket was available to expire.";
                    flashType = "error";
                } else {
                    createQueueNotification(staffUser, "QUEUE_EXPIRED", expiredTicketId, selectedService);
                    flashMessage = "Current called ticket was marked as expired.";
                    flashType = "success";
                }
            } else if ("MARK_SERVED".equalsIgnoreCase(action)) {
                boolean updated = queueDB.markCurrentCalledTicketServed(staffUser.getClinicId(), selectedService, queueDate);
                flashMessage = updated ? "Current called ticket was marked as served." : "No called ticket was available to mark as served.";
                flashType = updated ? "success" : "error";
            } else {
                flashMessage = "Unsupported queue action.";
                flashType = "error";
            }
        } catch (Exception ex) {
            flashMessage = "Unable to update queue records from the database.";
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

        String selectedService = normalize(request.getParameter("service"));
        String selectedStatus = normalize(request.getParameter("status"));
        String selectedPatientNameFilter = normalize(request.getParameter("patientName"));
        String selectedDate = normalize(request.getParameter("date"));
        LocalDate queueDate = parseDate(selectedDate);
        if (queueDate == null) {
            queueDate = LocalDate.now();
            selectedDate = queueDate.toString();
        }

        List<QueueTicket> queueTickets = Collections.emptyList();
        List<QueueTicket> queueOverviewTickets = Collections.emptyList();
        List<String> serviceOptions = Collections.emptyList();
        String queueError = null;

        try {
            queueTickets = queueDB.findQueueTicketsByClinic(staffUser.getClinicId(), selectedService, selectedStatus, selectedPatientNameFilter, queueDate);
            queueOverviewTickets = queueDB.findQueueTicketsByClinic(staffUser.getClinicId(), selectedService, null, selectedPatientNameFilter, queueDate);
            serviceOptions = appointmentDB.findServiceNamesByClinic(staffUser.getClinicId());
        } catch (Exception ex) {
            queueError = "Unable to load queue records from the database.";
            ex.printStackTrace();
        }

        int waitingCount = 0;
        int calledCount = 0;
        int servedCount = 0;
        int estimatedWaitTotal = 0;
        int estimatedWaitCount = 0;
        String currentServingLabel = "None";
        String nextWaitingLabel = "None";
        for (QueueTicket ticket : queueOverviewTickets) {
            if (ticket == null) {
                continue;
            }
            if ("WAITING".equalsIgnoreCase(ticket.getStatus())) {
                waitingCount++;
                if ("None".equals(nextWaitingLabel)) {
                    nextWaitingLabel = ticket.getDisplayCode();
                }
            } else if ("CALLED".equalsIgnoreCase(ticket.getStatus())) {
                calledCount++;
                if ("None".equals(currentServingLabel)) {
                    currentServingLabel = ticket.getDisplayCode();
                }
            } else if ("SERVED".equalsIgnoreCase(ticket.getStatus())) {
                servedCount++;
            }

            if (ticket.getEstimatedWaitMinutes() != null) {
                estimatedWaitTotal += ticket.getEstimatedWaitMinutes();
                estimatedWaitCount++;
            }
        }

        int averageWait = estimatedWaitCount == 0 ? 0 : Math.round((float) estimatedWaitTotal / estimatedWaitCount);

        request.setAttribute("queueTickets", queueTickets);
        request.setAttribute("serviceOptions", serviceOptions);
        request.setAttribute("queueError", queueError);
        request.setAttribute("selectedService", selectedService);
        request.setAttribute("selectedStatus", selectedStatus);
        request.setAttribute("selectedPatientNameFilter", selectedPatientNameFilter);
        request.setAttribute("selectedDate", selectedDate);
        request.setAttribute("waitingQueueCount", waitingCount);
        request.setAttribute("calledQueueCount", calledCount);
        request.setAttribute("servedQueueCount", servedCount);
        request.setAttribute("totalQueueCount", queueOverviewTickets.size());
        request.setAttribute("averageWaitMinutes", averageWait);
        request.setAttribute("currentServingLabel", currentServingLabel);
        request.setAttribute("nextWaitingLabel", nextWaitingLabel);
        request.setAttribute("assignedClinicId", staffUser.getClinicId());
        request.setAttribute("assignedClinicName", findClinicName(staffUser.getClinicId()));
        request.setAttribute("activeStaffPath", "/staff/queue/manage");
        if (flashMessage != null) {
            request.setAttribute("flashMessage", flashMessage);
            request.setAttribute("flashType", flashType);
        }
        request.getRequestDispatcher("/staff/queue/manage.jsp").forward(request, response);
    }

    private void createQueueNotification(User staffUser, String notificationType, Integer ticketId, String serviceName) {
        if (staffUser == null || staffUser.getUserId() == null || ticketId == null) {
            return;
        }

        String serviceContext = serviceName == null || serviceName.trim().isEmpty() ? "the clinic queue" : serviceName.trim();
        String title;
        String body;

        switch (notificationType) {
            case "QUEUE_CALLED":
                title = "Queue ticket called - " + serviceContext;
                body = "Queue ticket #" + ticketId + " was called for " + serviceContext + ".";
                break;
            case "QUEUE_SKIPPED":
                title = "Queue ticket skipped - " + serviceContext;
                body = "Queue ticket #" + ticketId + " was skipped after no response.";
                break;
            case "QUEUE_EXPIRED":
                title = "Queue ticket expired - " + serviceContext;
                body = "Queue ticket #" + ticketId + " expired while waiting.";
                break;
            default:
                return;
        }

        try {
            notificationDB.createNotification(staffUser.getUserId(), notificationType, title, body, null, ticketId, null);
        } catch (Exception ex) {
            ex.printStackTrace();
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

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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