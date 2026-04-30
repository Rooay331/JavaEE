<%@page import="ict.bean.ClinicService"%>
<%@page import="ict.bean.ClinicServiceStatus"%>
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

    List<ClinicService> activeClinics = (List<ClinicService>) request.getAttribute("activeClinics");
    if (activeClinics == null) {
        activeClinics = Collections.emptyList();
    }

    List<ClinicServiceStatus> availableServices = (List<ClinicServiceStatus>) request.getAttribute("availableServices");
    if (availableServices == null) {
        availableServices = Collections.emptyList();
    }

    Integer selectedClinicId = (Integer) request.getAttribute("selectedClinicId");
    Integer selectedServiceId = (Integer) request.getAttribute("selectedServiceId");
    Integer defaultCapacity = (Integer) request.getAttribute("defaultCapacity");
    if (defaultCapacity == null) {
        defaultCapacity = 25;
    }

    Boolean walkInEnabled = (Boolean) request.getAttribute("walkInEnabled");
    if (walkInEnabled == null) {
        walkInEnabled = Boolean.TRUE;
    }

    Boolean approvalRequired = (Boolean) request.getAttribute("approvalRequired");
    if (approvalRequired == null) {
        approvalRequired = Boolean.FALSE;
    }

    String adminNote = (String) request.getAttribute("adminNote");
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Admin - Clinic Policies</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="/admin/common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Clinic-specific Policies</h1>
      <p class="section-subtitle">Override default capacity, walk-in, and approval rules per clinic and service.</p>
    </section>

    <nav class="sub-nav" aria-label="Policy navigation">
      <a href="<%= request.getContextPath() %>/admin/policies/system">System Policies</a>
      <a class="active" href="<%= request.getContextPath() %>/admin/policies/clinic">Clinic-specific Policies</a>
    </nav>

    <section class="card">
      <form class="form-grid" action="<%= request.getContextPath() %>/admin/policies/clinic" method="post">
        <div class="field">
          <label for="clinicId">Clinic</label>
          <select id="clinicId" name="clinicId">
            <% for (ClinicService clinic : activeClinics) { %>
            <option value="<%= clinic.getClinicId() %>" <%= selectedClinicId != null && selectedClinicId.equals(clinic.getClinicId()) ? "selected" : "" %>><%= escapeHtml(clinic.getClinicName()) %></option>
            <% } %>
          </select>
        </div>
        <div class="field">
          <label for="serviceId">Service</label>
          <select id="serviceId" name="serviceId">
            <% if (availableServices.isEmpty()) { %>
            <option value="">No services available</option>
            <% } else {
                 for (ClinicServiceStatus service : availableServices) { %>
            <option value="<%= service.getServiceId() %>" <%= selectedServiceId != null && selectedServiceId.equals(service.getServiceId()) ? "selected" : "" %>><%= escapeHtml(service.getServiceName()) %></option>
            <%   }
               } %>
          </select>
        </div>
        <div class="field">
          <label for="defaultCapacity">Default capacity override</label>
          <input id="defaultCapacity" name="defaultCapacity" type="number" min="1" value="<%= defaultCapacity %>">
        </div>
        <div class="field">
          <label for="walkInEnabled">Walk-in override</label>
          <div class="inline-row" style="padding: 10px 12px; border: 1px solid #b8cce0; border-radius: 10px; background: #fcfeff;">
            <input id="walkInEnabled" name="walkInEnabled" type="checkbox" value="1" <%= walkInEnabled.booleanValue() ? "checked" : "" %>>
            <span>Enable walk-in for this clinic/service pair</span>
          </div>
        </div>
        <div class="field">
          <label for="approvalRequired">Approval override</label>
          <div class="inline-row" style="padding: 10px 12px; border: 1px solid #b8cce0; border-radius: 10px; background: #fcfeff;">
            <input id="approvalRequired" name="approvalRequired" type="checkbox" value="1" <%= approvalRequired.booleanValue() ? "checked" : "" %>>
            <span>Require manual approval</span>
          </div>
        </div>
        <div class="field field-full">
          <label for="adminNote">Admin note</label>
          <input id="adminNote" name="adminNote" type="text" value="<%= formValue(adminNote) %>" placeholder="Temporary flu season arrangement">
        </div>
        <div class="field field-full form-actions">
          <button class="btn btn-primary" type="submit">Save Clinic Policy</button>
        </div>
      </form>
    </section>
  </main>

  <footer class="site-footer"><div class="container">Clinic policy configuration sample page.</div></footer>
</body>
</html>