<%@page import="ict.bean.Appointment" %>
  <%@page import="ict.bean.ClinicService" %>
    <%@page import="java.util.Collections" %>
      <%@page import="java.util.List" %>
        <%@page contentType="text/html" pageEncoding="UTF-8" %>
          <%@ include file="/admin/common/util.jspf" %>
            <% Object sessionUser=session.getAttribute("userInfo"); if (!(sessionUser instanceof ict.bean.User) ||
              !"ADMIN".equalsIgnoreCase(((ict.bean.User) sessionUser).getRole())) {
              response.sendRedirect(request.getContextPath() + "/login" ); return; } List<ClinicService> activeClinics =
              (List<ClinicService>) request.getAttribute("activeClinics");
                if (activeClinics == null) {
                activeClinics = Collections.emptyList();
                }

                List<Appointment> appointmentRecords = (List<Appointment>) request.getAttribute("appointmentRecords");
                    if (appointmentRecords == null) {
                    appointmentRecords = Collections.emptyList();
                    }

                    Integer appointmentCount = (Integer) request.getAttribute("appointmentCount");
                    if (appointmentCount == null) {
                    appointmentCount = appointmentRecords.size();
                    }

                    Integer completedAppointmentCount = (Integer) request.getAttribute("completedAppointmentCount");
                    if (completedAppointmentCount == null) {
                    completedAppointmentCount = 0;
                    }

                    Integer noShowAppointmentCount = (Integer) request.getAttribute("noShowAppointmentCount");
                    if (noShowAppointmentCount == null) {
                    noShowAppointmentCount = 0;
                    }

                    Integer cancelledAppointmentCount = (Integer) request.getAttribute("cancelledAppointmentCount");
                    if (cancelledAppointmentCount == null) {
                    cancelledAppointmentCount = 0;
                    }

                    Integer selectedClinicId = (Integer) request.getAttribute("selectedClinicId");
                    Integer selectedMonth = (Integer) request.getAttribute("selectedMonth");
                    if (selectedMonth == null) {
                    selectedMonth = java.time.LocalDate.now().getMonthValue();
                    }

                    Integer selectedYear = (Integer) request.getAttribute("selectedYear");
                    if (selectedYear == null) {
                    selectedYear = java.time.LocalDate.now().getYear();
                    }

                    String reportMonthLabel = (String) request.getAttribute("reportMonthLabel");
                    if (reportMonthLabel == null || reportMonthLabel.trim().isEmpty()) {
                    reportMonthLabel = monthName(selectedMonth) + " " + selectedYear;
                    }

                    String reportError = (String) request.getAttribute("reportError");
                    %>
                    <!doctype html>
                    <html lang="en">

                    <head>
                      <meta charset="UTF-8">
                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                      <title>CCHC Admin - Appointment Records</title>
                      <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
                    </head>

                    <body>
                      <%@ include file="/admin/common/nav.jspf" %>

                        <main class="container">
                          <section>
                            <h1>Appointment Records</h1>
                            <p class="section-subtitle">Browse appointment records by clinic for <strong>
                                <%= escapeHtml(reportMonthLabel) %>
                              </strong>.</p>
                          </section>

                          <% if (reportError !=null && !reportError.trim().isEmpty()) { %>
                            <section class="notice" style="margin-bottom: 16px;">
                              <%= escapeHtml(reportError) %>
                            </section>
                            <% } %>

                              <section class="card" style="margin-bottom: 18px;">
                                <form class="filter-form"
                                  action="<%= request.getContextPath() %>/admin/audit/appointments" method="get">
                                  <div class="field">
                                    <label for="clinicId">Clinic</label>
                                    <select id="clinicId" name="clinicId">
                                      <option value="" <%=selectedClinicId==null ? "selected" : "" %>>All clinics
                                      </option>
                                      <% for (ClinicService clinic : activeClinics) { %>
                                        <option value="<%= clinic.getClinicId() %>" <%=selectedClinicId !=null &&
                                          selectedClinicId.equals(clinic.getClinicId()) ? "selected" : "" %>><%=
                                            escapeHtml(clinic.getClinicName()) %>
                                        </option>
                                        <% } %>
                                    </select>
                                  </div>
                                  <div class="field">
                                    <label for="month">Month</label>
                                    <select id="month" name="month">
                                      <% for (int month=1; month <=12; month++) { %>
                                        <option value="<%= month %>" <%=selectedMonth==month ? "selected" : "" %>><%=
                                            monthName(month) %>
                                        </option>
                                        <% } %>
                                    </select>
                                  </div>
                                  <div class="field">
                                    <label for="year">Year</label>
                                    <select id="year" name="year">
                                      <% for (int year=selectedYear - 2; year <=selectedYear + 2; year++) { %>
                                        <option value="<%= year %>" <%=selectedYear==year ? "selected" : "" %>><%= year
                                            %>
                                        </option>
                                        <% } %>
                                    </select>
                                  </div>
                                  <div class="filter-actions">
                                    <button class="btn btn-primary" type="submit">Refresh</button>
                                  </div>
                                </form>
                              </section>

                              <section class="summary-strip">
                                <article class="summary-box">
                                  <h4>
                                    <%= appointmentCount %>
                                  </h4>
                                  <p>Total records</p>
                                </article>
                                <article class="summary-box">
                                  <h4>
                                    <%= completedAppointmentCount %>
                                  </h4>
                                  <p>Completed</p>
                                </article>
                                <article class="summary-box">
                                  <h4>
                                    <%= noShowAppointmentCount %>
                                  </h4>
                                  <p>No-shows</p>
                                </article>
                                <article class="summary-box">
                                  <h4>
                                    <%= cancelledAppointmentCount %>
                                  </h4>
                                  <p>Cancelled</p>
                                </article>
                              </section>

                              <section class="card" style="margin-bottom: 18px;">
                                <%@ include file="/admin/common/audit-tabs.jspf" %>
                              </section>

                              <section class="table-wrap">
                                <table>
                                  <thead>
                                    <tr>
                                      <th>Appointment</th>
                                      <th>Patient</th>
                                      <th>Clinic / Service</th>
                                      <th>Date &amp; Time</th>
                                      <th>Status</th>
                                      <th>Reviewed By</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    <% if (appointmentRecords.isEmpty()) { %>
                                      <tr>
                                        <td colspan="6">No appointment records were found for the selected month.</td>
                                      </tr>
                                      <% } else { for (Appointment appointment : appointmentRecords) { %>
                                        <tr>
                                          <td>
                                            <%= appointment.getDisplayCode() %>
                                          </td>
                                          <td>
                                            <%= safeText(appointment.getPatientName()) %>
                                          </td>
                                          <td>
                                            <%= safeText(appointment.getClinicServiceLabel()) %>
                                          </td>
                                          <td>
                                            <%= safeText(appointment.getScheduleLabel()) %>
                                          </td>
                                          <td><span class="status-chip <%= appointment.getStatusChipClass() %>">
                                              <%= safeText(appointment.getStatus()) %>
                                            </span></td>
                                          <td>
                                            <%= safeText(appointment.getReviewedBy()) %>
                                          </td>
                                        </tr>
                                        <% } } %>
                                  </tbody>
                                </table>
                              </section>
                        </main>

                        <footer class="site-footer">
                          <div class="container">Appointment audit sample page.</div>
                        </footer>
                    </body>

                    </html>