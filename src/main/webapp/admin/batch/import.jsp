<%@page import="ict.bean.BatchImportResult" %>
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

                List<BatchImportResult> importResults = (List<BatchImportResult>) request.getAttribute("importResults");
                    if (importResults == null) {
                    importResults = Collections.emptyList();
                    }

                    Integer importSummaryTotal = (Integer) request.getAttribute("importSummaryTotal");
                    if (importSummaryTotal == null) {
                    importSummaryTotal = 0;
                    }

                    Integer importSummarySuccess = (Integer) request.getAttribute("importSummarySuccess");
                    if (importSummarySuccess == null) {
                    importSummarySuccess = 0;
                    }

                    Integer importSummaryFailed = (Integer) request.getAttribute("importSummaryFailed");
                    if (importSummaryFailed == null) {
                    importSummaryFailed = 0;
                    }

                    String importError = (String) request.getAttribute("importError");
                    String importMessage = (String) request.getAttribute("importMessage");
                    %>
                    <!doctype html>
                    <html lang="en">

                    <head>
                      <meta charset="UTF-8">
                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                      <title>CCHC Admin - Batch Import</title>
                      <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
                    </head>

                    <body>
                      <%@ include file="/admin/common/nav.jspf" %>

                        <main class="container">
                          <section>
                            <h1>Batch Import</h1>
                            <p class="section-subtitle">Upload CSV files to add or update users, services, and clinic
                              timeslots in bulk.</p>
                          </section>

                          <nav class="sub-nav" aria-label="Batch import navigation">
                            <a class="active" href="<%= request.getContextPath() %>/admin/batch/import">Import</a>
                            <a href="<%= request.getContextPath() %>/admin/batch/history">History</a>
                          </nav>

                          <% if (importError !=null && !importError.trim().isEmpty()) { %>
                            <section class="notice notice-error" style="margin-bottom: 16px;">
                              <%= escapeHtml(importError) %>
                            </section>
                            <% } %>

                              <% if (importMessage !=null && !importMessage.trim().isEmpty()) { %>
                                <section class="notice notice-success" style="margin-bottom: 16px;">
                                  <%= escapeHtml(importMessage) %>
                                </section>
                                <% } %>

                                  <section class="summary-strip">
                                    <article class="summary-box">
                                      <h4>
                                        <%= importSummaryTotal %>
                                      </h4>
                                      <p>Rows processed</p>
                                    </article>
                                    <article class="summary-box">
                                      <h4>
                                        <%= importSummarySuccess %>
                                      </h4>
                                      <p>Successful rows</p>
                                    </article>
                                    <article class="summary-box">
                                      <h4>
                                        <%= importSummaryFailed %>
                                      </h4>
                                      <p>Failed rows</p>
                                    </article>
                                    <article class="summary-box">
                                      <h4>
                                        <%= activeClinics.size() %>
                                      </h4>
                                      <p>Active clinics</p>
                                    </article>
                                  </section>

                                  <section class="layout-split">
                                    <div class="card">
                                      <h2 class="section-title">Upload CSV</h2>
                                      <form class="form-grid"
                                        action="<%= request.getContextPath() %>/admin/batch/import" method="post"
                                        enctype="multipart/form-data">
                                        <div class="field">
                                          <label for="importType">Import type</label>
                                          <select id="importType" name="importType" required>
                                            <option value="users">Users</option>
                                            <option value="services">Services</option>
                                            <option value="timeslots">Timeslots</option>
                                          </select>
                                        </div>
                                        <div class="field">
                                          <label for="mode">Mode</label>
                                          <select id="mode" name="mode">
                                            <option value="UPSERT">Upsert</option>
                                            <option value="INSERT">Insert only</option>
                                          </select>
                                        </div>
                                        <div class="field field-full">
                                          <label for="csvFile">CSV file</label>
                                          <input id="csvFile" name="csvFile" type="file" accept=".csv,text/csv"
                                            required>
                                        </div>
                                        <div class="field field-full form-actions">
                                          <button class="btn btn-primary" type="submit">Start Import</button>
                                          <a class="btn btn-secondary"
                                            href="<%= request.getContextPath() %>/admin/batch/history.html">Open
                                            History</a>
                                        </div>
                                      </form>
                                    </div>

                                    <aside class="card">
                                      <h2 class="section-title">CSV formats</h2>
                                      <div class="list-group">
                                        <div class="list-item">
                                          <h4>Users</h4>
                                          <p>user_id, full_name, email, phone, role, status, clinic_ref, date_of_birth,
                                            gender, password</p>
                                        </div>
                                        <div class="list-item">
                                          <h4>Services</h4>
                                          <p>service_id, service_name, description, avg_minutes, walkin_enabled,
                                            approval_required, status</p>
                                        </div>
                                        <div class="list-item">
                                          <h4>Timeslots</h4>
                                          <p>clinic_ref, service_ref, date, start_time, end_time, capacity</p>
                                        </div>
                                      </div>
                                    </aside>
                                  </section>

                                  <section class="card" style="margin-top: 18px;">
                                    <h2 class="section-title">Latest import results</h2>
                                    <div class="table-wrap">
                                      <table>
                                        <thead>
                                          <tr>
                                            <th>Row</th>
                                            <th>Type</th>
                                            <th>Identifier</th>
                                            <th>Action</th>
                                            <th>Status</th>
                                            <th>Message</th>
                                          </tr>
                                        </thead>
                                        <tbody>
                                          <% if (importResults.isEmpty()) { %>
                                            <tr>
                                              <td colspan="6">No import has been run yet.</td>
                                            </tr>
                                            <% } else { for (BatchImportResult result : importResults) { %>
                                              <tr>
                                                <td>
                                                  <%= result.getRowLabel() %>
                                                </td>
                                                <td>
                                                  <%= safeText(result.getEntityType()) %>
                                                </td>
                                                <td>
                                                  <%= safeText(result.getIdentifier()) %>
                                                </td>
                                                <td>
                                                  <%= safeText(result.getAction()) %>
                                                </td>
                                                <td><span class="status-chip <%= result.getStatusChipClass() %>">
                                                    <%= result.getStatusLabel() %>
                                                  </span></td>
                                                <td>
                                                  <%= safeText(result.getMessage()) %>
                                                </td>
                                              </tr>
                                              <% } } %>
                                        </tbody>
                                      </table>
                                    </div>
                                  </section>

                                  <section class="card" style="margin-top: 18px;">
                                    <h2 class="section-title">Clinic references</h2>
                                    <div class="list-group">
                                      <% if (activeClinics.isEmpty()) { %>
                                        <div class="list-item">
                                          <p>No active clinics are available for reference.</p>
                                        </div>
                                        <% } else { for (ClinicService clinic : activeClinics) { %>
                                          <div class="list-item">
                                            <h4>
                                              <%= escapeHtml(clinic.getClinicName()) %>
                                            </h4>
                                            <p>
                                              <%= escapeHtml(clinic.getServicesLabel()) %>
                                            </p>
                                          </div>
                                          <% } } %>
                                    </div>
                                  </section>
                        </main>

                        <footer class="site-footer">
                          <div class="container">Batch import sample page.</div>
                        </footer>
                    </body>

                    </html>