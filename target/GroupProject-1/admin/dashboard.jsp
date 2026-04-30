<%@page import="ict.bean.ClinicService"%>
<%@page import="ict.bean.User"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/admin/common/util.jspf" %>
<%
    User adminUser = null;
    Object sessionUser = session.getAttribute("userInfo");
    if (sessionUser instanceof User) {
        adminUser = (User) sessionUser;
    }

    if (adminUser == null || !"ADMIN".equalsIgnoreCase(adminUser.getRole())) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    if (request.getAttribute("totalUsers") == null && request.getAttribute("dashboardError") == null) {
        response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        return;
    }

    List<ClinicService> activeClinics = (List<ClinicService>) request.getAttribute("activeClinics");
    if (activeClinics == null) {
        activeClinics = Collections.emptyList();
    }

    Integer totalUsers = (Integer) request.getAttribute("totalUsers");
    if (totalUsers == null) {
        totalUsers = 0;
    }

    Integer activeUsers = (Integer) request.getAttribute("activeUsers");
    if (activeUsers == null) {
        activeUsers = 0;
    }

    Integer activeStaffUsers = (Integer) request.getAttribute("activeStaffUsers");
    if (activeStaffUsers == null) {
        activeStaffUsers = 0;
    }

    Integer activeAdminUsers = (Integer) request.getAttribute("activeAdminUsers");
    if (activeAdminUsers == null) {
        activeAdminUsers = 0;
    }

    Integer activePatientUsers = (Integer) request.getAttribute("activePatientUsers");
    if (activePatientUsers == null) {
        activePatientUsers = 0;
    }

    Integer maxBookingsPerPatient = (Integer) request.getAttribute("maxBookingsPerPatient");
    if (maxBookingsPerPatient == null) {
        maxBookingsPerPatient = 3;
    }

    Integer maxBookingsPerSlot = (Integer) request.getAttribute("maxBookingsPerSlot");
    if (maxBookingsPerSlot == null) {
      maxBookingsPerSlot = 1;
    }

    Integer cancellationCutoffHours = (Integer) request.getAttribute("cancellationCutoffHours");
    if (cancellationCutoffHours == null) {
        cancellationCutoffHours = 2;
    }

    Boolean queueEnabled = (Boolean) request.getAttribute("queueEnabled");
    if (queueEnabled == null) {
        queueEnabled = Boolean.TRUE;
    }

    String dashboardError = (String) request.getAttribute("dashboardError");
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Admin Dashboard</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="/admin/common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Admin Dashboard</h1>
      <p class="section-subtitle">User administration, policy controls, and operational analytics in one place.</p>
    </section>

    <% if (dashboardError != null && !dashboardError.trim().isEmpty()) { %>
    <section class="notice" style="margin-bottom: 16px;">
      <%= escapeHtml(dashboardError) %>
    </section>
    <% } %>

    <section class="summary-strip">
      <article class="summary-box"><h4><%= totalUsers %></h4><p>Total users</p></article>
      <article class="summary-box"><h4><%= activeUsers %></h4><p>Active accounts</p></article>
      <article class="summary-box"><h4><%= activeStaffUsers %></h4><p>Active staff</p></article>
      <article class="summary-box"><h4><%= activeAdminUsers %></h4><p>Admin accounts</p></article>
      <article class="summary-box"><h4><%= activePatientUsers %></h4><p>Patient accounts</p></article>
    </section>

    <section class="layout-split">
      <div class="card">
        <h2 class="section-title">Quick actions</h2>
        <div class="actions">
          <a class="btn btn-primary" href="<%= request.getContextPath() %>/admin/users/create">Create User</a>
          <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/users/list">Manage Users</a>
          <!-- <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/policies/system">Edit Policies</a> -->
          <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/batch/import.html">Batch Import</a>
        </div>

        <div class="list-group" style="margin-top: 18px;">
          <div class="list-item"><h4>Booking policy</h4><p>Max active bookings per user: <%= maxBookingsPerPatient %></p></div>
          <div class="list-item"><h4>Slot policy</h4><p>Max bookings per slot: <%= maxBookingsPerSlot %></p></div>
          <div class="list-item"><h4>Cancellation cutoff</h4><p><%= cancellationCutoffHours %> hours before appointment time</p></div>
          <div class="list-item"><h4>Queue module</h4><p><%= queueEnabled.booleanValue() ? "Enabled" : "Disabled" %></p></div>
        </div>
      </div>

      <aside class="card">
        <h2 class="section-title">Clinic coverage</h2>
        <div class="list-group">
          <% if (activeClinics.isEmpty()) { %>
          <div class="list-item"><p>No active clinics found.</p></div>
          <% } else {
               for (ClinicService clinic : activeClinics) {
          %>
          <div class="list-item">
            <h4><%= escapeHtml(clinic.getClinicName()) %></h4>
            <p><%= escapeHtml(clinic.getServicesLabel()) %></p>
          </div>
          <%     }
             } %>
        </div>
      </aside>
    </section>
  </main>

  <footer class="site-footer">
    <div class="container">CCHC administrator workspace.</div>
  </footer>
</body>
</html>