<%@page import="ict.bean.User"%>
<%@page import="ict.bean.Appointment"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!
  private boolean isSelected(String current, String option) {
    return current != null && current.equals(option);
  }

  private String appointmentStatusClass(String status) {
    if (status == null) {
      return "appointment-status-neutral";
    }

    switch (status.trim().toUpperCase()) {
      case "PENDING":
        return "appointment-status-pending";
      case "CONFIRMED":
        return "appointment-status-confirmed";
      case "REJECTED_BY_CLINIC":
        return "appointment-status-rejected";
      case "ARRIVED":
        return "appointment-status-arrived";
      case "COMPLETED":
        return "appointment-status-completed";
      case "CANCELLED_BY_PATIENT":
        return "appointment-status-cancelled-patient";
      case "CANCELLED_BY_CLINIC":
        return "appointment-status-cancelled-clinic";
      case "NO_SHOW":
        return "appointment-status-no-show";
      default:
        return "appointment-status-neutral";
    }
  }

  private boolean canUpdate(String status) {
    if (status == null) {
      return true;
    }
    return !(
      "REJECTED_BY_CLINIC".equals(status)
      || "COMPLETED".equals(status)
      || "CANCELLED_BY_PATIENT".equals(status)
      || "CANCELLED_BY_CLINIC".equals(status)
      || "NO_SHOW".equals(status)
    );
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

  if (request.getAttribute("appointments") == null && request.getAttribute("appointmentsError") == null) {
      response.sendRedirect(request.getContextPath() + "/staff/appointments/manage");
      return;
  }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Staff - Appointment Management</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
  <style>
    .appointment-status-chip {
      border: 1px solid transparent;
    }

    .appointment-status-chip.appointment-status-pending {
      color: #8b5d00;
      background: #fff7e3;
      border-color: #f6d487;
    }

    .appointment-status-chip.appointment-status-confirmed {
      color: #1e7f5f;
      background: #edf7f2;
      border-color: #bae7d4;
    }

    .appointment-status-chip.appointment-status-rejected {
      color: #9f2b2b;
      background: #fdeeee;
      border-color: #f1b6b6;
    }

    .appointment-status-chip.appointment-status-arrived {
      color: #1f5fbf;
      background: #e6f1ff;
      border-color: #b8cff3;
    }

    .appointment-status-chip.appointment-status-completed {
      color: #0f7a76;
      background: #e7fbfa;
      border-color: #bfe9e6;
    }

    .appointment-status-chip.appointment-status-cancelled-patient {
      color: #6941c6;
      background: #f0e8ff;
      border-color: #d5c6f9;
    }

    .appointment-status-chip.appointment-status-cancelled-clinic {
      color: #b42318;
      background: #fff1f0;
      border-color: #f4b4b1;
    }

    .appointment-status-chip.appointment-status-no-show {
      color: #a05a00;
      background: #fef3e6;
      border-color: #f5c573;
    }

    .appointment-status-chip.appointment-status-neutral {
      color: #425466;
      background: #edf2f7;
      border-color: #d3dbe6;
    }
  </style>
</head>
<body>
  <%@ include file="common/nav.jspf" %>

  <main class="container">

    <%
      List<Appointment> appointments = (List<Appointment>) request.getAttribute("appointments");
      if (appointments == null) {
          appointments = Collections.emptyList();
      }

      String selectedService = (String) request.getAttribute("selectedService");
      if (selectedService == null) {
          selectedService = request.getParameter("service");
      }

      String selectedStatus = (String) request.getAttribute("selectedStatus");
      if (selectedStatus == null) {
          selectedStatus = request.getParameter("status");
      }

        String selectedPatientNameFilter = (String) request.getAttribute("selectedPatientNameFilter");
        if (selectedPatientNameFilter == null) {
          selectedPatientNameFilter = request.getParameter("patientName");
        }

      String selectedDate = (String) request.getAttribute("selectedDate");
      if (selectedDate == null) {
          selectedDate = request.getParameter("date");
      }

      String appointmentsError = (String) request.getAttribute("appointmentsError");
      String updateMessage = (String) request.getAttribute("updateMessage");
      String updateMessageType = (String) request.getAttribute("updateMessageType");
      String updateMessageStyle = "error".equalsIgnoreCase(updateMessageType) ? "border-color:#f1b6b6;background:#fdeeee;color:#9f2b2b;" : "";
      boolean updatePanelOpen = Boolean.TRUE.equals(request.getAttribute("updatePanelOpen"));
        List<String> availableServices = (List<String>) request.getAttribute("availableServices");
        if (availableServices == null) {
          availableServices = Collections.emptyList();
        }
      String selectedAppointmentCode = (String) request.getParameter("selectedAppointmentCode");
      String selectedPatientName = (String) request.getParameter("selectedPatientName");
      String selectedServiceName = (String) request.getParameter("selectedServiceName");
      String selectedScheduleLabel = (String) request.getParameter("selectedScheduleLabel");
      String selectedUpdateAction = (String) request.getParameter("updateAction");
      String selectedRemark = (String) request.getParameter("remark");
      Integer assignedClinicId = (Integer) request.getAttribute("assignedClinicId");
      if (assignedClinicId == null) {
          assignedClinicId = loggedInUser.getClinicId();
      }
      Object assignedClinicName = request.getAttribute("assignedClinicName");
      String clinicLocationLabel = assignedClinicName == null ? ("Clinic ID " + assignedClinicId) : String.valueOf(assignedClinicName);
    %>

    <% if (appointmentsError != null && !appointmentsError.trim().isEmpty()) { %>
    <section class="notice" style="margin-bottom: 16px;">
      <%= appointmentsError %>
    </section>
    <% } %>

    <% if (updateMessage != null && !updateMessage.trim().isEmpty()) { %>
      <% if ("error".equalsIgnoreCase(updateMessageType)) { %>
    <section class="notice" style="margin-bottom: 16px; border-color:#f1b6b6;background:#fdeeee;color:#9f2b2b;">
      <%= updateMessage %>
    </section>
      <% } else { %>
    <section class="notice" style="margin-bottom: 16px;">
      <%= updateMessage %>
    </section>
      <% } %>
    <% } %>

    <section class="notice" style="margin-bottom: 16px;">
      Clinic location: <strong><%= clinicLocationLabel %></strong>
    </section>

    <section class="card">
      <h2 class="section-title">Filter appointments</h2>
      <form class="filter-form" id="appointmentsFilterForm" action="<%= request.getContextPath() %>/staff/appointments/manage" method="get">
        <div class="field">
          <label for="patientName">Patient name</label>
          <input id="patientName" name="patientName" type="search" value="<%= selectedPatientNameFilter == null ? "" : selectedPatientNameFilter %>" placeholder="Search patient name">
        </div>
        <div class="field">
          <label for="service">Service</label>
          <select id="service" name="service">
            <option value="" <%= selectedService == null ? "selected" : "" %>>All services</option>
            <% for (String serviceName : availableServices) { %>
            <option value="<%= serviceName %>" <%= isSelected(selectedService, serviceName) ? "selected" : "" %>><%= serviceName %></option>
            <% } %>
          </select>
        </div>
        <div class="field">
          <label for="status">Status</label>
          <select id="status" name="status">
            <option value="" <%= selectedStatus == null ? "selected" : "" %>>Any status</option>
            <option value="PENDING" <%= isSelected(selectedStatus, "PENDING") ? "selected" : "" %>>PENDING</option>
            <option value="CONFIRMED" <%= isSelected(selectedStatus, "CONFIRMED") ? "selected" : "" %>>CONFIRMED</option>
            <option value="REJECTED_BY_CLINIC" <%= isSelected(selectedStatus, "REJECTED_BY_CLINIC") ? "selected" : "" %>>REJECTED_BY_CLINIC</option>
            <option value="ARRIVED" <%= isSelected(selectedStatus, "ARRIVED") ? "selected" : "" %>>ARRIVED</option>
            <option value="COMPLETED" <%= isSelected(selectedStatus, "COMPLETED") ? "selected" : "" %>>COMPLETED</option>
            <option value="CANCELLED_BY_PATIENT" <%= isSelected(selectedStatus, "CANCELLED_BY_PATIENT") ? "selected" : "" %>>CANCELLED_BY_PATIENT</option>
            <option value="CANCELLED_BY_CLINIC" <%= isSelected(selectedStatus, "CANCELLED_BY_CLINIC") ? "selected" : "" %>>CANCELLED_BY_CLINIC</option>
            <option value="NO_SHOW" <%= isSelected(selectedStatus, "NO_SHOW") ? "selected" : "" %>>NO_SHOW</option>
          </select>
        </div>
        <div class="field">
          <label for="date">Date</label>
          <input id="date" name="date" type="date" value="<%= selectedDate == null ? "" : selectedDate %>">
        </div>
        <div class="filter-actions">
          <button class="btn btn-primary" type="submit">Apply</button>
          <button class="btn btn-secondary" type="button" id="appointmentsFilterReset">Reset</button>
        </div>
      </form>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Patient</th>
              <th>Service</th>
              <th>Date</th>
              <th>Time</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <% if (appointments.isEmpty()) { %>
            <tr>
              <td colspan="7">No appointments match the current filter.</td>
            </tr>
            <% } else {
                for (Appointment appointment : appointments) {
            %>
            <tr>
              <td><%= appointment.getDisplayCode() %></td>
              <td><%= appointment.getPatientName() %></td>
              <td><%= appointment.getServiceLabel() %></td>
              <td><%= appointment.getDisplayDateLabel() %></td>
              <td><%= appointment.getTimeRangeLabel() %></td>
              <td><span class="status-chip appointment-status-chip <%= appointmentStatusClass(appointment.getStatus()) %>"><%= appointment.getStatus() %></span></td>
              <td>
                <% if (canUpdate(appointment.getStatus())) { %>
                <button
                  type="button"
                  class="btn btn-secondary"
                  data-update-button="true"
                  data-appointment-id="<%= appointment.getAppointmentId() %>"
                  data-appointment-code="<%= appointment.getDisplayCode() %>"
                  data-patient-name="<%= appointment.getPatientName() %>"
                  data-service-name="<%= appointment.getServiceLabel() %>"
                  data-schedule-label="<%= appointment.getScheduleLabel() %>"
                  data-current-status="<%= appointment.getStatus() %>"
                  data-default-action="<%= appointment.getSuggestedUpdateAction() %>">
                  Update
                </button>
                <% } else { %>
                <span class="muted">No update</span>
                <% } %>
              </td>
            </tr>
            <%      }
               } %>
          </tbody>
        </table>
      </div>
    </section>

    <section class="card" id="updatePanel" <%= updatePanelOpen ? "" : "hidden" %>>
      <h2 class="section-title">Attendance and outcome update</h2>
      <div class="quick-note" id="selectedAppointmentNote" style="margin-bottom: 14px;">
        <%= selectedAppointmentCode == null || selectedAppointmentCode.trim().isEmpty() ? "No appointment selected." : "Selected " + selectedAppointmentCode + " for update." %>
      </div>
      <form class="form-grid" action="<%= request.getContextPath() %>/staff/appointments/manage" method="post" id="updateForm">
        <input type="hidden" id="updateAppointmentId" name="appointmentId" value="">
        <input type="hidden" name="service" value="<%= selectedService == null ? "" : selectedService %>">
        <input type="hidden" name="status" value="<%= selectedStatus == null ? "" : selectedStatus %>">
        <input type="hidden" name="patientName" value="<%= selectedPatientNameFilter == null ? "" : selectedPatientNameFilter %>">
        <input type="hidden" name="date" value="<%= selectedDate == null ? "" : selectedDate %>">
        <input type="hidden" id="selectedAppointmentCode" name="selectedAppointmentCode" value="<%= selectedAppointmentCode == null ? "" : selectedAppointmentCode %>">
        <input type="hidden" id="selectedPatientName" name="selectedPatientName" value="<%= selectedPatientName == null ? "" : selectedPatientName %>">
        <input type="hidden" id="selectedServiceName" name="selectedServiceName" value="<%= selectedServiceName == null ? "" : selectedServiceName %>">
        <input type="hidden" id="selectedScheduleLabel" name="selectedScheduleLabel" value="<%= selectedScheduleLabel == null ? "" : selectedScheduleLabel %>">
        <div class="field">
          <label for="updateAppointmentCode">Appointment ID</label>
          <input id="updateAppointmentCode" type="text" readonly placeholder="A-1025" value="<%= selectedAppointmentCode == null ? "" : selectedAppointmentCode %>">
        </div>
        <div class="field">
          <label for="updateAction">Update action</label>
          <select id="updateAction" name="updateAction">
            <option value="APPROVE" <%= isSelected(selectedUpdateAction, "APPROVE") ? "selected" : "" %>>Approve booking</option>
            <option value="REJECT_BY_CLINIC" <%= isSelected(selectedUpdateAction, "REJECT_BY_CLINIC") ? "selected" : "" %>>Reject booking</option>
            <option value="CHECK_IN" <%= isSelected(selectedUpdateAction, "CHECK_IN") || selectedUpdateAction == null ? "selected" : "" %>>Check-in patient</option>
            <option value="NO_SHOW" <%= isSelected(selectedUpdateAction, "NO_SHOW") ? "selected" : "" %>>Mark no-show</option>
            <option value="COMPLETE" <%= isSelected(selectedUpdateAction, "COMPLETE") ? "selected" : "" %>>Complete appointment</option>
            <option value="CANCEL_BY_CLINIC" <%= isSelected(selectedUpdateAction, "CANCEL_BY_CLINIC") ? "selected" : "" %>>Cancel by clinic</option>
          </select>
        </div>
        <div class="field">
          <label for="updatePatientName">Patient</label>
          <input id="updatePatientName" type="text" readonly value="<%= selectedPatientName == null ? "" : selectedPatientName %>">
        </div>
        <div class="field">
          <label for="updateServiceName">Service</label>
          <input id="updateServiceName" type="text" readonly value="<%= selectedServiceName == null ? "" : selectedServiceName %>">
        </div>
        <div class="field field-full">
          <label for="updateScheduleLabel">Time</label>
          <input id="updateScheduleLabel" type="text" readonly value="<%= selectedScheduleLabel == null ? "" : selectedScheduleLabel %>">
        </div>
        <div class="field field-full">
          <label for="remark">Remark / reason</label>
          <textarea id="remark" name="remark" rows="3" placeholder="Optional note for status history"><%= selectedRemark == null ? "" : selectedRemark %></textarea>
        </div>
        <div class="field field-full form-actions">
          <button class="btn btn-primary" type="submit">Submit Update</button>
        </div>
      </form>
    </section>
  </main>

  <footer class="site-footer">
    <div class="container">CCHC Community Clinic System</div>
  </footer>

  <script>
    (function () {
      const filterForm = document.getElementById('appointmentsFilterForm');
      const filterResetButton = document.getElementById('appointmentsFilterReset');
      const updatePanel = document.getElementById('updatePanel');
      const updateAppointmentId = document.getElementById('updateAppointmentId');
      const updateAppointmentCode = document.getElementById('updateAppointmentCode');
      const updatePatientName = document.getElementById('updatePatientName');
      const updateServiceName = document.getElementById('updateServiceName');
      const updateScheduleLabel = document.getElementById('updateScheduleLabel');
      const updateAction = document.getElementById('updateAction');
      const updateRemark = document.getElementById('remark');
      const selectedAppointmentNote = document.getElementById('selectedAppointmentNote');

      if (filterForm && filterResetButton) {
        filterResetButton.addEventListener('click', function () {
          window.location.href = filterForm.action;
        });
      }

      function openUpdatePanel(button) {
        updatePanel.hidden = false;
        updateAppointmentId.value = button.dataset.appointmentId || '';
        updateAppointmentCode.value = button.dataset.appointmentCode || '';
        updatePatientName.value = button.dataset.patientName || '';
        updateServiceName.value = button.dataset.serviceName || '';
        updateScheduleLabel.value = button.dataset.scheduleLabel || '';
        updateAction.value = button.dataset.defaultAction || 'CHECK_IN';
        document.getElementById('selectedAppointmentCode').value = button.dataset.appointmentCode || '';
        document.getElementById('selectedPatientName').value = button.dataset.patientName || '';
        document.getElementById('selectedServiceName').value = button.dataset.serviceName || '';
        document.getElementById('selectedScheduleLabel').value = button.dataset.scheduleLabel || '';
        selectedAppointmentNote.textContent = 'Selected ' + (button.dataset.appointmentCode || 'appointment') + ' for update.';
        updatePanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
        updateRemark.focus();
      }

      document.querySelectorAll('[data-update-button="true"]').forEach(function (button) {
        button.addEventListener('click', function () {
          openUpdatePanel(button);
        });
      });

      if (updatePanel.hidden) {
        selectedAppointmentNote.textContent = 'No appointment selected.';
      }
    })();
  </script>

</body>
</html>