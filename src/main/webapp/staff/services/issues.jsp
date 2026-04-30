<%@page import="ict.bean.ClinicServiceStatus"%>
<%@page import="ict.bean.IncidentLog"%>
<%@page import="ict.bean.User"%>
<%@page import="java.time.LocalDateTime"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!
  private static final DateTimeFormatter DATETIME_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  private boolean isSelected(String current, String option) {
    return current != null && current.equals(option);
  }

  private String nowDateTimeInput() {
    return LocalDateTime.now().withSecond(0).withNano(0).format(DATETIME_INPUT);
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

  if (request.getAttribute("incidents") == null && request.getAttribute("incidentError") == null) {
      response.sendRedirect(request.getContextPath() + "/staff/services/issues");
      return;
  }

  List<IncidentLog> incidents = (List<IncidentLog>) request.getAttribute("incidents");
  if (incidents == null) {
      incidents = Collections.emptyList();
  }

  List<ClinicServiceStatus> serviceStatuses = (List<ClinicServiceStatus>) request.getAttribute("serviceStatuses");
  if (serviceStatuses == null) {
      serviceStatuses = Collections.emptyList();
  }

  String incidentError = (String) request.getAttribute("incidentError");
    String selectedSeverity = request.getParameter("severityFilter");
    String selectedStatus = request.getParameter("statusFilter");
  Integer assignedClinicId = (Integer) request.getAttribute("assignedClinicId");
  if (assignedClinicId == null) {
      assignedClinicId = loggedInUser.getClinicId();
  }
  Object assignedClinicName = request.getAttribute("assignedClinicName");

  int totalIncidents = 0;
  int openIncidents = 0;
  int inProgressIncidents = 0;
  int resolvedIncidents = 0;
  for (IncidentLog incident : incidents) {
      if (incident == null) {
          continue;
      }
      totalIncidents++;
      if ("OPEN".equalsIgnoreCase(incident.getStatus())) {
          openIncidents++;
      } else if ("IN_PROGRESS".equalsIgnoreCase(incident.getStatus())) {
          inProgressIncidents++;
      } else if ("RESOLVED".equalsIgnoreCase(incident.getStatus())) {
          resolvedIncidents++;
      }
  }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Staff - Operational Issues</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="../common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Report Operational Issue</h1>
      <p class="section-subtitle">Log service incidents such as doctor unavailability, queue interruptions, or temporary suspensions.</p>
    </section>

    <section class="notice" style="margin-bottom: 18px;">
      Incident logging is scoped to <strong><%= assignedClinicName == null ? ("Clinic ID " + assignedClinicId) : assignedClinicName %></strong>.
    </section>

    <% if (incidentError != null && !incidentError.trim().isEmpty()) { %>
    <section class="notice" style="margin-bottom: 16px;">
      <%= incidentError %>
    </section>
    <% } %>

    <section class="summary-strip">
      <div class="summary-box">
        <h4><%= totalIncidents %></h4>
        <p>Total incidents</p>
      </div>
      <div class="summary-box">
        <h4><%= openIncidents %></h4>
        <p>Open</p>
      </div>
      <div class="summary-box">
        <h4><%= inProgressIncidents %></h4>
        <p>In progress</p>
      </div>
      <div class="summary-box">
        <h4><%= resolvedIncidents %></h4>
        <p>Resolved</p>
      </div>
      <div class="summary-box">
        <h4><%= assignedClinicId == null ? "-" : assignedClinicId %></h4>
        <p>Clinic ID</p>
      </div>
    </section>

    <section class="layout-split">
      <div class="card">
        <h2 class="section-title">Create incident report</h2>
        <form class="form-grid" action="<%= request.getContextPath() %>/staff/services/issues" method="post">
          <input type="hidden" name="severityFilter" value="<%= selectedSeverity == null ? "" : selectedSeverity %>">
          <input type="hidden" name="statusFilter" value="<%= selectedStatus == null ? "" : selectedStatus %>">
          <div class="field">
            <label for="serviceId">Affected service</label>
            <select id="serviceId" name="serviceId">
              <option value="">General clinic issue</option>
              <% for (ClinicServiceStatus service : serviceStatuses) { %>
              <option value="<%= service.getServiceId() %>"><%= service.getClinicServiceLabel() %></option>
              <% } %>
            </select>
          </div>
          <div class="field">
            <label for="severity">Severity</label>
            <select id="severity" name="severity" required>
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </div>
          <div class="field field-full">
            <label for="title">Issue title</label>
            <input id="title" name="title" type="text" placeholder="Vaccination fridge alarm triggered" required>
          </div>
          <div class="field">
            <label for="occurredAt">Occurred at</label>
            <input id="occurredAt" name="occurredAt" type="datetime-local" value="<%= nowDateTimeInput() %>" required>
          </div>
          <div class="field field-full">
            <label for="description">Description</label>
            <textarea id="description" name="description" rows="4" placeholder="Describe what happened and any temporary action taken." required></textarea>
          </div>
          <div class="field field-full form-actions">
            <button class="btn btn-primary" type="submit">Submit Incident</button>
          </div>
        </form>
      </div>

      <aside class="card">
        <h2 class="section-title">Current context</h2>
        <div class="list-group">
          <div class="list-item">
            <h4>Clinic scope</h4>
            <p><%= assignedClinicName == null ? ("Clinic ID " + assignedClinicId) : assignedClinicName %></p>
          </div>
          <div class="list-item">
            <h4>Reported incidents</h4>
            <p><%= totalIncidents %> loaded from the database</p>
          </div>
          <div class="list-item">
            <h4>Operational note</h4>
            <p>New incidents are inserted into the incident log table and can be reviewed immediately below.</p>
          </div>
        </div>
      </aside>
    </section>

    <section class="card">
      <h2 class="section-title">Recent operational issues</h2>
      <form class="filter-form" action="<%= request.getContextPath() %>/staff/services/issues" method="get">
        <div class="field">
          <label for="severityFilter">Severity</label>
          <select id="severityFilter" name="severityFilter">
            <option value="" <%= selectedSeverity == null ? "selected" : "" %>>All severities</option>
            <option value="LOW" <%= isSelected(selectedSeverity, "LOW") ? "selected" : "" %>>LOW</option>
            <option value="MEDIUM" <%= isSelected(selectedSeverity, "MEDIUM") ? "selected" : "" %>>MEDIUM</option>
            <option value="HIGH" <%= isSelected(selectedSeverity, "HIGH") ? "selected" : "" %>>HIGH</option>
            <option value="CRITICAL" <%= isSelected(selectedSeverity, "CRITICAL") ? "selected" : "" %>>CRITICAL</option>
          </select>
        </div>
        <div class="field">
          <label for="statusFilter">Status</label>
          <select id="statusFilter" name="statusFilter">
            <option value="" <%= selectedStatus == null ? "selected" : "" %>>All statuses</option>
            <option value="OPEN" <%= isSelected(selectedStatus, "OPEN") ? "selected" : "" %>>OPEN</option>
            <option value="IN_PROGRESS" <%= isSelected(selectedStatus, "IN_PROGRESS") ? "selected" : "" %>>IN_PROGRESS</option>
            <option value="RESOLVED" <%= isSelected(selectedStatus, "RESOLVED") ? "selected" : "" %>>RESOLVED</option>
            <option value="CLOSED" <%= isSelected(selectedStatus, "CLOSED") ? "selected" : "" %>>CLOSED</option>
          </select>
        </div>
        <div class="filter-actions">
          <button class="btn btn-primary" type="submit">Apply Filters</button>
          <button class="btn btn-secondary" type="reset">Reset</button>
        </div>
      </form>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Incident</th>
              <th>Context</th>
              <th>Severity</th>
              <th>Status</th>
              <th>Occurred At</th>
              <th>Reporter</th>
              <th>Title</th>
            </tr>
          </thead>
          <tbody>
            <% if (incidents.isEmpty()) { %>
            <tr>
              <td colspan="7">No incidents match the current filter.</td>
            </tr>
            <% } else {
                for (IncidentLog incident : incidents) {
            %>
            <tr>
              <td><%= incident.getDisplayCode() %></td>
              <td><%= incident.getContextLabel() %></td>
              <td><span class="status-chip <%= incident.getSeverityChipClass() %>"><%= incident.getSeverity() %></span></td>
              <td><span class="status-chip <%= incident.getStatusChipClass() %>"><%= incident.getStatus() %></span></td>
              <td><%= incident.getOccurredAtLabel() %></td>
              <td><%= incident.getReporterLabel() %></td>
              <td>
                <strong><%= incident.getTitle() %></strong><br>
                <span class="muted"><%= incident.getDescription() %></span>
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
