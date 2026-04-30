<%@page import="ict.bean.Appointment"%>
<%@page import="ict.bean.QueueTicket"%>
<%@page import="ict.bean.User"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.time.LocalDateTime"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!
    private static final DateTimeFormatter PROFILE_DATE = DateTimeFormatter.ofPattern("d-M-yyyy");
    private static final DateTimeFormatter PROFILE_DATE_TIME = DateTimeFormatter.ofPattern("d-M-yyyy H:mm");

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
        return value == null ? "-" : PROFILE_DATE.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : PROFILE_DATE_TIME.format(value);
    }

    private String patientCode(Integer userId) {
        return userId == null ? "Patient" : "P-" + userId;
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

    if (request.getAttribute("patientUser") == null && request.getAttribute("dashboardError") == null) {
            response.sendRedirect(request.getContextPath() + "/patient/dashboard");
            return;
    }

    User patientUser = (User) request.getAttribute("patientUser");
    if (patientUser == null) {
            patientUser = loggedInUser;
    }

    List<Appointment> recentAppointments = (List<Appointment>) request.getAttribute("recentAppointments");
    if (recentAppointments == null) {
            recentAppointments = Collections.emptyList();
    }

    Appointment activeAppointment = (Appointment) request.getAttribute("activeAppointment");
    QueueTicket activeQueueTicket = (QueueTicket) request.getAttribute("activeQueueTicket");
    String dashboardError = (String) request.getAttribute("dashboardError");
    Integer activeAppointmentCount = (Integer) request.getAttribute("activeAppointmentCount");
    if (activeAppointmentCount == null) {
            activeAppointmentCount = 0;
    }
    Integer recentAppointmentCount = (Integer) request.getAttribute("recentAppointmentCount");
    if (recentAppointmentCount == null) {
            recentAppointmentCount = recentAppointments.size();
    }
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CCHC Patient Dashboard</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
    <%@ include file="common/nav.jspf" %>

    <main class="container">
        <section>
            <h1>Patient Dashboard</h1>
            <p class="section-subtitle">Review your active appointment and queue records from the live clinic database.</p>
        </section>

        <% if (dashboardError != null && !dashboardError.trim().isEmpty()) { %>
        <section class="notice" style="margin-bottom: 16px;">
            <%= dashboardError %>
        </section>
        <% } %>

        <section class="summary-strip">
            <div class="summary-box">
                <h4><%= patientCode(patientUser.getUserId()) %></h4>
                <p>Patient ID</p>
            </div>
            <div class="summary-box">
                <h4><%= activeAppointmentCount %></h4>
                <p>Active appointments</p>
            </div>
            <div class="summary-box">
                <h4><%= activeQueueTicket == null ? "None" : activeQueueTicket.getDisplayCode() %></h4>
                <p>Queue status</p>
            </div>
            <div class="summary-box">
                <h4><%= recentAppointmentCount %></h4>
                <p>Recent bookings</p>
            </div>
        </section>

        <section class="layout-split">
            <div class="card">
                <h2 class="section-title">Account summary</h2>
                <div class="list-group">
                    <div class="list-item"><h4>Patient ID</h4><p><%= patientCode(patientUser.getUserId()) %></p></div>
                    <div class="list-item"><h4>Full name</h4><p><%= safeText(patientUser.getFullName()) %></p></div>
                    <div class="list-item"><h4>Email</h4><p><%= safeText(patientUser.getEmail()) %></p></div>
                    <div class="list-item"><h4>Phone</h4><p><%= safeText(patientUser.getPhone()) %></p></div>
                    <div class="list-item"><h4>Date of birth</h4><p><%= formatDate(patientUser.getDateOfBirth()) %></p></div>
                    <div class="list-item"><h4>Gender</h4><p><%= safeText(patientUser.getGender()) %></p></div>
                    <div class="list-item"><h4>Account status</h4><p><%= patientUser.getIsActive() == 1 ? "Active" : "Inactive" %></p></div>
                    <div class="list-item"><h4>Last login</h4><p><%= formatDateTime(patientUser.getLastLoginAt()) %></p></div>
                </div>
            </div>

            <aside class="card">
                <h2 class="section-title">Current activity</h2>
                <% if (activeAppointment == null) { %>
                <div class="ticket-card">
                    <p class="muted">No active appointment</p>
                    <p>You do not have a pending, confirmed, or arrived appointment at the moment.</p>
                </div>
                <% } else { %>
                <div class="ticket-card">
                    <p class="muted"><%= escapeHtml(activeAppointment.getClinicServiceLabel()) %></p>
                    <p class="ticket-number"><%= escapeHtml(activeAppointment.getDisplayCode()) %></p>
                    <p><span class="status-chip <%= activeAppointment.getStatusChipClass() %>"><%= activeAppointment.getStatus() %></span></p>
                    <p>Schedule: <%= activeAppointment.getScheduleLabel() %></p>
                    <p>Reviewed by: <%= safeText(activeAppointment.getReviewedBy()) %></p>
                    <% if (activeAppointment.getReviewReason() != null && !activeAppointment.getReviewReason().trim().isEmpty()) { %>
                    <p><%= safeText(activeAppointment.getReviewReason()) %></p>
                    <% } %>
                </div>
                <% } %>

                <div style="height: 14px;"></div>

                <% if (activeQueueTicket == null) { %>
                <div class="ticket-card">
                    <p class="muted">No active queue ticket</p>
                    <p>You are not currently waiting in a same-day queue.</p>
                </div>
                <% } else { %>
                <div class="ticket-card">
                    <p class="muted"><%= escapeHtml(activeQueueTicket.getClinicServiceLabel()) %></p>
                    <p class="ticket-number"><%= escapeHtml(activeQueueTicket.getDisplayCode()) %></p>
                    <p><span class="status-chip <%= activeQueueTicket.getStatusChipClass() %>"><%= activeQueueTicket.getStatus() %></span></p>
                    <p>Queue date: <%= activeQueueTicket.getQueueLabel() %></p>
                    <p>Estimated wait: <%= activeQueueTicket.getEstimatedWaitLabel() %></p>
                </div>
                <% } %>

                <div class="action-bar" style="margin-top: 12px;">
                    <a class="btn btn-primary" href="<%= request.getContextPath() %>/patient/appointments/book">Book Appointment</a>
                    <a class="btn btn-primary" href="<%= request.getContextPath() %>/patient/clinics">Browse Clinics</a>
                </div>
            </aside>
        </section>


    </main>

    <footer class="site-footer">
        <div class="container">Patient dashboard connected to live booking and queue data.</div>
    </footer>
</body>
</html>
