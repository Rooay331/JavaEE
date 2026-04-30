<%@page import="ict.bean.Appointment"%>
<%@page import="ict.bean.ClinicService" %>
<%@page import="ict.bean.ClinicServiceStatus" %>
<%@page import="ict.bean.AppointmentBookingSlot"%>
<%@page import="ict.bean.QueueTicket"%>
<%@page import="ict.bean.User"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.time.LocalTime"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.time.format.TextStyle"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Locale"%>
<%@page import="java.util.Map"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d-M-yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "-" : escapeHtml(value);
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : DATE_FORMATTER.format(value);
    }

    private String patientCode(Integer userId) {
        return userId == null ? "Patient" : "P-" + userId;
    }

    private String selectedDateClass(LocalDate selectedDate, LocalDate candidate) {
        return selectedDate != null && selectedDate.equals(candidate) ? "active" : "";
    }

    private void appendQueryParam(StringBuilder query, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        if (query.length() > 0) {
            query.append('&');
        }
        query.append(key).append('=').append(value.trim());
    }

    private String buildBookingQuery(LocalDate date, Integer clinicId, Integer serviceId, LocalTime slotStartTime, LocalTime slotEndTime, Integer bookingId) {
        StringBuilder query = new StringBuilder();
        appendQueryParam(query, "date", date == null ? null : ISO_DATE_FORMATTER.format(date));
        appendQueryParam(query, "clinicId", clinicId == null ? null : clinicId.toString());
        appendQueryParam(query, "serviceId", serviceId == null ? null : serviceId.toString());
        appendQueryParam(query, "slotStartTime", slotStartTime == null ? null : slotStartTime.toString());
        appendQueryParam(query, "slotEndTime", slotEndTime == null ? null : slotEndTime.toString());
        appendQueryParam(query, "bookingId", bookingId == null ? null : bookingId.toString());
        return query.length() == 0 ? "" : "?" + query;
    }

    private String selectedClinicLabel(Integer clinicId, List<ClinicService> clinics) {
        if (clinicId == null) {
            return "All clinics";
        }

        if (clinics != null) {
            for (ClinicService clinic : clinics) {
                if (clinic != null && clinic.getClinicId() != null && clinicId.equals(clinic.getClinicId())) {
                    return clinic.getClinicName() == null ? "Clinic " + clinicId : clinic.getClinicName();
                }
            }
        }

        return "Clinic " + clinicId;
    }

    private String selectedServiceLabel(Integer serviceId, List<ClinicServiceStatus> services) {
        if (serviceId == null) {
            return "All services";
        }

        if (services != null) {
            for (ClinicServiceStatus service : services) {
                if (service != null && service.getServiceId() != null && serviceId.equals(service.getServiceId())) {
                    return service.getServiceName() == null ? "Service " + serviceId : service.getServiceName();
                }
            }
        }

        return "Service " + serviceId;
    }

    private String slotCardClass(AppointmentBookingSlot selectedSlot, AppointmentBookingSlot slot) {
        if (slot == null) {
            return "slot-card";
        }

        boolean selected = selectedSlot != null
            && selectedSlot.getClinicServiceId() != null
            && selectedSlot.getClinicServiceId().equals(slot.getClinicServiceId())
            && selectedSlot.getSlotDate() != null
            && selectedSlot.getSlotDate().equals(slot.getSlotDate())
            && selectedSlot.getStartTime() != null
            && selectedSlot.getStartTime().equals(slot.getStartTime())
            && selectedSlot.getEndTime() != null
            && selectedSlot.getEndTime().equals(slot.getEndTime());
        if (selected) {
            return "slot-card selected";
        }

        return slot.isAvailable() ? "slot-card" : "slot-card unavailable";
    }

    private String dayLabel(LocalDate value) {
        return value == null ? "" : value.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private String selectedSlotSummary(AppointmentBookingSlot slot) {
        if (slot == null) {
            return "Select a slot to see the booking details.";
        }

        return slot.getClinicServiceLabel() + " - " + slot.getScheduleLabel();
    }
%>
<%
    User loggedInUser = null;
    Object sessionUser = session.getAttribute("userInfo");
    if (sessionUser instanceof User) {
        loggedInUser = (User) sessionUser;
    }

    if (loggedInUser == null || !"PATIENT".equalsIgnoreCase(loggedInUser.getRole())) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    if (request.getAttribute("availableBookingSlots") == null && request.getAttribute("bookingError") == null && request.getAttribute("bookingMessage") == null) {
        response.sendRedirect(request.getContextPath() + "/patient/appointments/book");
        return;
    }

    User patientUser = (User) request.getAttribute("patientUser");
    if (patientUser == null) {
        patientUser = loggedInUser;
    }

    List<AppointmentBookingSlot> availableBookingSlots = (List<AppointmentBookingSlot>) request.getAttribute("availableBookingSlots");
    if (availableBookingSlots == null) {
        availableBookingSlots = Collections.emptyList();
    }

    List<ClinicService> availableClinics = (List<ClinicService>) request.getAttribute("availableClinics");
    if (availableClinics == null) {
        availableClinics = Collections.emptyList();
    }

    List<ClinicServiceStatus> availableServices = (List<ClinicServiceStatus>) request.getAttribute("availableServices");
    if (availableServices == null) {
        availableServices = Collections.emptyList();
    }

    List<LocalDate> bookingDateOptions = (List<LocalDate>) request.getAttribute("bookingDateOptions");
    if (bookingDateOptions == null) {
        bookingDateOptions = Collections.emptyList();
    }

    String selectedServiceHoursLabel = (String) request.getAttribute("selectedServiceHoursLabel");
    String selectionInstruction = (String) request.getAttribute("selectionInstruction");

    Map<LocalDate, Integer> bookingDateAvailability = (Map<LocalDate, Integer>) request.getAttribute("bookingDateAvailability");
    if (bookingDateAvailability == null) {
        bookingDateAvailability = Collections.emptyMap();
    }

    List<Appointment> recentAppointments = (List<Appointment>) request.getAttribute("recentAppointments");
    if (recentAppointments == null) {
        recentAppointments = Collections.emptyList();
    }

    Appointment activeAppointment = (Appointment) request.getAttribute("activeAppointment");
    QueueTicket activeQueueTicket = (QueueTicket) request.getAttribute("activeQueueTicket");
    Appointment bookingConfirmation = (Appointment) request.getAttribute("bookingConfirmation");
    AppointmentBookingSlot selectedSlot = (AppointmentBookingSlot) request.getAttribute("selectedSlot");

    String bookingError = (String) request.getAttribute("bookingError");
    String bookingMessage = (String) request.getAttribute("bookingMessage");
    String bookingMessageType = (String) request.getAttribute("bookingMessageType");

    LocalDate selectedDate = (LocalDate) request.getAttribute("selectedDate");
    Integer selectedClinicId = (Integer) request.getAttribute("selectedClinicId");
    Integer selectedServiceId = (Integer) request.getAttribute("selectedServiceId");
    Integer activeAppointmentCount = (Integer) request.getAttribute("activeAppointmentCount");
    if (activeAppointmentCount == null) {
        activeAppointmentCount = 0;
    }
    Integer maxActiveAppointments = (Integer) request.getAttribute("maxActiveAppointments");
    if (maxActiveAppointments == null) {
        maxActiveAppointments = 3;
    }
    Integer availableSlotCount = (Integer) request.getAttribute("availableSlotCount");
    if (availableSlotCount == null) {
        availableSlotCount = 0;
        for (AppointmentBookingSlot slot : availableBookingSlots) {
            if (slot != null && slot.isAvailable()) {
                availableSlotCount++;
            }
        }
    }

    String selectedDateParam = request.getParameter("date");
    boolean dateSelectedByUser = selectedDateParam != null && !selectedDateParam.trim().isEmpty() && selectedDate != null;
    String selectedDateLabel = dateSelectedByUser ? formatDate(selectedDate) : "Choose a date";
    String selectedClinicLabel = selectedClinicLabel(selectedClinicId, availableClinics);
    String selectedServiceLabel = selectedServiceLabel(selectedServiceId, availableServices);
    boolean bookingSlotsUnlocked = dateSelectedByUser && selectedClinicId != null && selectedServiceId != null;
    String bookingLockMessage = bookingSlotsUnlocked ? null
        : !dateSelectedByUser
            ? "Choose a date first, then select a clinic and service to unlock appointment times and availability."
            : "Choose a clinic and service for " + selectedDateLabel + " to unlock appointment times and availability.";
    String bookingSummaryText = bookingSlotsUnlocked ? "No slot selected yet" : "Choose a clinic and service to unlock appointment times.";
    String bookingSummaryDetails = bookingSlotsUnlocked
        ? "Select a slot card to see clinic, service, time, approval status, and walk-in rules here."
        : "Once clinic and service are selected for the chosen date, the available time cards will appear here.";
    String bookingClinicSummaryLabel = selectedSlot == null
        ? (selectedClinicId == null ? "Choose a clinic" : selectedClinicLabel)
        : selectedSlot.getClinicName();
    String bookingServiceSummaryLabel = selectedSlot == null
        ? (selectedServiceId == null ? "Choose a service" : selectedServiceLabel)
        : selectedSlot.getServiceName();
    String bookingTimePlaceholder = selectedSlot == null
        ? (bookingSlotsUnlocked ? "Select a slot" : "Locked until clinic and service are chosen")
        : selectedSlot.getTimeRangeLabel();
    String bookingAvailabilityPlaceholder = selectedSlot == null
        ? (bookingSlotsUnlocked ? "Select a slot" : "Locked")
        : selectedSlot.getAvailabilityLabel();
    String bookingSectionNote = bookingSlotsUnlocked
        ? "Each card shows the clinic, service, and exact timing. Disabled cards are already full."
        : "Choose the clinic and service for the selected date to reveal the available appointment times.";
    String bookingSelectionPrompt = bookingSlotsUnlocked
        ? "Select an available time card after the filters are set. The system checks your active booking limit before saving."
        : "Time cards stay locked until you choose both a clinic and a service for the selected date.";
    String availableSlotDisplay = bookingSlotsUnlocked ? String.valueOf(availableSlotCount) : "Locked";
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CCHC Patient - Book Appointment</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
    <%@ include file="../common/nav.jspf" %>

    <main class="container">
        <section class="booking-hero">
            <div class="card booking-hero-copy">
                <span class="hero-badge">Appointment booking</span>
                <h1>Pick a date, then choose the clinic and service to book a time</h1>
                <p class="section-subtitle">Use the filters below to narrow the live appointment times derived from each service's weekly opening hours. The date rail shows the next 14 days and keeps your clinic and service selections in place until the time cards unlock.</p>
                <div class="actions">
                    <a class="btn btn-secondary" href="<%= request.getContextPath() %>/patient/dashboard">Back to dashboard</a>
                    <a class="btn btn-secondary" href="<%= request.getContextPath() %>/patient/clinics">View clinics</a>
                </div>
            </div>
            <div class="card booking-hero-stats">
                <div class="metric-mini">
                    <article class="kpi-card">
                        <h3><%= activeAppointmentCount %></h3>
                        <p>Active appointments</p>
                    </article>
                    <article class="kpi-card">
                        <h3><%= bookingSlotsUnlocked ? availableSlotDisplay : "Locked" %></h3>
                        <p><%= bookingSlotsUnlocked ? "Open appointment times for selected day" : "Choose clinic and service to reveal times" %></p>
                    </article>
                    <article class="kpi-card">
                        <h3><%= activeQueueTicket == null ? "None" : activeQueueTicket.getDisplayCode() %></h3>
                        <p>Queue ticket</p>
                    </article>
                </div>
            </div>
        </section>

        <% if (bookingLockMessage != null) { %>
        <section class="notice" style="margin-bottom: 16px;">
            <%= bookingLockMessage %>
        </section>
        <% } else if (selectedDate != null && selectedServiceId != null) { %>
        <section class="notice" style="margin-bottom: 16px;">
            Service hours for <strong><%= escapeHtml(selectedServiceLabel) %></strong> on <strong><%= selectedDateLabel %></strong>: <strong><%= escapeHtml(selectedServiceHoursLabel == null ? "Closed" : selectedServiceHoursLabel) %></strong>
        </section>
        <% } %>

        <% if (bookingError != null && !bookingError.trim().isEmpty()) { %>
        <section class="notice notice-error booking-message" style="margin-bottom: 16px;">
            <%= bookingError %>
        </section>
        <% } %>

        <% if (bookingMessage != null && !bookingMessage.trim().isEmpty()) { %>
        <section class="notice <%= "error".equalsIgnoreCase(bookingMessageType) ? "notice-error" : "notice-success" %> booking-message" style="margin-bottom: 16px;">
            <%= bookingMessage %>
        </section>
        <% } %>

        <% if (bookingConfirmation != null) { %>
        <section class="card booking-confirmation" style="margin-bottom: 18px;">
            <h2 class="section-title">Booking confirmed</h2>
            <div class="ticket-card">
                <p class="muted"><%= escapeHtml(bookingConfirmation.getClinicServiceLabel()) %></p>
                <p class="ticket-number"><%= escapeHtml(bookingConfirmation.getDisplayCode()) %></p>
                <p><span class="status-chip <%= bookingConfirmation.getStatusChipClass() %>"><%= bookingConfirmation.getStatus() %></span></p>
                <p>Schedule: <%= bookingConfirmation.getScheduleLabel() %></p>
                <p>Reference saved to your booking history.</p>
            </div>
        </section>
        <% } %>

        <section class="booking-layout">
            <div class="booking-main">
                <section class="card">
                    <h2 class="section-title">Select a date</h2>
                    <p class="booking-note">Choose a date first. After date selection, clinic and service filters will appear below and then the matching time cards can be loaded.</p>
                    <div class="date-rail">
                        <% for (LocalDate dateOption : bookingDateOptions) {
                            Integer openCount = bookingDateAvailability.get(dateOption);
                            if (openCount == null) {
                                openCount = 0;
                            }
                        %>
                        <a class="date-card <%= selectedDateClass(dateSelectedByUser ? selectedDate : null, dateOption) %>" href="<%= request.getContextPath() %>/patient/appointments/book<%= buildBookingQuery(dateOption, null, null, null, null, null) %>">
                            <span class="date-card-day"><%= escapeHtml(dayLabel(dateOption)) %></span>
                            <span class="date-card-value"><%= formatDate(dateOption) %></span>
                            <span class="date-card-count"><%= openCount %> open slots</span>
                        </a>
                        <% } %>
                    </div>

                    <div id="clinicServiceSelectionPanel" style="margin-top: 16px;" <%= dateSelectedByUser ? "" : "hidden" %>>
                        <h3 style="margin-bottom: 8px;">Select clinic and service</h3>
                        <p class="booking-note">Now choose both clinic and service for the selected date to unlock time and availability selection.</p>
                        <form class="filter-form" id="clinicServiceFilterForm" action="<%= request.getContextPath() %>/patient/appointments/book" method="get">
                            <input type="hidden" name="date" id="clinicServiceDateInput" value="<%= dateSelectedByUser && selectedDate != null ? selectedDate.format(ISO_DATE_FORMATTER) : "" %>">
                            <div class="field">
                                <label for="clinicId">Clinic</label>
                                <select id="clinicId" name="clinicId" <%= dateSelectedByUser ? "" : "disabled" %>>
                                    <option value="" <%= selectedClinicId == null ? "selected" : "" %>>All clinics</option>
                                    <% for (ClinicService clinic : availableClinics) { %>
                                    <option value="<%= clinic.getClinicId() %>" <%= clinic.getClinicId() != null && clinic.getClinicId().equals(selectedClinicId) ? "selected" : "" %>><%= escapeHtml(clinic.getClinicName()) %></option>
                                    <% } %>
                                </select>
                            </div>
                            <div class="field">
                                <label for="serviceId">Service</label>
                                <select id="serviceId" name="serviceId" <%= dateSelectedByUser ? "" : "disabled" %>>
                                    <option value="" <%= selectedServiceId == null ? "selected" : "" %>>All services</option>
                                    <% for (ClinicServiceStatus service : availableServices) { %>
                                    <option value="<%= service.getServiceId() %>" <%= service.getServiceId() != null && service.getServiceId().equals(selectedServiceId) ? "selected" : "" %>><%= escapeHtml(service.getServiceName()) %></option>
                                    <% } %>
                                </select>
                            </div>
                            <div class="filter-actions">
                                <button class="btn btn-primary" type="submit">Apply clinic/service</button>
                                <a class="btn btn-secondary" id="clearClinicServiceLink" href="<%= request.getContextPath() %>/patient/appointments/book<%= buildBookingQuery(selectedDate, null, null, null, null, null) %>">Clear clinic/service</a>
                            </div>
                        </form>
                    </div>
                </section>

                <section class="card" style="margin-top: 16px;">
                    <div class="layout-split booking-section-header">
                        <div>
                            <h2 class="section-title">Available appointment details</h2>
                            <p class="booking-note"><%= bookingSectionNote %></p>
                        </div>
                        <div class="booking-day-summary">
                            <div class="summary-box">
                                <h4><%= availableSlotDisplay %></h4>
                                <p><%= bookingSlotsUnlocked ? "Selectable slots" : "Slots locked" %></p>
                            </div>
                            <div class="summary-box">
                                <h4><%= selectedDateLabel %></h4>
                                <p>Current day</p>
                            </div>
                        </div>
                    </div>

                    <form class="booking-form" action="<%= request.getContextPath() %>/patient/appointments/book" method="post" id="bookingForm">
                        <input type="hidden" name="date" id="bookingDateInput" value="<%= selectedDate == null ? "" : selectedDate.format(ISO_DATE_FORMATTER) %>">
                        <input type="hidden" name="clinicId" id="selectedClinicIdInput" value="<%= selectedClinicId == null ? "" : selectedClinicId %>">
                        <input type="hidden" name="serviceId" id="selectedServiceIdInput" value="<%= selectedServiceId == null ? "" : selectedServiceId %>">
                        <input type="hidden" name="slotStartTime" id="selectedSlotStartTimeInput" value="<%= selectedSlot == null || selectedSlot.getStartTime() == null ? "" : selectedSlot.getStartTime() %>">
                        <input type="hidden" name="slotEndTime" id="selectedSlotEndTimeInput" value="<%= selectedSlot == null || selectedSlot.getEndTime() == null ? "" : selectedSlot.getEndTime() %>">

                        <div class="slot-grid" id="slotGrid">
                            <% if (!dateSelectedByUser) { %>
                            <div class="booking-empty-state">
                                <h3>Choose a date to start booking</h3>
                                <p>Select a date from the rail or date picker first. Then pick a clinic and service to unlock the appointment times and availability below.</p>
                            </div>
                            <% } else if (!bookingSlotsUnlocked) { %>
                            <div class="booking-empty-state">
                                <h3>Choose a clinic and service to unlock time selection</h3>
                                <p>Appointment times stay hidden until both filters are set for the selected date. Once you choose them, available time cards and availability will appear here.</p>
                            </div>
                            <% } else if (availableBookingSlots.isEmpty()) { %>
                            <div class="booking-empty-state">
                                <h3>No slots are open on this date</h3>
                                <p>Try another date from the rail above. If you still cannot find a time, the clinic has not published a booking window yet.</p>
                            </div>
                            <% } else {
                                for (AppointmentBookingSlot slot : availableBookingSlots) {
                                    boolean selected = selectedSlot != null
                                        && selectedSlot.getClinicServiceId() != null
                                        && selectedSlot.getClinicServiceId().equals(slot.getClinicServiceId())
                                        && selectedSlot.getSlotDate() != null
                                        && selectedSlot.getSlotDate().equals(slot.getSlotDate())
                                        && selectedSlot.getStartTime() != null
                                        && selectedSlot.getStartTime().equals(slot.getStartTime())
                                        && selectedSlot.getEndTime() != null
                                        && selectedSlot.getEndTime().equals(slot.getEndTime());
                                    String selectedMarker = selected ? "selected" : "";
                            %>
                            <button
                                type="button"
                                class="<%= slotCardClass(selectedSlot, slot) %>"
                                data-slot-card="true"
                                data-clinic-id="<%= slot.getClinicId() %>"
                                data-service-id="<%= slot.getServiceId() %>"
                                data-clinic-name="<%= escapeHtml(slot.getClinicName()) %>"
                                data-service-name="<%= escapeHtml(slot.getServiceName()) %>"
                                data-clinic-service-label="<%= escapeHtml(slot.getClinicServiceLabel()) %>"
                                data-schedule-label="<%= escapeHtml(slot.getScheduleLabel()) %>"
                                data-date-label="<%= escapeHtml(slot.getDateLabel()) %>"
                                data-time-range-label="<%= escapeHtml(slot.getTimeRangeLabel()) %>"
                                data-slot-start-time="<%= slot.getStartTime() == null ? "" : slot.getStartTime() %>"
                                data-slot-end-time="<%= slot.getEndTime() == null ? "" : slot.getEndTime() %>"
                                data-capacity-summary="<%= escapeHtml(slot.getCapacitySummary()) %>"
                                data-availability-label="<%= escapeHtml(slot.getAvailabilityLabel()) %>"
                                data-approval-label="<%= escapeHtml(slot.getApprovalLabel()) %>"
                                data-walkin-label="<%= escapeHtml(slot.getWalkInLabel()) %>"
                                data-service-description="<%= escapeHtml(slot.getServiceDescription()) %>"
                                data-summary-text="<%= escapeHtml(selectedSlotSummary(slot)) %>"
                                data-summary-note="<%= escapeHtml(slot.getApprovalLabel() + " | " + slot.getWalkInLabel()) %>"
                                data-selected="<%= selectedMarker %>"
                                aria-pressed="<%= selected ? "true" : "false" %>"
                                <%= slot.isAvailable() ? "" : "disabled" %>>
                                <div class="slot-card-head">
                                    <div>
                                        <h3><%= escapeHtml(slot.getClinicServiceLabel()) %></h3>
                                        <p><%= escapeHtml(slot.getDateLabel()) %> <%= escapeHtml(slot.getTimeRangeLabel()) %></p>
                                    </div>
                                    <span class="status-chip <%= slot.getAvailabilityChipClass() %>"><%= escapeHtml(slot.getAvailabilityLabel()) %></span>
                                </div>
                                <p class="slot-card-time"><%= escapeHtml(slot.getScheduleLabel()) %></p>
                                <div class="slot-meta">
                                    <span class="slot-meta-pill slot-meta-capacity"><%= escapeHtml(slot.getCapacitySummary()) %> capacity</span>
                                    <span class="slot-meta-pill slot-meta-approval"><%= escapeHtml(slot.getApprovalLabel()) %></span>
                                    <span class="slot-meta-pill slot-meta-walkin"><%= escapeHtml(slot.getWalkInLabel()) %></span>
                                </div>
                                <p class="booking-note"><%= safeText(slot.getServiceDescription()) %></p>
                            </button>
                            <%      }
                               } %>
                        </div>

                        <div class="booking-form-actions">
                            <button class="btn btn-primary" type="submit" id="confirmBookingButton" <%= selectedSlot == null || !bookingSlotsUnlocked ? "disabled" : "" %>>Confirm appointment</button>
                            <button class="btn btn-secondary" type="button" id="clearSelectionButton">Clear selection</button>
                        </div>

                    </form>
                </section>

                <!-- <section class="card" style="margin-top: 16px;">
                    <h2 class="section-title">Recent appointments</h2>
                    <div class="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>Appointment</th>
                                    <th>Schedule</th>
                                    <th>Clinic / Service</th>
                                    <th>Status</th>
                                    <th>Note</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (recentAppointments.isEmpty()) { %>
                                <tr>
                                    <td colspan="5">No appointments have been recorded for your account yet.</td>
                                </tr>
                                <% } else {
                                    for (Appointment appointment : recentAppointments) {
                                %>
                                <tr>
                                    <td><%= escapeHtml(appointment.getDisplayCode()) %></td>
                                    <td><%= escapeHtml(appointment.getScheduleLabel()) %></td>
                                    <td><%= escapeHtml(appointment.getClinicServiceLabel()) %></td>
                                    <td><span class="status-chip <%= appointment.getStatusChipClass() %>"><%= escapeHtml(appointment.getStatus()) %></span></td>
                                    <td><%= safeText(appointment.getOutcomeNotes() == null ? appointment.getReviewReason() : appointment.getOutcomeNotes()) %></td>
                                </tr>
                                <%      }
                                   } %>
                            </tbody>
                        </table>
                    </div>
                </section> -->
            </div>

            <aside class="booking-summary card">
                <h2 class="section-title">Booking summary</h2>

                <div class="ticket-card">
                    <p class="muted">Patient account</p>
                    <p class="ticket-number"><%= patientCode(patientUser.getUserId()) %></p>
                    <p><%= safeText(patientUser.getFullName()) %></p>
                    <p><%= safeText(patientUser.getEmail()) %></p>
                </div>

                <div class="quick-note" aria-live="polite">
                    <strong id="selectedSlotSummary"><%= selectedSlot == null ? escapeHtml(bookingSummaryText) : escapeHtml(selectedSlotSummary(selectedSlot)) %></strong>
                    <div id="selectedSlotDetails" style="margin-top: 4px;"><%= selectedSlot == null ? escapeHtml(bookingSummaryDetails) : escapeHtml(selectedSlot.getApprovalLabel() + " | " + selectedSlot.getWalkInLabel()) %></div>
                </div>

                <div class="booking-summary-list">
                    <div class="booking-summary-row"><span>Selected date</span><strong id="selectedDateLabel"><%= selectedDateLabel %></strong></div>
                    <div class="booking-summary-row"><span>Selected clinic</span><strong id="selectedClinicLabel"><%= selectedSlot == null ? escapeHtml(bookingClinicSummaryLabel) : escapeHtml(selectedSlot.getClinicName()) %></strong></div>
                    <div class="booking-summary-row"><span>Selected service</span><strong id="selectedServiceLabel"><%= selectedSlot == null ? escapeHtml(bookingServiceSummaryLabel) : escapeHtml(selectedSlot.getServiceName()) %></strong></div>
                    <div class="booking-summary-row"><span>Selected time</span><strong id="selectedTimeLabel"><%= selectedSlot == null ? escapeHtml(bookingTimePlaceholder) : escapeHtml(selectedSlot.getTimeRangeLabel()) %></strong></div>
                    <div class="booking-summary-row"><span>Availability</span><strong id="selectedAvailabilityLabel"><%= selectedSlot == null ? escapeHtml(bookingAvailabilityPlaceholder) : escapeHtml(selectedSlot.getAvailabilityLabel()) %></strong></div>
                </div>

                <div class="ticket-card">
                    <% if (activeAppointment == null) { %>
                    <p class="muted">No active appointment</p>
                    <p>You do not have a pending, confirmed, or arrived appointment at the moment.</p>
                    <% } else { %>
                    <p class="muted"><%= escapeHtml(activeAppointment.getClinicServiceLabel()) %></p>
                    <p class="ticket-number"><%= escapeHtml(activeAppointment.getDisplayCode()) %></p>
                    <p><span class="status-chip <%= activeAppointment.getStatusChipClass() %>"><%= escapeHtml(activeAppointment.getStatus()) %></span></p>
                    <p>Schedule: <%= escapeHtml(activeAppointment.getScheduleLabel()) %></p>
                    <% } %>
                </div>

                <div class="ticket-card">
                    <% if (activeQueueTicket == null) { %>
                    <p class="muted">No active queue ticket</p>
                    <p>You are not currently waiting in a same-day queue.</p>
                    <% } else { %>
                    <p class="muted"><%= escapeHtml(activeQueueTicket.getClinicServiceLabel()) %></p>
                    <p class="ticket-number"><%= escapeHtml(activeQueueTicket.getDisplayCode()) %></p>
                    <p><span class="status-chip <%= activeQueueTicket.getStatusChipClass() %>"><%= escapeHtml(activeQueueTicket.getStatus()) %></span></p>
                    <p>Queue date: <%= escapeHtml(activeQueueTicket.getQueueLabel()) %></p>
                    <p>Estimated wait: <%= escapeHtml(activeQueueTicket.getEstimatedWaitLabel()) %></p>
                    <% } %>
                </div>

                <div class="ticket-card">
                    <p class="muted">Appointment rules</p>
                    <p>Slots are checked against the live database. Full slots are disabled and the patient booking limit is enforced before save.</p>
                </div>
            </aside>
        </section>
    </main>

    <script id="bookingPageScript">
        (function () {
            const bookingPageScriptId = 'bookingPageScript';
            const previousController = window.__bookingPageController;
            if (previousController) {
                previousController.abort();
            }

            const controller = new AbortController();
            window.__bookingPageController = controller;
            const signal = controller.signal;

            const slotButtons = Array.from(document.querySelectorAll('[data-slot-card="true"]'));
            const confirmButton = document.getElementById('confirmBookingButton');
            const clearButton = document.getElementById('clearSelectionButton');
            const selectedClinicIdInput = document.getElementById('selectedClinicIdInput');
            const selectedServiceIdInput = document.getElementById('selectedServiceIdInput');
            const selectedSlotStartTimeInput = document.getElementById('selectedSlotStartTimeInput');
            const selectedSlotEndTimeInput = document.getElementById('selectedSlotEndTimeInput');
            const selectedSlotSummary = document.getElementById('selectedSlotSummary');
            const selectedSlotDetails = document.getElementById('selectedSlotDetails');
            const selectedDateLabel = document.getElementById('selectedDateLabel');
            const selectedClinicLabel = document.getElementById('selectedClinicLabel');
            const selectedServiceLabel = document.getElementById('selectedServiceLabel');
            const selectedTimeLabel = document.getElementById('selectedTimeLabel');
            const selectedAvailabilityLabel = document.getElementById('selectedAvailabilityLabel');
            const bookingDateFilterInput = document.getElementById('bookingDate');
            const bookingDateFilterForm = document.getElementById('bookingDateFilterForm');
            const clinicServiceFilterForm = document.getElementById('clinicServiceFilterForm');
            const bookingDateCards = Array.from(document.querySelectorAll('.date-rail .date-card[href]'));
            const bookingDateResetLink = document.getElementById('bookingDateResetLink');
            const clearClinicServiceLink = document.getElementById('clearClinicServiceLink');
            const clinicServiceSelectionPanel = document.getElementById('clinicServiceSelectionPanel');
            const clinicServiceDateInput = document.getElementById('clinicServiceDateInput');
            const clinicFilterSelect = document.getElementById('clinicId');
            const serviceFilterSelect = document.getElementById('serviceId');
            const defaultSummaryText = '<%= escapeHtml(bookingSummaryText) %>';
            const defaultSummaryDetails = '<%= escapeHtml(bookingSummaryDetails) %>';
            const defaultDateLabel = '<%= escapeHtml(selectedDateLabel) %>';
            const defaultClinicLabel = '<%= escapeHtml(bookingClinicSummaryLabel) %>';
            const defaultServiceLabel = '<%= escapeHtml(bookingServiceSummaryLabel) %>';
            const defaultTimeLabel = '<%= escapeHtml(bookingTimePlaceholder) %>';
            const defaultAvailabilityLabel = '<%= escapeHtml(bookingAvailabilityPlaceholder) %>';
            const defaultClinicIdValue = selectedClinicIdInput.value;
            const defaultServiceIdValue = selectedServiceIdInput.value;
            const initialDateFilterValue = bookingDateFilterInput ? bookingDateFilterInput.value : '';

            const isPlainLeftClick = function (event) {
                return event.button === 0 && !event.metaKey && !event.ctrlKey && !event.shiftKey && !event.altKey;
            };

            const buildFormUrl = function (form) {
                if (!form) {
                    return window.location.href;
                }

                const requestUrl = new URL(form.getAttribute('action') || window.location.href, window.location.href);
                requestUrl.search = new URLSearchParams(new FormData(form)).toString();
                return requestUrl.toString();
            };

            const navigateWithoutReload = async function (targetUrl, updateHistory) {
                if (!targetUrl) {
                    return;
                }

                if (typeof updateHistory === 'undefined') {
                    updateHistory = true;
                }

                const normalizedUrl = new URL(targetUrl, window.location.href).toString();
                const previousRequestController = window.__bookingPageNavigationController;
                if (previousRequestController) {
                    previousRequestController.abort();
                }

                const requestController = new AbortController();
                window.__bookingPageNavigationController = requestController;

                let response;
                try {
                    response = await fetch(normalizedUrl, {
                        credentials: 'same-origin',
                        headers: {
                            'X-Requested-With': 'XMLHttpRequest'
                        },
                        signal: requestController.signal
                    });
                } catch (error) {
                    if (error && error.name === 'AbortError') {
                        return;
                    }
                    window.__bookingPageNavigationController = null;
                    window.location.assign(normalizedUrl);
                    return;
                }

                if (!response.ok) {
                    window.__bookingPageNavigationController = null;
                    window.location.assign(normalizedUrl);
                    return;
                }

                let html;
                try {
                    html = await response.text();
                } catch (error) {
                    window.__bookingPageNavigationController = null;
                    window.location.assign(response.url || normalizedUrl);
                    return;
                }

                const nextDocument = new DOMParser().parseFromString(html, 'text/html');
                const nextMain = nextDocument.querySelector('main.container');
                const nextScript = nextDocument.getElementById(bookingPageScriptId);
                const currentMain = document.querySelector('main.container');
                const currentScript = document.getElementById(bookingPageScriptId);

                if (!nextMain || !nextScript || !currentMain || !currentScript) {
                    window.__bookingPageNavigationController = null;
                    window.location.assign(response.url || normalizedUrl);
                    return;
                }

                const nextUrl = response.url || normalizedUrl;
                document.title = nextDocument.title || document.title;
                if (window.location.href !== nextUrl) {
                    if (updateHistory) {
                        history.pushState({ bookingPage: true }, '', nextUrl);
                    } else {
                        history.replaceState({ bookingPage: true }, '', nextUrl);
                    }
                } else if (updateHistory) {
                    history.replaceState({ bookingPage: true }, '', nextUrl);
                }

                currentMain.innerHTML = nextMain.innerHTML;
                window.__bookingPageNavigationController = null;

                const replacementScript = document.createElement('script');
                replacementScript.id = bookingPageScriptId;
                replacementScript.textContent = nextScript.textContent || '';
                currentScript.replaceWith(replacementScript);
            };

            const updateFilterLockState = function () {
                if (!bookingDateFilterInput) {
                    return;
                }

                const hasDate = (bookingDateFilterInput.value || '').trim().length > 0;
                if (clinicServiceSelectionPanel) {
                    clinicServiceSelectionPanel.hidden = !hasDate;
                }
                if (clinicServiceDateInput) {
                    clinicServiceDateInput.value = hasDate ? bookingDateFilterInput.value : '';
                }
                if (clinicFilterSelect) {
                    clinicFilterSelect.disabled = !hasDate;
                    if (!hasDate) {
                        clinicFilterSelect.value = '';
                    }
                }
                if (serviceFilterSelect) {
                    serviceFilterSelect.disabled = !hasDate;
                    if (!hasDate) {
                        serviceFilterSelect.value = '';
                    }
                }
            };

            const clearSelection = function () {
                if (selectedClinicIdInput) {
                    selectedClinicIdInput.value = defaultClinicIdValue;
                }
                if (selectedServiceIdInput) {
                    selectedServiceIdInput.value = defaultServiceIdValue;
                }
                if (selectedSlotStartTimeInput) {
                    selectedSlotStartTimeInput.value = '';
                }
                if (selectedSlotEndTimeInput) {
                    selectedSlotEndTimeInput.value = '';
                }
                if (confirmButton) {
                    confirmButton.disabled = true;
                }
                slotButtons.forEach(function (button) {
                    button.classList.remove('selected');
                    button.setAttribute('aria-pressed', 'false');
                });
                if (selectedSlotSummary) {
                    selectedSlotSummary.textContent = defaultSummaryText;
                }
                if (selectedSlotDetails) {
                    selectedSlotDetails.textContent = defaultSummaryDetails;
                }
                selectedDateLabel.textContent = defaultDateLabel;
                selectedClinicLabel.textContent = defaultClinicLabel;
                selectedServiceLabel.textContent = defaultServiceLabel;
                selectedTimeLabel.textContent = defaultTimeLabel;
                selectedAvailabilityLabel.textContent = defaultAvailabilityLabel;
            };

            const selectSlot = function (button) {
                if (!button || button.disabled) {
                    return;
                }

                slotButtons.forEach(function (item) {
                    item.classList.remove('selected');
                    item.setAttribute('aria-pressed', 'false');
                });
                button.classList.add('selected');
                button.setAttribute('aria-pressed', 'true');

                selectedClinicIdInput.value = button.getAttribute('data-clinic-id') || '';
                selectedServiceIdInput.value = button.getAttribute('data-service-id') || '';
                selectedSlotStartTimeInput.value = button.getAttribute('data-slot-start-time') || '';
                selectedSlotEndTimeInput.value = button.getAttribute('data-slot-end-time') || '';
                if (selectedSlotSummary) {
                    selectedSlotSummary.textContent = button.getAttribute('data-summary-text') || defaultSummaryText;
                }
                if (selectedSlotDetails) {
                    selectedSlotDetails.textContent = button.getAttribute('data-summary-note') || defaultSummaryDetails;
                }
                selectedDateLabel.textContent = button.getAttribute('data-date-label') || defaultDateLabel;
                selectedClinicLabel.textContent = button.getAttribute('data-clinic-name') || defaultClinicLabel;
                selectedServiceLabel.textContent = button.getAttribute('data-service-name') || defaultServiceLabel;
                selectedTimeLabel.textContent = button.getAttribute('data-time-range-label') || 'Select a slot';
                selectedAvailabilityLabel.textContent = button.getAttribute('data-availability-label') || 'Select a slot';
                if (confirmButton) {
                    confirmButton.disabled = false;
                }
            };

            slotButtons.forEach(function (button) {
                button.addEventListener('click', function () {
                    selectSlot(button);
                }, { signal: signal });
            });

            if (clearButton) {
                clearButton.addEventListener('click', function () {
                    clearSelection();
                }, { signal: signal });
            }

            if (bookingDateFilterForm) {
                bookingDateFilterForm.addEventListener('submit', function (event) {
                    event.preventDefault();
                    navigateWithoutReload(buildFormUrl(bookingDateFilterForm));
                }, { signal: signal });
            }

            if (clinicServiceFilterForm) {
                clinicServiceFilterForm.addEventListener('submit', function (event) {
                    event.preventDefault();
                    navigateWithoutReload(buildFormUrl(clinicServiceFilterForm));
                }, { signal: signal });
            }

            bookingDateCards.forEach(function (button) {
                button.addEventListener('click', function (event) {
                    if (!isPlainLeftClick(event)) {
                        return;
                    }
                    event.preventDefault();
                    navigateWithoutReload(button.href);
                }, { signal: signal });
            });

            if (bookingDateResetLink) {
                bookingDateResetLink.addEventListener('click', function (event) {
                    if (!isPlainLeftClick(event)) {
                        return;
                    }
                    event.preventDefault();
                    navigateWithoutReload(bookingDateResetLink.href);
                }, { signal: signal });
            }

            if (clearClinicServiceLink) {
                clearClinicServiceLink.addEventListener('click', function (event) {
                    if (!isPlainLeftClick(event)) {
                        return;
                    }
                    event.preventDefault();
                    navigateWithoutReload(clearClinicServiceLink.href);
                }, { signal: signal });
            }

            window.addEventListener('popstate', function () {
                navigateWithoutReload(window.location.href, false);
            }, { signal: signal });

            const initiallySelected = slotButtons.find(function (button) {
                return button.classList.contains('selected');
            });

            if (initiallySelected) {
                selectSlot(initiallySelected);
            } else {
                clearSelection();
            }

            if (bookingDateFilterInput) {
                bookingDateFilterInput.addEventListener('change', function () {
                    const nextDateValue = bookingDateFilterInput.value || '';
                    if (nextDateValue !== initialDateFilterValue) {
                        if (clinicFilterSelect) {
                            clinicFilterSelect.value = '';
                        }
                        if (serviceFilterSelect) {
                            serviceFilterSelect.value = '';
                        }
                    }
                    updateFilterLockState();
                }, { signal: signal });
            }

            updateFilterLockState();
            if (bookingDateFilterInput && (bookingDateFilterInput.value || '').trim().length === 0) {
                clearSelection();
            }
        })();
    </script>

    <footer class="site-footer">
        <div class="container">Patient booking connected to the live clinic slot database.</div>
    </footer>
</body>
</html>