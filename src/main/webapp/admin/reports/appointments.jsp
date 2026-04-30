<%@page import="ict.bean.Appointment"%>
<%@page import="ict.bean.ClinicService"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page import="java.time.Month"%>
<%@page import="java.time.format.TextStyle"%>
<%@page import="java.util.Locale"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/admin/common/util.jspf" %>
<%
    Object sessionUser = session.getAttribute("userInfo");
    if (!(sessionUser instanceof ict.bean.User) || !"ADMIN".equalsIgnoreCase(((ict.bean.User) sessionUser).getRole())) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    List<ClinicService> activeClinics = (List<ClinicService>) request.getAttribute("activeClinics");
    if (activeClinics == null) activeClinics = Collections.emptyList();
    pageContext.setAttribute("activeClinics", activeClinics);

    List<String> availableServices = (List<String>) request.getAttribute("availableServices");
    if (availableServices == null) availableServices = Collections.emptyList();
    pageContext.setAttribute("availableServices", availableServices);

    List<Appointment> appointmentRows = (List<Appointment>) request.getAttribute("appointmentRows");
    if (appointmentRows == null) appointmentRows = Collections.emptyList();
    pageContext.setAttribute("appointmentRows", appointmentRows);

    Integer selectedClinicId = (Integer) request.getAttribute("selectedClinicId");
    pageContext.setAttribute("selectedClinicId", selectedClinicId);

    String selectedServiceName = (String) request.getAttribute("selectedServiceName");
    pageContext.setAttribute("selectedServiceName", selectedServiceName);

    String selectedStatus = (String) request.getAttribute("selectedStatus");
    pageContext.setAttribute("selectedStatus", selectedStatus);

    String selectedPatientName = (String) request.getAttribute("selectedPatientName");
    pageContext.setAttribute("selectedPatientName", selectedPatientName);

    Integer selectedMonth = (Integer) request.getAttribute("selectedMonth");
    if (selectedMonth == null) selectedMonth = java.time.LocalDate.now().getMonthValue();
    pageContext.setAttribute("selectedMonth", selectedMonth);

    Integer selectedYear = (Integer) request.getAttribute("selectedYear");
    if (selectedYear == null) selectedYear = java.time.LocalDate.now().getYear();
    pageContext.setAttribute("selectedYear", selectedYear);

    String reportMonthLabel = (String) request.getAttribute("reportMonthLabel");
    if (reportMonthLabel == null || reportMonthLabel.trim().isEmpty()) {
      reportMonthLabel = Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + selectedYear;
    }
    pageContext.setAttribute("reportMonthLabel", reportMonthLabel);

    String reportError = (String) request.getAttribute("reportError");
    pageContext.setAttribute("reportError", reportError);
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Admin - Appointment Report</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="/admin/common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Appointment Report</h1>
      <p class="section-subtitle">Review appointments by clinic, service, patient, and status for <strong><c:out value="${reportMonthLabel}" /></strong>.</p>
    </section>

    <c:if test="${not empty reportError}">
      <section class="notice" style="margin-bottom: 16px;"><c:out value="${reportError}" /></section>
    </c:if>

    <section class="card">
      <form class="filter-form" action="<%= request.getContextPath() %>/admin/reports/appointments" method="get">
        <div class="field">
          <label for="clinicId">Clinic</label>
          <select id="clinicId" name="clinicId">
            <c:choose>
              <c:when test="${empty activeClinics}">
                <option value="">No active clinics</option>
              </c:when>
              <c:otherwise>
                <c:forEach items="${activeClinics}" var="clinic">
                  <option value="${clinic.clinicId}" ${selectedClinicId == clinic.clinicId ? 'selected' : ''}><c:out value="${clinic.clinicName}" /></option>
                </c:forEach>
              </c:otherwise>
            </c:choose>
          </select>
        </div>
        <div class="field">
          <label for="serviceName">Service</label>
          <select id="serviceName" name="serviceName">
            <option value="" ${empty selectedServiceName ? 'selected' : ''}>All services</option>
            <c:forEach items="${availableServices}" var="service">
              <option value="${service}" ${service == selectedServiceName ? 'selected' : ''}><c:out value="${service}" /></option>
            </c:forEach>
          </select>
        </div>
        <div class="field">
          <label for="status">Status</label>
          <select id="status" name="status">
            <option value="" ${empty selectedStatus ? 'selected' : ''}>All statuses</option>
            <option value="COMPLETED" ${selectedStatus == 'COMPLETED' ? 'selected' : ''}>Completed</option>
            <option value="NO_SHOW" ${selectedStatus == 'NO_SHOW' ? 'selected' : ''}>No-show</option>
            <option value="CANCELLED" ${selectedStatus == 'CANCELLED' ? 'selected' : ''}>Cancelled</option>
          </select>
        </div>
        <div class="field">
          <label for="month">Month</label>
          <select id="month" name="month">
            <% for (int m = 1; m <= 12; m++) { %>
              <option value="<%= m %>" <%= (Integer)pageContext.getAttribute("selectedMonth") == m ? "selected" : "" %>><%= Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH) %></option>
            <% } %>
          </select>
        </div>
        <div class="field">
          <label for="year">Year</label>
          <select id="year" name="year">
            <% int curY = (Integer)pageContext.getAttribute("selectedYear"); for (int y = curY - 2; y <= curY + 2; y++) { %>
              <option value="<%= y %>" <%= curY == y ? "selected" : "" %>><%= y %></option>
            <% } %>
          </select>
        </div>
        <div class="field">
          <label for="patientName">Patient</label>
          <input id="patientName" name="patientName" type="search" value="<c:out value="${selectedPatientName}" />" placeholder="Patient name">
        </div>
        <div class="filter-actions">
          <button class="btn btn-primary" type="submit">Search</button>
          <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/reports/appointments">Reset</a>
        </div>
      </form>
    </section>

    <section class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Appointment</th>
            <th>Patient</th>
            <th>Clinic / Service</th>
            <th>Date & Time</th>
            <th>Status</th>
            <th>Reviewed By</th>
          </tr>
        </thead>
        <tbody>
          <c:choose>
            <c:when test="${empty appointmentRows}">
              <tr><td colspan="6">No appointment rows matched the current filters.</td></tr>
            </c:when>
            <c:otherwise>
              <c:forEach items="${appointmentRows}" var="appointment">
                <tr>
                  <td><c:out value="${appointment.displayCode}" /></td>
                  <td><c:out value="${appointment.patientName}" /></td>
                  <td><c:out value="${appointment.clinicServiceLabel}" /></td>
                  <td><c:out value="${appointment.scheduleLabel}" /></td>
                  <td><span class="status-chip ${appointment.statusChipClass}"><c:out value="${appointment.status}" /></span></td>
                  <td><c:out value="${appointment.reviewedBy}" /></td>
                </tr>
              </c:forEach>
            </c:otherwise>
          </c:choose>
        </tbody>
      </table>
    </section>
  </main>

  <footer class="site-footer"><div class="container">Appointment report sample page.</div></footer>
</body>
</html>