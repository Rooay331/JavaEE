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

  if (loggedInUser == null || !"STAFF".equalsIgnoreCase(loggedInUser.getRole()) || loggedInUser.getClinicId() == null) {
      response.sendRedirect(request.getContextPath() + "/login");
      return;
  }

  if (request.getAttribute("patientUser") == null && request.getAttribute("patientError") == null) {
      response.sendRedirect(request.getContextPath() + "/staff/patients/search");
      return;
  }

  User patientUser = (User) request.getAttribute("patientUser");
  QueueTicket activeQueueTicket = (QueueTicket) request.getAttribute("activeQueueTicket");
  List<Appointment> recentAppointments = (List<Appointment>) request.getAttribute("recentAppointments");
  if (recentAppointments == null) {
      recentAppointments = Collections.emptyList();
  }

  String patientError = (String) request.getAttribute("patientError");
  Integer assignedClinicId = (Integer) request.getAttribute("assignedClinicId");
  if (assignedClinicId == null) {
      assignedClinicId = loggedInUser.getClinicId();
  }
  Object assignedClinicName = request.getAttribute("assignedClinicName");
  String patientId = (String) request.getAttribute("patientId");
  if (patientId == null && patientUser != null) {
      patientId = String.valueOf(patientUser.getUserId());
  }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Staff - Patient Details</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="../common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Patient Details</h1>
      <p class="section-subtitle">View patient profile, recent appointments, and current queue activity.</p>
    </section>

    <section class="notice" style="margin-bottom: 18px;">
      Patient record is scoped to <strong><%= assignedClinicName == null ? ("Clinic ID " + assignedClinicId) : assignedClinicName %></strong>.
    </section>

    <% if (patientError != null && !patientError.trim().isEmpty()) { %>
    <section class="notice" style="margin-bottom: 16px;">
      <%= patientError %>
    </section>
    <% } %>

    <% if (patientUser != null) { %>
    <section class="summary-strip">
      <div class="summary-box">
        <h4><%= patientCode(patientUser.getUserId()) %></h4>
        <p>Patient ID</p>
      </div>
      <div class="summary-box">
        <h4><%= patientUser.getFullName() == null ? "-" : escapeHtml(patientUser.getFullName()) %></h4>
        <p>Full name</p>
      </div>
      <div class="summary-box">
        <h4><%= patientUser.getPhone() == null ? "-" : escapeHtml(patientUser.getPhone()) %></h4>
        <p>Phone</p>
      </div>
      <div class="summary-box">
        <h4><%= activeQueueTicket == null ? "None" : activeQueueTicket.getDisplayCode() %></h4>
        <p>Current queue</p>
      </div>
      <div class="summary-box">
        <h4><%= recentAppointments.size() %></h4>
        <p>Recent appointments</p>
      </div>
    </section>
    <% } %>

    <section class="layout-split">
      <div class="card">
        <h2 class="section-title">Profile summary</h2>
        <% if (patientUser == null) { %>
        <p>No patient profile was loaded.</p>
        <% } else { %>
        <div class="list-group">
          <div class="list-item"><h4>Patient ID</h4><p><%= patientCode(patientUser.getUserId()) %></p></div>
          <div class="list-item"><h4>Full name</h4><p><%= safeText(patientUser.getFullName()) %></p></div>
          <div class="list-item"><h4>Email</h4><p><%= safeText(patientUser.getEmail()) %></p></div>
          <div class="list-item"><h4>Phone</h4><p><%= safeText(patientUser.getPhone()) %></p></div>
          <div class="list-item"><h4>Date of birth</h4><p><%= formatDate(patientUser.getDateOfBirth()) %></p></div>
          <div class="list-item"><h4>Gender</h4><p><%= safeText(patientUser.getGender()) %></p></div>
        </div>
        <% } %>
      </div>

      <aside class="card">
        <h2 class="section-title">Current queue ticket</h2>
        <% if (activeQueueTicket == null) { %>
        <div class="ticket-card">
          <p class="muted">No active queue ticket</p>
          <p>There is no waiting or called queue record for this patient at the assigned clinic.</p>
        </div>
        <% } else { %>
        <div class="ticket-card">
          <p class="muted"><%= escapeHtml(activeQueueTicket.getClinicServiceLabel()) %></p>
          <p class="ticket-number"><%= escapeHtml(activeQueueTicket.getDisplayCode()) %></p>
          <p><span class="status-chip <%= activeQueueTicket.getStatusChipClass() %>"><%= activeQueueTicket.getStatus() %></span></p>
          <p>Estimated wait: <%= activeQueueTicket.getEstimatedWaitLabel() %></p>
          <p>Queue date: <%= activeQueueTicket.getQueueLabel() %></p>
        </div>
        <% } %>
        <div class="action-bar" style="margin-top: 12px;">
          <a class="btn btn-primary" href="<%= request.getContextPath() %>/staff/patients/checkin?patientId=<%= patientId %>">Manual Check-in</a>
          <a class="btn btn-secondary" href="<%= request.getContextPath() %>/staff/patients/search?patientId=<%= patientId %>">Back to Search</a>
        </div>
      </aside>
    </section>

    <section>
      <h2 class="section-title">Recent appointments</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Appointment</th>
              <th>Date / Time</th>
              <th>Clinic / Service</th>
              <th>Status</th>
              <th>Remark</th>
            </tr>
          </thead>
          <tbody>
            <% if (recentAppointments.isEmpty()) { %>
            <tr>
              <td colspan="5">No appointment history is available for this patient at the clinic.</td>
            </tr>
            <% } else {
                for (Appointment appointment : recentAppointments) {
            %>
            <tr>
              <td><%= appointment.getDisplayCode() %></td>
              <td><%= appointment.getScheduleLabel() %></td>
              <td><%= appointment.getClinicServiceLabel() %></td>
              <td><span class="status-chip <%= appointment.getStatusChipClass() %>"><%= appointment.getStatus() %></span></td>
              <td><%= safeText(appointment.getOutcomeNotes() == null ? appointment.getReviewReason() : appointment.getOutcomeNotes()) %></td>
            </tr>
            <%      }
               } %>
          </tbody>
        </table>
      </div>
    </section>
  </main>

  <footer class="site-footer">
    <div class="container">CCHC Community Clinic System</div>
  </footer>
</body>
</html>
