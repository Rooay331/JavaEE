<%@page import="ict.bean.ClinicService" %>
  <%@page import="ict.bean.IncidentLog" %>
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

                List<IncidentLog> incidentRecords = (List<IncidentLog>) request.getAttribute("incidentRecords");
                    if (incidentRecords == null) {
                    incidentRecords = Collections.emptyList();
                    }

                    Integer incidentCount = (Integer) request.getAttribute("incidentCount");
                    if (incidentCount == null) {
                    incidentCount = incidentRecords.size();
                    }

                    Integer openIncidentCount = (Integer) request.getAttribute("openIncidentCount");
                    if (openIncidentCount == null) {
                    openIncidentCount = 0;
                    }

                    Integer resolvedIncidentCount = (Integer) request.getAttribute("resolvedIncidentCount");
                    if (resolvedIncidentCount == null) {
                    resolvedIncidentCount = 0;
                    }

                    Integer criticalIncidentCount = (Integer) request.getAttribute("criticalIncidentCount");
                    if (criticalIncidentCount == null) {
                    criticalIncidentCount = 0;
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
                      <title>CCHC Admin - Operational Logs</title>
                      <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
                    </head>

                    <body>
                      <%@ include file="/admin/common/nav.jspf" %>

                        <main class="container">
                          <section>
                            <h1>Operational Logs</h1>
                            <p class="section-subtitle">Review incident records for <strong>
                                <%= escapeHtml(reportMonthLabel) %>
                              </strong>.</p>
                          </section>

                          <% if (reportError !=null && !reportError.trim().isEmpty()) { %>
                            <section class="notice" style="margin-bottom: 16px;">
                              <%= escapeHtml(reportError) %>
                            </section>
                            <% } %>

                              <section class="card" style="margin-bottom: 18px;">
                                <form class="filter-form" action="<%= request.getContextPath() %>/admin/audit/config"
                                  method="get">
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
                                    <%= incidentCount %>
                                  </h4>
                                  <p>Total incidents</p>
                                </article>
                                <article class="summary-box">
                                  <h4>
                                    <%= openIncidentCount %>
                                  </h4>
                                  <p>Open incidents</p>
                                </article>
                                <article class="summary-box">
                                  <h4>
                                    <%= resolvedIncidentCount %>
                                  </h4>
                                  <p>Resolved incidents</p>
                                </article>
                                <article class="summary-box">
                                  <h4>
                                    <%= criticalIncidentCount %>
                                  </h4>
                                  <p>High / critical</p>
                                </article>
                              </section>

                              <section class="card" style="margin-bottom: 18px;">
                                <%@ include file="/admin/common/audit-tabs.jspf" %>
                              </section>

                              <section class="table-wrap">
                                <table>
                                  <thead>
                                    <tr>
                                      <th>Incident</th>
                                      <th>Clinic / Service</th>
                                      <th>Reporter</th>
                                      <th>Severity</th>
                                      <th>Status</th>
                                      <th>Occurred At</th>
                                      <th>Resolved At</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    <% if (incidentRecords.isEmpty()) { %>
                                      <tr>
                                        <td colspan="7">No operational logs were found for the selected month.</td>
                                      </tr>
                                      <% } else { for (IncidentLog incident : incidentRecords) { %>
                                        <tr>
                                          <td>
                                            <%= incident.getDisplayCode() %> <br><strong>
                                                <%= safeText(incident.getTitle()) %>
                                              </strong>
                                          </td>
                                          <td>
                                            <%= safeText(incident.getContextLabel()) %>
                                          </td>
                                          <td>
                                            <%= safeText(incident.getReporterLabel()) %>
                                          </td>
                                          <td><span class="status-chip <%= incident.getSeverityChipClass() %>">
                                              <%= safeText(incident.getSeverity()) %>
                                            </span></td>
                                          <td><span class="status-chip <%= incident.getStatusChipClass() %>">
                                              <%= safeText(incident.getStatus()) %>
                                            </span></td>
                                          <td>
                                            <%= incident.getOccurredAtLabel() %>
                                          </td>
                                          <td>
                                            <%= safeText(incident.getResolvedAtLabel()) %>
                                          </td>
                                        </tr>
                                        <% } } %>
                                  </tbody>
                                </table>
                              </section>
                        </main>

                        <footer class="site-footer">
                          <div class="container">Operational log sample page.</div>
                        </footer>
                    </body>

                    </html>