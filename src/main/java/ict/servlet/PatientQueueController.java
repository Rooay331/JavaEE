package ict.servlet;

import ict.bean.ClinicService;
import ict.bean.ClinicServiceStatus;
import ict.bean.QueueTicket;
import ict.bean.User;
import ict.db.ClinicServiceDB;
import ict.db.PatientDB;
import ict.db.QueueDB;
import ict.db.ServiceDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebServlet(urlPatterns = {"/patient/queue/join"})
public class PatientQueueController extends HttpServlet {

    private static final int QUEUE_TABLE_LIMIT = 30;

    private PatientDB patientDB;
    private QueueDB queueDB;
    private ClinicServiceDB clinicServiceDB;
    private ServiceDB serviceDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        patientDB = new PatientDB(dbUrl, dbUser, dbPassword);
        queueDB = new QueueDB(dbUrl, dbUser, dbPassword);
        clinicServiceDB = new ClinicServiceDB(dbUrl, dbUser, dbPassword);
        serviceDB = new ServiceDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User patientUser = getLoggedInPatientUser(request, response);
        if (patientUser == null) {
            return;
        }

        String selectedDateText = normalize(request.getParameter("date"));
        LocalDate selectedDate = resolveRequestedQueueDate(selectedDateText);
        if (selectedDate == null || selectedDate.isBefore(LocalDate.now())) {
            selectedDate = LocalDate.now();
        }
        selectedDateText = selectedDate.toString();

        Integer selectedClinicId = parseInteger(request.getParameter("clinicId"));
        Integer selectedServiceId = parseInteger(request.getParameter("serviceId"));

        String queueMessage = null;
        String queueMessageType = null;
        String messageCode = normalize(request.getParameter("message"));
        if ("joined".equalsIgnoreCase(messageCode)) {
            queueMessage = "Queue ticket created successfully. Staff can now manage it from the queue board.";
            queueMessageType = "success";
        }

        renderPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                null, queueMessage, queueMessageType, null, parseInteger(request.getParameter("ticketId")));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User patientUser = getLoggedInPatientUser(request, response);
        if (patientUser == null) {
            return;
        }

        String selectedDateText = normalize(request.getParameter("date"));
        Integer selectedClinicId = parseInteger(request.getParameter("clinicId"));
        Integer selectedServiceId = parseInteger(request.getParameter("serviceId"));
        String contactPhone = normalize(request.getParameter("contactPhone"));

        LocalDate selectedDate = resolveRequestedQueueDate(selectedDateText);
        LocalDate today = LocalDate.now();
        if (selectedDate == null) {
            renderPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                contactPhone, null, null, "Queue date format is invalid. Please refresh and try again.", null);
            return;
        }

        if (selectedDate.isBefore(today)) {
            selectedDateText = selectedDate.toString();
            renderPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                contactPhone, null, null, "Queue date must be today or later.", null);
            return;
        }

        selectedDateText = selectedDate.toString();

        if (selectedClinicId == null) {
            renderPage(request, response, patientUser, selectedDateText, null, selectedServiceId,
                    contactPhone, null, null, "Please choose a clinic first.", null);
            return;
        }

        if (selectedServiceId == null) {
            renderPage(request, response, patientUser, selectedDateText, selectedClinicId, null,
                    contactPhone, null, null, "Please choose a service for the selected clinic.", null);
            return;
        }

        if (contactPhone != null && !isValidPhone(contactPhone)) {
            renderPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                    contactPhone, null, null, "Phone format is invalid. Use 6-32 characters with digits, spaces, +, -, or parentheses.", null);
            return;
        }

        try {
            ClinicServiceStatus selectedService = findWalkInService(selectedClinicId, selectedServiceId);
            if (selectedService == null || selectedService.getClinicServiceId() == null) {
                renderPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                        contactPhone, null, null, "The selected service is not available for walk-in queue at this clinic.", null);
                return;
            }

            if (queueDB.hasActiveQueueTicket(patientUser.getUserId(), selectedClinicId, selectedDate)) {
                renderPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                        contactPhone, null, null, "You already have an active queue ticket for this clinic on " + selectedDate + ".", null);
                return;
            }

            User patientProfile = patientDB.findPatientById(patientUser.getUserId());
            String existingPhone = patientProfile == null ? null : normalize(patientProfile.getPhone());
            if (contactPhone != null && !contactPhone.equals(existingPhone)) {
                patientDB.updatePatientPhone(patientUser.getUserId(), contactPhone);
            }

            Integer ticketId = queueDB.createPatientWalkInQueueTicket(
                    patientUser.getUserId(),
                    selectedClinicId,
                    selectedService.getClinicServiceId(),
                    selectedDate);

            if (ticketId == null) {
                renderPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                        contactPhone, null, null, "Unable to create queue ticket. Please check your selection and try again.", null);
                return;
            }

            response.sendRedirect(request.getContextPath() + "/patient/queue/join?message=joined"
                    + "&ticketId=" + ticketId
                    + "&date=" + selectedDate
                    + "&clinicId=" + selectedClinicId
                    + "&serviceId=" + selectedServiceId);
        } catch (Exception ex) {
            getServletContext().log("Unable to save queue data from the database.", ex);
            renderPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                    contactPhone, null, null, "Unable to save queue data from the database.", null);
        }
    }

    private void renderPage(HttpServletRequest request, HttpServletResponse response, User patientUser,
            String selectedDateText, Integer selectedClinicId, Integer selectedServiceId, String contactPhone,
            String queueMessage, String queueMessageType, String queueError, Integer highlightedTicketId)
            throws ServletException, IOException {
        User patientProfile = patientUser;
        LocalDate selectedDate = parseDate(selectedDateText);
        if (selectedDate == null || selectedDate.isBefore(LocalDate.now())) {
            selectedDate = LocalDate.now();
        }

        List<ClinicService> availableClinics = Collections.emptyList();
        List<ClinicServiceStatus> availableServices = Collections.emptyList();
        List<ClinicServiceStatus> allWalkInServices = Collections.emptyList();
        List<QueueTicket> patientQueueTickets = Collections.emptyList();
        QueueTicket activeQueueTicket = null;

        try {
            User persistedProfile = patientDB.findPatientById(patientUser.getUserId());
            if (persistedProfile != null) {
                patientProfile = persistedProfile;
            }

            availableClinics = clinicServiceDB.findActiveClinics();
            allWalkInServices = new ArrayList<>();
            for (ClinicService clinic : availableClinics) {
                if (clinic == null || clinic.getClinicId() == null) {
                    continue;
                }
                allWalkInServices.addAll(findWalkInServices(clinic.getClinicId()));
            }

            availableServices = filterServicesByClinic(allWalkInServices, selectedClinicId);

            boolean selectedServiceVisible = false;
            for (ClinicServiceStatus serviceStatus : availableServices) {
                if (serviceStatus != null
                        && serviceStatus.getServiceId() != null
                        && serviceStatus.getServiceId().equals(selectedServiceId)) {
                    selectedServiceVisible = true;
                    break;
                }
            }
            if (!selectedServiceVisible) {
                selectedServiceId = null;
            }

            patientQueueTickets = queueDB.findQueueTicketsByPatient(patientUser.getUserId(), null, null, QUEUE_TABLE_LIMIT);
            activeQueueTicket = patientDB.findAnyActiveQueueTicketByPatient(patientUser.getUserId());
        } catch (Exception ex) {
            getServletContext().log("Unable to load queue page data from the database.", ex);
            if (queueError == null) {
                queueError = "Unable to load queue page data from the database.";
            }
        }

        String phoneDraft = contactPhone;
        if (phoneDraft == null) {
            phoneDraft = normalize(patientProfile.getPhone());
        }

        request.setAttribute("patientUser", patientProfile);
        request.setAttribute("selectedDate", selectedDate);
        request.setAttribute("selectedClinicId", selectedClinicId);
        request.setAttribute("selectedServiceId", selectedServiceId);
        request.setAttribute("contactPhone", phoneDraft == null ? "" : phoneDraft);
        request.setAttribute("availableClinics", availableClinics == null ? Collections.emptyList() : availableClinics);
        request.setAttribute("availableServices", availableServices == null ? Collections.emptyList() : availableServices);
        request.setAttribute("allWalkInServices", allWalkInServices == null ? Collections.emptyList() : allWalkInServices);
        request.setAttribute("patientQueueTickets", patientQueueTickets == null ? Collections.emptyList() : patientQueueTickets);
        request.setAttribute("activeQueueTicket", activeQueueTicket);
        request.setAttribute("queueMessage", queueMessage);
        request.setAttribute("queueMessageType", queueMessageType);
        request.setAttribute("queueError", queueError);
        request.setAttribute("highlightedTicketId", highlightedTicketId);
        request.setAttribute("activePatientPath", "/patient/queue/join");
        request.getRequestDispatcher("/patient/queue/joinQueue.jsp").forward(request, response);
    }

    private List<ClinicServiceStatus> findWalkInServices(Integer clinicId) throws Exception {
        if (clinicId == null) {
            return Collections.emptyList();
        }

        List<ClinicServiceStatus> allServices = serviceDB.findClinicServiceStatuses(clinicId);
        List<ClinicServiceStatus> walkInServices = new ArrayList<>();
        for (ClinicServiceStatus serviceStatus : allServices) {
            if (serviceStatus != null && serviceStatus.isActive() && serviceStatus.isWalkInEnabled()) {
                walkInServices.add(serviceStatus);
            }
        }
        return walkInServices;
    }

    private ClinicServiceStatus findWalkInService(Integer clinicId, Integer serviceId) throws Exception {
        if (clinicId == null || serviceId == null) {
            return null;
        }

        List<ClinicServiceStatus> services = findWalkInServices(clinicId);
        for (ClinicServiceStatus serviceStatus : services) {
            if (serviceStatus != null
                    && serviceStatus.getServiceId() != null
                    && serviceStatus.getServiceId().equals(serviceId)) {
                return serviceStatus;
            }
        }

        return null;
    }

    private List<ClinicServiceStatus> filterServicesByClinic(List<ClinicServiceStatus> services, Integer clinicId) {
        if (services == null || services.isEmpty() || clinicId == null) {
            return Collections.emptyList();
        }

        List<ClinicServiceStatus> filtered = new ArrayList<>();
        for (ClinicServiceStatus serviceStatus : services) {
            if (serviceStatus == null || serviceStatus.getClinicId() == null) {
                continue;
            }
            if (clinicId.equals(serviceStatus.getClinicId())) {
                filtered.add(serviceStatus);
            }
        }
        return filtered;
    }

    private User getLoggedInPatientUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

        User patientUser = (User) sessionUser;
        if (!"PATIENT".equalsIgnoreCase(patientUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        return patientUser;
    }

    private Integer parseInteger(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            return Integer.valueOf(trimmed);
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
            return null;
        }
    }

    private LocalDate resolveRequestedQueueDate(String selectedDateText) {
        if (selectedDateText == null) {
            return LocalDate.now();
        }

        return parseDate(selectedDateText);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isValidPhone(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        return value.trim().matches("^[0-9+()\\-\\s]{6,32}$");
    }
}
