<%@page import="ict.bean.ClinicService" %>
  <%@page import="ict.bean.Notification" %>
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

                List<Notification> notificationRecords = (List<Notification>)
                    request.getAttribute("notificationRecords");
                    if (notificationRecords == null) {
                    notificationRecords = Collections.emptyList();
                    }

                    Integer notificationCount = (Integer) request.getAttribute("notificationCount");
                    if (notificationCount == null) {
                    notificationCount = notificationRecords.size();
                    }

                    Integer unreadNotificationCount = (Integer) request.getAttribute("unreadNotificationCount");
                    if (unreadNotificationCount == null) {
                    unreadNotificationCount = 0;
                    }

                    Integer accountAlertCount = (Integer) request.getAttribute("accountAlertCount");
                    if (accountAlertCount == null) {
                    accountAlertCount = 0;
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
                      <title>CCHC Admin - Staff Audit Trail</title>
                      <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
                    </head>

                    <body>
                      <%@ include file="/admin/common/nav.jspf" %>

                        <main class="container">
                          <section>
                            <h1>Staff Audit Trail</h1>
                            <p class="section-subtitle">Comprehensive log of staff actions and automated system alerts
                              for
                              <strong>
                                <%= escapeHtml(reportMonthLabel) %>
                              </strong>.
                            </p>
                          </section>

                          <% if (reportError !=null && !reportError.trim().isEmpty()) { %>
                            <section class="notice" style="margin-bottom: 16px;">
                              <%= escapeHtml(reportError) %>
                            </section>
                            <% } %>

                              <section class="card" style="margin-bottom: 18px;">
                                <form class="filter-form" action="<%= request.getContextPath() %>/admin/audit/users"
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
                                    <%= notificationCount %>
                                  </h4>
                                  <p>Total actions logged</p>
                                </article>
                                <article class="summary-box">
                                  <h4>
                                    <%= unreadNotificationCount %>
                                  </h4>
                                  <p>New / unreviewed</p>
                                </article>
                                <article class="summary-box">
                                  <h4>
                                    <%= accountAlertCount %>
                                  </h4>
                                  <p>Security alerts</p>
                                </article>
                                <article class="summary-box">
                                  <h4>
                                    <%= activeClinics.size() %>
                                  </h4>
                                  <p>Active clinics</p>
                                </article>
                              </section>

                              <section class="card" style="margin-bottom: 18px;">
                                <%@ include file="/admin/common/audit-tabs.jspf" %>
                              </section>

                              <section class="table-wrap">
                                <table>
                                  <thead>
                                    <tr>
                                      <th>Log ID</th>
                                      <th>Action By (Staff)</th>
                                      <th>Clinic</th>
                                      <th>Action Type</th>
                                      <th>Action Detail</th>
                                      <th>Read State</th>
                                      <th>Timestamp</th>
                                      <th>Target Record</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    <% if (notificationRecords.isEmpty()) { %>
                                      <tr>
                                        <td colspan="8">No staff action logs were found for the selected month.</td>
                                      </tr>
                                      <% } else { for (Notification notification : notificationRecords) { %>
                                        <tr>
                                          <td>
                                            <%= notification.getDisplayCode() %>
                                          </td>
                                          <td>
                                            <%= safeText(notification.getRecipientName()) %>
                                          </td>
                                          <td>
                                            <%= safeText(notification.getClinicName()) %>
                                          </td>
                                          <td><span class="status-chip <%= notification.getTypeBadgeClass() %>">
                                              <%= safeText(notification.getTypeLabel()) %>
                                            </span></td>
                                          <td>
                                            <%= safeText(notification.getTitle()) %>
                                          </td>
                                          <td><span class="status-chip <%= notification.getReadStateClass() %>">
                                              <%= notification.getReadStateLabel() %>
                                            </span></td>
                                          <td>
                                            <%= notification.getCreatedAtLabel() %>
                                          </td>
                                          <td>
                                            <%= safeText(notification.getRelatedContextLabel()) %>
                                          </td>
                                        </tr>
                                        <% } } %>
                                  </tbody>
                                </table>
                              </section>
                        </main>

                        <footer class="site-footer">
                          <div class="container">User log sample page.</div>
                        </footer>
                    </body>

                    </html>