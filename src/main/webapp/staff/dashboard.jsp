<%@page import="ict.bean.User"%>
<%@page import="ict.bean.StaffDashboardStats"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
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

  if (request.getAttribute("dashboardStats") == null) {
    response.sendRedirect(request.getContextPath() + "/staff/dashboard");
    return;
  }

  Integer assignedClinicId = loggedInUser.getClinicId();
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Staff Dashboard</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="common/nav.jspf" %>

  <main class="container">

    <section class="notice" style="margin-bottom: 18px;">
      Viewing appointments for <strong><%= request.getAttribute("assignedClinicName") == null ? ("Clinic ID " + assignedClinicId) : request.getAttribute("assignedClinicName") %></strong>.
    </section>

    <%
      StaffDashboardStats dashboardStats = (StaffDashboardStats) request.getAttribute("dashboardStats");
      if (dashboardStats == null) {
          dashboardStats = new StaffDashboardStats();
      }
    %>

    <section class="kpi-row">
      <article class="kpi-card">
        <h3><%= dashboardStats.getTodayAppointments() %></h3>
        <p>Today's appointments</p>
      </article>
      <article class="kpi-card">
        <h3><%= dashboardStats.getWaitingQueueTickets() %></h3>
        <p>Queue tickets waiting</p>
      </article>
      <article class="kpi-card">
        <h3><%= dashboardStats.getPendingApprovals() %></h3>
        <p>Pending approvals</p>
      </article>
      <article class="kpi-card">
        <h3><%= dashboardStats.getOpenServiceIssues() %></h3>
        <p>Open service issues</p>
      </article>
    </section>

    <section class="layout-split">
      <div class="card">
        <h2 class="section-title">Daily Operations</h2>
        <div class="action-bar">
          <a class="btn btn-secondary" href="<%= request.getContextPath() %>/staff/appointments/manage">Manage Appointments</a>
          <a class="btn btn-warning" href="<%= request.getContextPath() %>/staff/queue/manage">Manage Queue</a>
        </div>

        <div class="list-group">
          <div class="list-item">
            <h4>09:00 - Morning session started</h4>
            <p>Clinic operations are synchronised with the live database for the assigned clinic.</p>
          </div>
          <div class="list-item">
            <h4>10:15 - Approval request queue</h4>
            <p>Pending appointment approvals are counted from the appointments table.</p>
          </div>
          <div class="list-item">
            <h4>11:20 - Queue peak alert</h4>
            <p>Queue tickets and service issues are pulled from the live staff workload data.</p>
          </div>
        </div>
      </div>

      <aside class="card">
        <h2 class="section-title">Quick Access</h2>
        <div class="list-group">
          <div class="list-item">
            <h4>Report Operational Issue</h4>
            <p>Log doctor unavailability, equipment problems, and disruptions.</p>
            <p><a href="<%= request.getContextPath() %>/staff/services/issues">Open page</a></p>
          </div>
          <div class="list-item">
            <h4>Patient Lookup</h4>
            <p>Search and verify patient details before manual check-in.</p>
            <p><a href="<%= request.getContextPath() %>/staff/patients/search">Open page</a></p>
          </div>
        </div>
      </aside>
    </section>
  </main>

  <footer class="site-footer">
    <div class="container">CCHC Community Clinic System</div>
  </footer>
</body>
</html>