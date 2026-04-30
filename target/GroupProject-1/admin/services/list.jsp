<%@page import="ict.bean.ClinicServiceStatus" %>
  <%@page import="java.util.Collections" %>
    <%@page import="java.util.List" %>
      <%@page contentType="text/html" pageEncoding="UTF-8" %>
        <%@ include file="/admin/common/util.jspf" %>
          <% Object sessionUser=session.getAttribute("userInfo"); if (!(sessionUser instanceof ict.bean.User) ||
            !"ADMIN".equalsIgnoreCase(((ict.bean.User) sessionUser).getRole())) {
            response.sendRedirect(request.getContextPath() + "/login" ); return; } List<ClinicServiceStatus>
            serviceRecords = (List<ClinicServiceStatus>) request.getAttribute("serviceRecords");
              if (serviceRecords == null) {
              serviceRecords = Collections.emptyList();
              }

              Integer serviceCount = (Integer) request.getAttribute("serviceCount");
              if (serviceCount == null) {
              serviceCount = serviceRecords.size();
              }

              Integer activeServiceCount = (Integer) request.getAttribute("activeServiceCount");
              if (activeServiceCount == null) {
              activeServiceCount = 0;
              for (ClinicServiceStatus service : serviceRecords) {
              if (service != null && service.isActive()) {
              activeServiceCount++;
              }
              }
              }

              Integer inactiveServiceCount = (Integer) request.getAttribute("inactiveServiceCount");
              if (inactiveServiceCount == null) {
              inactiveServiceCount = Math.max(serviceCount - activeServiceCount, 0);
              }

              Integer averageServiceMinutes = (Integer) request.getAttribute("averageServiceMinutes");
              if (averageServiceMinutes == null) {
              averageServiceMinutes = 0;
              }
              %>
              <!doctype html>
              <html lang="en">

              <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>CCHC Admin - Service Management</title>
                <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
              </head>

              <body>
                <%@ include file="/admin/common/nav.jspf" %>

                  <main class="container">
                    <section>
                      <h1>Service Management</h1>
                      <p class="section-subtitle">Create, edit, and suspend clinic services from one catalog.</p>
                    </section>

                    <section class="summary-strip">
                      <article class="summary-box">
                        <h4>
                          <%= serviceCount %>
                        </h4>
                        <p>Total services</p>
                      </article>
                      <article class="summary-box">
                        <h4>
                          <%= activeServiceCount %>
                        </h4>
                        <p>Active services</p>
                      </article>
                      <article class="summary-box">
                        <h4>
                          <%= inactiveServiceCount %>
                        </h4>
                        <p>Suspended services</p>
                      </article>
                      <article class="summary-box">
                        <h4>
                          <%= averageServiceMinutes %>
                        </h4>
                        <p>Avg. service minutes</p>
                      </article>
                    </section>

                    <section class="action-bar">
                      <a class="btn btn-primary" href="<%= request.getContextPath() %>/admin/services/add">Add
                        Service</a>
                      <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/batch/import.html">Batch
                        Import</a>
                      <a class="btn btn-secondary"
                        href="<%= request.getContextPath() %>/admin/reports/utilization">Utilization Report</a>
                    </section>

                    <section class="card" style="margin-bottom: 18px;">
                      <%@ include file="/admin/common/service-tabs.jspf" %>
                    </section>

                    <section class="table-wrap">
                      <table>
                        <thead>
                          <tr>
                            <th>Service</th>
                            <th>Description</th>
                            <th>Approval</th>
                            <th>Walk-in</th>
                            <th>Avg Minutes</th>
                            <th>Status</th>
                            <th>Actions</th>
                          </tr>
                        </thead>
                        <tbody>
                          <% if (serviceRecords.isEmpty()) { %>
                            <tr>
                              <td colspan="7">No service records are available yet.</td>
                            </tr>
                            <% } else { for (ClinicServiceStatus service : serviceRecords) { if (service==null) {
                              continue; } %>
                              <tr>
                                <td>S-<%= service.getServiceId() %> <br><strong>
                                      <%= safeText(service.getServiceName()) %>
                                    </strong></td>
                                <td>
                                  <%= safeText(service.getServiceDescription()) %>
                                </td>
                                <td>
                                  <%= service.getApprovalLabel() %>
                                </td>
                                <td>
                                  <%= service.getWalkInLabel() %>
                                </td>
                                <td>
                                  <%= service.getAvgServiceMinutes() %>
                                </td>
                                <td><span class="status-chip <%= service.getStatusChipClass() %>">
                                    <%= service.getStatusLabel() %>
                                  </span></td>
                                <td>
                                  <div class="actions">
                                    <a class="btn btn-secondary"
                                      href="<%= request.getContextPath() %>/admin/services/edit?serviceId=<%= service.getServiceId() %>">Edit</a>
                                    <% if (service.isActive()) { %>
                                      <a class="btn btn-warning"
                                        href="<%= request.getContextPath() %>/admin/services/delete?serviceId=<%= service.getServiceId() %>">Deactivate</a>
                                      <% } else { %>
                                        <span class="status-chip status-pending">Suspended</span>
                                        <% } %>
                                  </div>
                                </td>
                              </tr>
                              <% } } %>
                        </tbody>
                      </table>
                    </section>
                  </main>

                  <footer class="site-footer">
                    <div class="container">Service management sample page.</div>
                  </footer>
              </body>

              </html>