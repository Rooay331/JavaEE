<%@page import="ict.bean.PatientSearchResult"%>
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

  if (request.getAttribute("searchResults") == null && request.getAttribute("searchError") == null) {
      response.sendRedirect(request.getContextPath() + "/staff/patients/search");
      return;
  }

  List<PatientSearchResult> searchResults = (List<PatientSearchResult>) request.getAttribute("searchResults");
  if (searchResults == null) {
      searchResults = Collections.emptyList();
  }

  String searchError = (String) request.getAttribute("searchError");
  String selectedKeyword = (String) request.getAttribute("selectedKeyword");
  String selectedPatientId = (String) request.getAttribute("selectedPatientId");
  String selectedStatus = (String) request.getAttribute("selectedStatus");
  Integer assignedClinicId = (Integer) request.getAttribute("assignedClinicId");
  if (assignedClinicId == null) {
      assignedClinicId = loggedInUser.getClinicId();
  }
  Object assignedClinicName = request.getAttribute("assignedClinicName");

  int activeAppointmentCount = 0;
  int queueCount = 0;
  int noBookingCount = 0;
  for (PatientSearchResult result : searchResults) {
      if (result == null) {
          continue;
      }
      if (result.getActiveAppointmentStatus() != null) {
          activeAppointmentCount++;
      }
      if (result.getQueueStatus() != null) {
          queueCount++;
      }
      if (result.getActiveAppointmentStatus() == null && result.getQueueStatus() == null) {
          noBookingCount++;
      }
  }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Staff - Find Patient</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="../common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Find Patient</h1>
      <p class="section-subtitle">Search patient records for appointment validation and manual check-in support.</p>
    </section>

    <section class="notice" style="margin-bottom: 18px;">
      Search scope: <strong><%= assignedClinicName == null ? ("Clinic ID " + assignedClinicId) : assignedClinicName %></strong>.
    </section>

    <% if (searchError != null && !searchError.trim().isEmpty()) { %>
    <section class="notice" style="margin-bottom: 16px;">
      <%= searchError %>
    </section>
    <% } %>

    <section class="summary-strip">
      <div class="summary-box">
        <h4><%= searchResults.size() %></h4>
        <p>Total results</p>
      </div>
      <div class="summary-box">
        <h4><%= activeAppointmentCount %></h4>
        <p>Active appointments</p>
      </div>
      <div class="summary-box">
        <h4><%= queueCount %></h4>
        <p>Queue tickets</p>
      </div>
      <div class="summary-box">
        <h4><%= noBookingCount %></h4>
        <p>No active booking</p>
      </div>
      <div class="summary-box">
        <h4><%= assignedClinicId == null ? "-" : assignedClinicId %></h4>
        <p>Clinic ID</p>
      </div>
    </section>

    <section class="card">
      <h2 class="section-title">Search criteria</h2>
      <form class="filter-form" action="<%= request.getContextPath() %>/staff/patients/search" method="get">
        <div class="field">
          <label for="keyword">Name / Email / Phone</label>
          <input id="keyword" name="keyword" type="text" placeholder="Chan Tai Man or 9123 4567" value="<%= selectedKeyword == null ? "" : selectedKeyword %>">
        </div>
        <div class="field">
          <label for="patientId">Patient ID</label>
          <input id="patientId" name="patientId" type="text" placeholder="P-10029" value="<%= selectedPatientId == null ? "" : selectedPatientId %>">
        </div>
        <div class="field">
          <label for="status">Current status</label>
          <select id="status" name="status">
            <option value="" <%= selectedStatus == null ? "selected" : "" %>>Any</option>
            <option value="Has Active Appointment" <%= isSelected(selectedStatus, "Has Active Appointment") ? "selected" : "" %>>Has Active Appointment</option>
            <option value="In Queue" <%= isSelected(selectedStatus, "In Queue") ? "selected" : "" %>>In Queue</option>
            <option value="No Active Booking" <%= isSelected(selectedStatus, "No Active Booking") ? "selected" : "" %>>No Active Booking</option>
          </select>
        </div>
        <div class="filter-actions">
          <button class="btn btn-primary" type="submit">Search</button>
          <button class="btn btn-secondary" type="reset">Reset</button>
        </div>
      </form>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Patient ID</th>
              <th>Name</th>
              <th>Phone</th>
              <th>Active Appointment</th>
              <th>Queue Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <% if (searchResults.isEmpty()) { %>
            <tr>
              <td colspan="6">No patients match the current search criteria.</td>
            </tr>
            <% } else {
                for (PatientSearchResult result : searchResults) {
            %>
            <tr>
              <td><%= result.getPatientCode() %></td>
              <td><%= result.getFullName() %></td>
              <td><%= result.getPhone() == null ? "-" : result.getPhone() %></td>
              <td>
                <% if (result.getActiveAppointmentStatus() == null) { %>
                  -
                <% } else { %>
                  <span class="status-chip <%= result.getAppointmentStatusChipClass() %>"><%= result.getActiveAppointmentStatus() %></span><br>
                  <span class="muted"><%= result.getActiveAppointmentLabel() %></span>
                <% } %>
              </td>
              <td>
                <% if (result.getQueueStatus() == null) { %>
                  -
                <% } else { %>
                  <span class="status-chip <%= result.getQueueStatusChipClass() %>"><%= result.getQueueStatus() %></span><br>
                  <span class="muted"><%= result.getQueueContextLabel() %> | <%= result.getQueueDateLabel() %></span>
                <% } %>
              </td>
              <td>
                <a href="<%= request.getContextPath() %>/staff/patients/view?patientId=<%= result.getUserId() %>">View</a>
                |
                <a href="<%= request.getContextPath() %>/staff/patients/checkin?patientId=<%= result.getUserId() %>">Check-in</a>
              </td>
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
