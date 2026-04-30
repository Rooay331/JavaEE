<%@page import="ict.bean.ClinicServiceStatus" %>
  <%@page contentType="text/html" pageEncoding="UTF-8" %>
    <%@ include file="/admin/common/util.jspf" %>
      <% Object sessionUser=session.getAttribute("userInfo"); if (!(sessionUser instanceof ict.bean.User) ||
        !"ADMIN".equalsIgnoreCase(((ict.bean.User) sessionUser).getRole())) {
        response.sendRedirect(request.getContextPath() + "/login" ); return; } ClinicServiceStatus
        serviceForm=(ClinicServiceStatus) request.getAttribute("serviceForm"); if (serviceForm==null) { serviceForm=new
        ClinicServiceStatus(); } %>
        <!doctype html>
        <html lang="en">

        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>CCHC Admin - Edit Service</title>
          <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
        </head>

        <body>
          <%@ include file="/admin/common/nav.jspf" %>

            <main class="container">
              <section class="form-wrap">
                <div class="form-card">
                  <div class="form-intro">
                    <h1>Edit Service</h1>
                    <p>Update the catalog record, availability flags, and display settings.</p>
                  </div>

                  <%@ include file="/admin/common/service-tabs.jspf" %>

                  <form class="form-grid" action="<%= request.getContextPath() %>/admin/services/edit" method="post">
                    <input type="hidden" name="serviceId"
                      value="<%= serviceForm.getServiceId() == null ? "" : serviceForm.getServiceId() %>">
                    <div class="field field-full">
                      <label for="serviceName">Service Name</label>
                      <input id="serviceName" name="serviceName" type="text"
                        value="<%= formValue(serviceForm.getServiceName()) %>" required>
                    </div>
                    <div class="field field-full">
                      <label for="description">Description</label>
                      <textarea id="description" name="description"
                        rows="3"><%= formValue(serviceForm.getServiceDescription()) %></textarea>
                    </div>
                    <div class="field">
                      <label for="requiresApproval">Requires Approval</label>
                      <select id="requiresApproval" name="requiresApproval">
                        <option value="0" <%=!serviceForm.isRequiresApproval() ? "selected" : "" %>>No</option>
                        <option value="1" <%=serviceForm.isRequiresApproval() ? "selected" : "" %>>Yes</option>
                      </select>
                    </div>
                    <div class="field">
                      <label for="walkInEnabled">Walk-in Enabled</label>
                      <select id="walkInEnabled" name="walkInEnabled">
                        <option value="1" <%=serviceForm.isWalkInEnabled() ? "selected" : "" %>>Yes</option>
                        <option value="0" <%=!serviceForm.isWalkInEnabled() ? "selected" : "" %>>No</option>
                      </select>
                    </div>
                    <div class="field">
                      <label for="avgServiceMinutes">Average Service Minutes</label>
                      <input id="avgServiceMinutes" name="avgServiceMinutes" type="number" min="1"
                        value="<%= serviceForm.getAvgServiceMinutes() <= 0 ? 10 : serviceForm.getAvgServiceMinutes() %>">
                    </div>
                    <div class="field">
                      <label for="isActive">Status</label>
                      <select id="isActive" name="isActive">
                        <option value="1" <%=serviceForm.isActive() ? "selected" : "" %>>ACTIVE</option>
                        <option value="0" <%=!serviceForm.isActive() ? "selected" : "" %>>SUSPENDED</option>
                      </select>
                    </div>
                    <div class="field field-full form-actions">
                      <button class="btn btn-primary" type="submit">Save Changes</button>
                      <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/services/list">Back</a>
                    </div>
                  </form>
                </div>
              </section>
            </main>

            <footer class="site-footer">
              <div class="container">Edit service sample page.</div>
            </footer>
        </body>

        </html>