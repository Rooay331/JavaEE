<%@page import="ict.bean.Appointment"%>
<%@page import="ict.bean.QueueTicket"%>
<%@page import="ict.bean.User"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!
  private boolean isSelected(String current, String option) {
    return current != null && current.equals(option);
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

  User patientUser = (User) request.getAttribute("patientUser");
  QueueTicket activeQueueTicket = (QueueTicket) request.getAttribute("activeQueueTicket");
  List<Appointment> recentAppointments = (List<Appointment>) request.getAttribute("recentAppointments");
  if (recentAppointments == null) {
      recentAppointments = Collections.emptyList();
  }

  String patientError = (String) request.getAttribute("patientError");
  String patientIdValue = (String) request.getAttribute("patientId");
  String appointmentIdValue = (String) request.getAttribute("appointmentId");
  String resultValue = (String) request.getAttribute("result");
  String noteValue = (String) request.getAttribute("note");
  Integer assignedClinicId = (Integer) request.getAttribute("assignedClinicId");
  if (assignedClinicId == null) {
      assignedClinicId = loggedInUser.getClinicId();
  }
  Object assignedClinicName = request.getAttribute("assignedClinicName");
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Staff - Manual Check-in</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="../common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Manual Check-in</h1>
      <p class="section-subtitle">Update appointment attendance status and keep the patient record in sync with the clinic database.</p>
    </section>

    <section class="notice" style="margin-bottom: 18px;">
      Check-in is scoped to <strong><%= assignedClinicName == null ? ("Clinic ID " + assignedClinicId) : assignedClinicName %></strong>.
    </section>

    <% if (patientError != null && !patientError.trim().isEmpty()) { %>
    <section class="notice" style="margin-bottom: 16px;">
      <%= patientError %>
    </section>
    <% } %>

    <section class="layout-split">
      <div class="card">
        <h2 class="section-title">Check-in form</h2>
        <form class="form-grid" action="<%= request.getContextPath() %>/staff/patients/checkin" method="post">
          <div class="field">
            <label for="patientId">Patient ID</label>
            <input id="patientId" name="patientId" type="text" placeholder="P-10029" value="<%= patientIdValue == null ? "" : patientIdValue %>">
          </div>
          <div class="field">
            <label for="appointmentId">Appointment ID</label>
            <input id="appointmentId" name="appointmentId" type="text" placeholder="A-1025" value="<%= appointmentIdValue == null ? "" : appointmentIdValue %>" required>
          </div>
          <div class="field">
            <label for="result">Status update</label>
            <select id="result" name="result" required>
              <option value="ARRIVED" <%= isSelected(resultValue, "ARRIVED") || resultValue == null ? "selected" : "" %>>ARRIVED</option>
              <option value="NO_SHOW" <%= isSelected(resultValue, "NO_SHOW") ? "selected" : "" %>>NO_SHOW</option>
              <option value="COMPLETED" <%= isSelected(resultValue, "COMPLETED") ? "selected" : "" %>>COMPLETED</option>
            </select>
          </div>
          <div class="field field-full">
            <label for="note">Remark</label>
            <textarea id="note" name="note" rows="3" placeholder="Optional status remark"><%= noteValue == null ? "" : noteValue %></textarea>
          </div>
          <div class="field field-full form-actions">
            <button class="btn btn-primary" type="submit">Submit Check-in</button>
            <a class="btn btn-secondary" href="<%= request.getContextPath() %>/staff/patients/search">Back to Search</a>
          </div>
        </form>
      </div>

      <aside class="card">
        <h2 class="section-title">Selected patient</h2>
        <% if (patientUser == null) { %>
        <div class="list-group">
          <div class="list-item">
            <h4>No patient selected</h4>
            <p>Search for a patient first, or enter the patient and appointment identifiers manually.</p>
          </div>
        </div>
        <% } else { %>
        <div class="list-group">
          <div class="list-item"><h4>Patient ID</h4><p>P-<%= patientUser.getUserId() %></p></div>
          <div class="list-item"><h4>Name</h4><p><%= patientUser.getFullName() %></p></div>
          <div class="list-item"><h4>Phone</h4><p><%= patientUser.getPhone() == null ? "-" : patientUser.getPhone() %></p></div>
          <div class="list-item"><h4>Current queue</h4><p><%= activeQueueTicket == null ? "No active queue" : activeQueueTicket.getDisplayCode() + " / " + activeQueueTicket.getStatus() %></p></div>
        </div>
        <% } %>
      </aside>
    </section>

    <section style="margin-top: 18px;">
      <h2 class="section-title">Today's appointment log</h2>
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
              <td colspan="5">No appointment history is available for this patient.</td>
            </tr>
            <% } else {
                for (Appointment appointment : recentAppointments) {
            %>
            <tr>
              <td><%= appointment.getDisplayCode() %></td>
              <td><%= appointment.getScheduleLabel() %></td>
              <td><%= appointment.getClinicServiceLabel() %></td>
              <td><span class="status-chip <%= appointment.getStatusChipClass() %>"><%= appointment.getStatus() %></span></td>
              <td><%= appointment.getOutcomeNotes() == null ? appointment.getReviewReason() : appointment.getOutcomeNotes() %></td>
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
