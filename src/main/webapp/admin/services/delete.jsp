<%@page import="ict.bean.ClinicServiceStatus" %>
  <%@page contentType="text/html" pageEncoding="UTF-8" %>
    <%@ include file="/admin/common/util.jspf" %>
      <% Object sessionUser=session.getAttribute("userInfo"); if (!(sessionUser instanceof ict.bean.User) ||
        !"ADMIN".equalsIgnoreCase(((ict.bean.User) sessionUser).getRole())) {
        response.sendRedirect(request.getContextPath() + "/login" ); return; } ClinicServiceStatus
        serviceForm=(ClinicServiceStatus) request.getAttribute("serviceForm"); if (serviceForm==null) {
        response.sendRedirect(request.getContextPath() + "/admin/services/list" ); return; } %>
        <!doctype html>
        <html lang="en">

        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>CCHC Admin - Deactivate Service</title>
          <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
        </head>

        <body>
          <%@ include file="/admin/common/nav.jspf" %>

            <main class="container">
              <section class="form-wrap">
                <div class="form-card">
                  <div class="form-intro">
                    <h1>Deactivate Service</h1>
                    <p>This will suspend the selected service from active use. You can reactivate it later from the edit
                      page.</p>
                  </div>

                  <%@ include file="/admin/common/service-tabs.jspf" %>

                  <section class="notice notice-error" style="margin-bottom: 16px;">
                    You are about to deactivate <strong>
                      <%= safeText(serviceForm.getServiceName()) %>
                    </strong>.
                  </section>

                  <div class="summary-strip" style="margin-bottom: 18px;">
                    <article class="summary-box">
                      <h4>
                        <%= serviceForm.getServiceId()==null ? "-" : "S-" + serviceForm.getServiceId() %>
                      </h4>
                      <p>Service ID</p>
                    </article>
                    <article class="summary-box">
                      <h4>
                        <%= serviceForm.getStatusLabel() %>
                      </h4>
                      <p>Current status</p>
                    </article>
                    <article class="summary-box">
                      <h4>
                        <%= serviceForm.getAvgServiceMinutes() %>
                      </h4>
                      <p>Avg. minutes</p>
                    </article>
                    <article class="summary-box">
                      <h4>
                        <%= serviceForm.getApprovalLabel() %>
                      </h4>
                      <p>Requires approval</p>
                    </article>
                  </div>

                  <form class="form-grid" action="<%= request.getContextPath() %>/admin/services/delete" method="post">
                    <input type="hidden" name="serviceId"
                      value="<%= serviceForm.getServiceId() == null ? "" : serviceForm.getServiceId() %>">
                    <div class="field field-full form-actions">
                      <button class="btn btn-warning" type="submit">Deactivate Service</button>
                      <a class="btn btn-secondary"
                        href="<%= request.getContextPath() %>/admin/services/edit?serviceId=<%= serviceForm.getServiceId() == null ? "" : serviceForm.getServiceId() %>">Edit
                        Instead</a>
                      <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/services/list">Cancel</a>
                    </div>
                  </form>
                </div>
              </section>
            </main>

            <footer class="site-footer">
              <div class="container">Deactivate service sample page.</div>
            </footer>
        </body>

        </html>