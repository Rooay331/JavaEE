package ict.servlet;

import ict.bean.ClinicService;
import ict.bean.ClinicServiceStatus;
import ict.bean.Appointment;
import ict.bean.AppointmentBookingSlot;
import ict.bean.QueueTicket;
import ict.bean.User;
import ict.db.AppointmentDB;
import ict.db.ClinicServiceDB;
import ict.db.NotificationDB;
import ict.db.PatientDB;
import ict.db.PolicyDB;
import ict.db.ServiceDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/patient/appointments/book"})
public class PatientAppointmentController extends HttpServlet {

    private static final int DATE_WINDOW_DAYS = 14;
    private static final int RECENT_APPOINTMENT_LIMIT = 6;

    private PatientDB patientDB;
    private AppointmentDB appointmentDB;
    private NotificationDB notificationDB;
    private PolicyDB policyDB;
    private ClinicServiceDB clinicServiceDB;
    private ServiceDB serviceDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        patientDB = new PatientDB(dbUrl, dbUser, dbPassword);
        appointmentDB = new AppointmentDB(dbUrl, dbUser, dbPassword);
        notificationDB = new NotificationDB(dbUrl, dbUser, dbPassword);
        policyDB = new PolicyDB(dbUrl, dbUser, dbPassword);
        clinicServiceDB = new ClinicServiceDB(dbUrl, dbUser, dbPassword);
        serviceDB = new ServiceDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User patientUser = getLoggedInPatientUser(request, response);
        if (patientUser == null) {
            return;
        }

        String selectedDateText = request.getParameter("date");
        String selectedClinicText = request.getParameter("clinicId");
        String selectedServiceText = request.getParameter("serviceId");
        String selectedStartTimeText = request.getParameter("slotStartTime");
        String selectedEndTimeText = request.getParameter("slotEndTime");
        String bookingIdText = request.getParameter("bookingId");
        Integer selectedClinicId = parseInteger(selectedClinicText);
        Integer selectedServiceId = parseInteger(selectedServiceText);
        LocalTime selectedStartTime = parseTime(selectedStartTimeText);
        LocalTime selectedEndTime = parseTime(selectedEndTimeText);
        Integer bookingId = parseInteger(bookingIdText);

        renderBookingPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
            selectedStartTime, selectedEndTime, bookingId, null, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User patientUser = getLoggedInPatientUser(request, response);
        if (patientUser == null) {
            return;
        }

        String selectedDateText = request.getParameter("date");
        String selectedClinicText = request.getParameter("clinicId");
        String selectedServiceText = request.getParameter("serviceId");
        String selectedStartTimeText = request.getParameter("slotStartTime");
        String selectedEndTimeText = request.getParameter("slotEndTime");
        Integer selectedClinicId = parseInteger(selectedClinicText);
        Integer selectedServiceId = parseInteger(selectedServiceText);
        LocalDate selectedDate = parseDate(selectedDateText);
        LocalTime selectedStartTime = parseTime(selectedStartTimeText);
        LocalTime selectedEndTime = parseTime(selectedEndTimeText);

        if (selectedDate == null || selectedClinicId == null || selectedServiceId == null || selectedStartTime == null || selectedEndTime == null) {
            renderBookingPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                selectedStartTime, selectedEndTime, null, null, null, "Please choose an available appointment time before booking.");
            return;
        }

        try {
            AppointmentBookingSlot selectedSlot = appointmentDB.findBookingSlot(selectedDate, selectedClinicId, selectedServiceId, selectedStartTime, selectedEndTime);
            if (selectedSlot == null) {
            renderBookingPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                selectedStartTime, selectedEndTime, null, null, null, "The selected appointment time could not be found.");
                return;
            }

            if (selectedDate != null && selectedSlot.getSlotDate() != null && !selectedSlot.getSlotDate().equals(selectedDate)) {
            renderBookingPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                selectedStartTime, selectedEndTime, null, null, null, "The selected appointment time does not match the chosen date.");
            return;
            }

            if (selectedClinicId != null && selectedSlot.getClinicId() != null && !selectedClinicId.equals(selectedSlot.getClinicId())) {
            renderBookingPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                selectedStartTime, selectedEndTime, null, null, null, "The selected appointment time does not match the chosen clinic.");
            return;
            }

            if (selectedServiceId != null && selectedSlot.getServiceId() != null && !selectedServiceId.equals(selectedSlot.getServiceId())) {
            renderBookingPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                selectedStartTime, selectedEndTime, null, null, null, "The selected appointment time does not match the chosen service.");
                return;
            }

            if (!selectedSlot.isAvailable()) {
            renderBookingPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                selectedStartTime, selectedEndTime, null, null, null, "This appointment time is no longer available.");
                return;
            }

            if (appointmentDB.hasPatientAppointmentConflict(patientUser.getUserId(), selectedSlot.getSlotDate(), selectedSlot.getStartTime(), selectedSlot.getEndTime())) {
            renderBookingPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                selectedStartTime, selectedEndTime, null, null, null, "You already have another appointment that overlaps with this time.");
                return;
            }

            int maxActiveAppointments = policyDB.findIntPolicy("MAX_ACTIVE_APPOINTMENTS", 3);
            int activeAppointmentCount = appointmentDB.countActiveAppointments(patientUser.getUserId());
            if (activeAppointmentCount >= maxActiveAppointments) {
            renderBookingPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                selectedStartTime, selectedEndTime, null, null, null, "You already have the maximum number of active appointments allowed by policy.");
                return;
            }

            String appointmentStatus = selectedSlot.isRequiresApproval() ? "PENDING" : "CONFIRMED";
            String note = selectedSlot.isRequiresApproval()
                    ? "Awaiting clinic approval"
                    : "Booked online through the patient portal";

            Integer appointmentId = appointmentDB.createPatientAppointment(
                    patientUser.getUserId(),
                    selectedSlot.getClinicId(),
                    selectedSlot.getServiceId(),
                    selectedSlot.getSlotDate(),
                    selectedSlot.getStartTime(),
                    selectedSlot.getEndTime(),
                    appointmentStatus,
                    note);

            if (appointmentId == null) {
                renderBookingPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                        selectedStartTime, selectedEndTime, null, null, null, "Unable to create the appointment record. Please try again.");
                return;
            }

            sendBookingNotification(patientUser, selectedSlot, appointmentId, appointmentStatus);
            response.sendRedirect(request.getContextPath() + "/patient/appointments/book?date=" + selectedSlot.getSlotDate()
                    + "&clinicId=" + selectedSlot.getClinicId()
                    + "&serviceId=" + selectedSlot.getServiceId()
                    + "&slotStartTime=" + selectedSlot.getStartTime()
                    + "&slotEndTime=" + selectedSlot.getEndTime()
                    + "&bookingId=" + appointmentId);
        } catch (ServletException ex) {
            throw ex;
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message != null && message.toLowerCase().contains("duplicate")) {
                message = "You already have an appointment for this appointment time.";
            } else {
                message = "Unable to create the appointment record. Please try again.";
            }
            renderBookingPage(request, response, patientUser, selectedDateText, selectedClinicId, selectedServiceId,
                    selectedStartTime, selectedEndTime, null, null, null, message);
        }
    }

    private void renderBookingPage(HttpServletRequest request, HttpServletResponse response, User patientUser,
            String selectedDateText, Integer selectedClinicId, Integer selectedServiceId, LocalTime selectedStartTime,
            LocalTime selectedEndTime, Integer bookingId, String bookingMessage, String bookingMessageType, String bookingError) throws ServletException, IOException {
        User patientProfile = patientUser;
        Appointment bookingConfirmation = null;
        List<Appointment> recentAppointments = Collections.emptyList();
        Appointment activeAppointment = null;
        QueueTicket activeQueueTicket = null;
        Map<LocalDate, Integer> bookingDateAvailability = Collections.emptyMap();
        List<LocalDate> bookingDateOptions = new ArrayList<>();
        List<AppointmentBookingSlot> availableBookingSlots = Collections.emptyList();
        List<ClinicService> availableClinics = Collections.emptyList();
        List<ClinicServiceStatus> availableServices = Collections.emptyList();
        String selectedServiceHoursLabel = null;
        String selectionInstruction = null;
        String pageError = bookingError;
        String pageMessage = bookingMessage;
        String pageMessageType = bookingMessageType;
        LocalDate selectedDate = parseDate(selectedDateText);
        AppointmentBookingSlot selectedSlot = null;

        try {
            User persistedProfile = patientDB.findPatientById(patientUser.getUserId());
            if (persistedProfile != null) {
                patientProfile = persistedProfile;
            }

            availableClinics = clinicServiceDB.findActiveClinics();
            List<ClinicServiceStatus> clinicServiceStatuses = selectedClinicId == null
                    ? serviceDB.findActiveServices()
                    : serviceDB.findClinicServiceStatuses(selectedClinicId);
            availableServices = new ArrayList<>();
            for (ClinicServiceStatus serviceStatus : clinicServiceStatuses) {
                if (serviceStatus != null && serviceStatus.isActive()) {
                    availableServices.add(serviceStatus);
                }
            }
            recentAppointments = appointmentDB.findPatientAppointments(patientUser.getUserId(), RECENT_APPOINTMENT_LIMIT);
            for (Appointment appointment : recentAppointments) {
                if (appointment != null && isActiveAppointmentStatus(appointment.getStatus())) {
                    activeAppointment = appointment;
                    break;
                }
            }

            activeQueueTicket = patientDB.findAnyActiveQueueTicketByPatient(patientUser.getUserId());
            bookingDateAvailability = appointmentDB.findAvailableBookingDateCounts(
                    LocalDate.now(),
                    LocalDate.now().plusDays(DATE_WINDOW_DAYS - 1),
                    selectedClinicId,
                    selectedServiceId);
            bookingDateOptions = buildDateOptions(LocalDate.now(), DATE_WINDOW_DAYS, selectedDate);

            if (selectedDate == null) {
                selectedDate = chooseDefaultDate(bookingDateAvailability, bookingDateOptions);
            }

            boolean waitingForClinicOrService = selectedDate != null && (selectedClinicId == null || selectedServiceId == null);
            if (waitingForClinicOrService) {
                selectionInstruction = "Select a clinic and service first, then choose a time and availability.";
            }

            if (bookingId != null) {
                bookingConfirmation = appointmentDB.findPatientAppointmentById(patientUser.getUserId(), bookingId);
                if (bookingConfirmation != null) {
                    pageMessage = pageMessage == null ? "Appointment booked successfully." : pageMessage;
                    pageMessageType = pageMessageType == null ? "success" : pageMessageType;
                    if (selectedDate == null) {
                        selectedDate = bookingConfirmation.getSlotDate();
                    }
                } else if (pageError == null) {
                    pageError = "The booked appointment could not be loaded.";
                }
            }

            if (selectedDate == null) {
                selectedDate = chooseDefaultDate(bookingDateAvailability, bookingDateOptions);
            }

            boolean canLoadBookingSlots = selectedDate != null && selectedClinicId != null && selectedServiceId != null;
            if (canLoadBookingSlots) {
                availableBookingSlots = appointmentDB.findBookingSlots(selectedDate, selectedClinicId, selectedServiceId);
                selectedSlot = resolveSelectedSlot(availableBookingSlots, selectedDate, selectedClinicId, selectedServiceId, selectedStartTime, selectedEndTime);
                selectedServiceHoursLabel = clinicServiceDB.findServiceOpeningHoursLabel(selectedServiceId, selectedDate);
            } else {
                availableBookingSlots = Collections.emptyList();
                selectedSlot = null;
            }
        } catch (Exception ex) {
            pageError = "Unable to load appointment booking data from the database.";
            ex.printStackTrace();
        }

        int activeAppointmentCount = 0;
        try {
            activeAppointmentCount = appointmentDB.countActiveAppointments(patientUser.getUserId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        int maxActiveAppointments = 3;
        try {
            maxActiveAppointments = policyDB.findIntPolicy("MAX_ACTIVE_APPOINTMENTS", 3);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        int availableSlotCount = 0;
        for (AppointmentBookingSlot slot : availableBookingSlots) {
            if (slot != null && slot.isAvailable()) {
                availableSlotCount++;
            }
        }

        request.setAttribute("patientUser", patientProfile);
        request.setAttribute("recentAppointments", recentAppointments);
        request.setAttribute("activeAppointment", activeAppointment);
        request.setAttribute("activeQueueTicket", activeQueueTicket);
        request.setAttribute("activeAppointmentCount", activeAppointmentCount);
        request.setAttribute("maxActiveAppointments", maxActiveAppointments);
        request.setAttribute("availableClinics", availableClinics);
        request.setAttribute("availableServices", availableServices);
        request.setAttribute("selectedClinicId", selectedClinicId);
        request.setAttribute("selectedServiceId", selectedServiceId);
        request.setAttribute("selectedServiceHoursLabel", selectedServiceHoursLabel);
        request.setAttribute("selectionInstruction", selectionInstruction);
        request.setAttribute("selectedDate", selectedDate);
        request.setAttribute("selectedStartTime", selectedStartTime);
        request.setAttribute("selectedEndTime", selectedEndTime);
        request.setAttribute("selectedSlot", selectedSlot);
        request.setAttribute("bookingDateAvailability", bookingDateAvailability);
        request.setAttribute("bookingDateOptions", bookingDateOptions);
        request.setAttribute("availableBookingSlots", availableBookingSlots);
        request.setAttribute("availableSlotCount", availableSlotCount);
        request.setAttribute("bookingMessage", pageMessage);
        request.setAttribute("bookingMessageType", pageMessageType);
        request.setAttribute("bookingError", pageError);
        request.setAttribute("bookingConfirmation", bookingConfirmation);
        request.setAttribute("activePatientPath", "/patient/appointments/book");
        request.getRequestDispatcher("/patient/appointments/book.jsp").forward(request, response);
    }

    private void sendBookingNotification(User patientUser, AppointmentBookingSlot slot, Integer appointmentId, String appointmentStatus) {
        if (patientUser == null || slot == null || appointmentId == null) {
            return;
        }

        String type = "PENDING".equalsIgnoreCase(appointmentStatus) ? "PENDING_APPROVAL" : "APPOINTMENT_CONFIRMED";
        String title = "PENDING".equalsIgnoreCase(appointmentStatus)
                ? "Appointment pending approval - A-" + appointmentId
                : "Appointment confirmed - A-" + appointmentId;
        String body = "Your booking for " + slot.getClinicServiceLabel() + " on " + slot.getScheduleLabel()
                + ("PENDING".equalsIgnoreCase(appointmentStatus) ? " is waiting for staff approval." : " is confirmed.");

        try {
            notificationDB.createNotification(patientUser.getUserId(), type, title, body, appointmentId, null, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private List<LocalDate> buildDateOptions(LocalDate startDate, int dayCount, LocalDate selectedDate) {
        List<LocalDate> dates = new ArrayList<>();
        if (startDate == null || dayCount <= 0) {
            return dates;
        }

        for (int index = 0; index < dayCount; index++) {
            dates.add(startDate.plusDays(index));
        }

        if (selectedDate != null && !dates.contains(selectedDate)) {
            dates.add(selectedDate);
        }

        return dates;
    }

    private LocalDate chooseDefaultDate(Map<LocalDate, Integer> bookingDateAvailability, List<LocalDate> bookingDateOptions) {
        if (bookingDateAvailability != null) {
            for (Map.Entry<LocalDate, Integer> entry : bookingDateAvailability.entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    return entry.getKey();
                }
            }
        }

        if (bookingDateOptions != null && !bookingDateOptions.isEmpty()) {
            return bookingDateOptions.get(0);
        }

        return LocalDate.now();
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

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            LocalDate parsedDate = LocalDate.parse(value.trim());
            if (parsedDate.isBefore(LocalDate.now())) {
                return null;
            }
            return parsedDate;
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private AppointmentBookingSlot resolveSelectedSlot(List<AppointmentBookingSlot> availableBookingSlots, LocalDate selectedDate,
            Integer selectedClinicId, Integer selectedServiceId, LocalTime selectedStartTime, LocalTime selectedEndTime) {
        if (availableBookingSlots == null || selectedDate == null || selectedStartTime == null || selectedClinicId == null || selectedServiceId == null) {
            return null;
        }

        for (AppointmentBookingSlot slot : availableBookingSlots) {
            if (slot == null) {
                continue;
            }

            if (slot.getSlotDate() == null || !selectedDate.equals(slot.getSlotDate())) {
                continue;
            }
            if (slot.getClinicId() == null || !selectedClinicId.equals(slot.getClinicId())) {
                continue;
            }
            if (slot.getServiceId() == null || !selectedServiceId.equals(slot.getServiceId())) {
                continue;
            }
            if (slot.getStartTime() == null || !selectedStartTime.equals(slot.getStartTime())) {
                continue;
            }
            if (selectedEndTime != null && slot.getEndTime() != null && !selectedEndTime.equals(slot.getEndTime())) {
                continue;
            }

            return slot;
        }

        return null;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isActiveAppointmentStatus(String status) {
        if (status == null) {
            return false;
        }

        switch (status.toUpperCase()) {
            case "PENDING":
            case "CONFIRMED":
            case "ARRIVED":
                return true;
            default:
                return false;
        }
    }
}