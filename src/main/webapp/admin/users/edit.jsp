<%@page import="ict.bean.ClinicService"%>
<%@page import="ict.bean.User"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
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

    User editUser = (User) request.getAttribute("editUser");
    if (editUser == null) {
        editUser = new User();
    }

    List<ClinicService> activeClinics = (List<ClinicService>) request.getAttribute("activeClinics");
    if (activeClinics == null) {
        activeClinics = Collections.emptyList();
    }

    Map<Integer, String> clinicNamesById = (Map<Integer, String>) request.getAttribute("clinicNamesById");
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Admin - Edit User</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="/admin/common/nav.jspf" %>

  <main class="container">
    <section class="form-wrap">
      <div class="form-card">
        <div class="form-intro">
          <h1>Edit User</h1>
          <p>Update account details, roles, clinic assignment, and status.</p>
        </div>

        <nav class="sub-nav" aria-label="User management navigation">
          <a href="<%= request.getContextPath() %>/admin/users/list">List Users</a>
          <a href="<%= request.getContextPath() %>/admin/users/create">Create User</a>
          <a class="active" href="<%= request.getContextPath() %>/admin/users/edit?userId=<%= editUser.getUserId() %>">Edit User</a>
        </nav>

        <form class="form-grid" action="<%= request.getContextPath() %>/admin/users/edit" method="post">
          <input type="hidden" name="userId" value="<%= editUser.getUserId() == null ? "" : editUser.getUserId() %>">
          <div class="field">
            <label for="role">Role</label>
            <select id="role" name="role" required>
              <option value="PATIENT" <%= "PATIENT".equalsIgnoreCase(editUser.getRole()) ? "selected" : "" %>>PATIENT</option>
              <option value="STAFF" <%= "STAFF".equalsIgnoreCase(editUser.getRole()) ? "selected" : "" %>>STAFF</option>
              <option value="ADMIN" <%= "ADMIN".equalsIgnoreCase(editUser.getRole()) ? "selected" : "" %>>ADMIN</option>
            </select>
          </div>
          <div class="field">
            <label for="isActive">Status</label>
            <select id="isActive" name="isActive">
              <option value="1" <%= editUser.getIsActive() != 0 ? "selected" : "" %>>ACTIVE</option>
              <option value="0" <%= editUser.getIsActive() == 0 ? "selected" : "" %>>SUSPENDED</option>
            </select>
          </div>
          <div class="field field-full">
            <label for="fullName">Full Name</label>
            <input id="fullName" name="fullName" type="text" value="<%= formValue(editUser.getFullName()) %>" required>
          </div>
          <div class="field">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" value="<%= formValue(editUser.getEmail()) %>">
          </div>
          <div class="field">
            <label for="phone">Phone</label>
            <input id="phone" name="phone" type="tel" value="<%= formValue(editUser.getPhone()) %>">
          </div>
          <div class="field">
            <label for="dateOfBirth">Date of Birth</label>
            <input id="dateOfBirth" name="dateOfBirth" type="date" value="<%= editUser.getDateOfBirth() == null ? "" : editUser.getDateOfBirth().toString() %>">
          </div>
          <div class="field">
            <label for="gender">Gender</label>
            <select id="gender" name="gender">
              <option value="" <%= editUser.getGender() == null || editUser.getGender().trim().isEmpty() ? "selected" : "" %>>Not specified</option>
              <option value="MALE" <%= "MALE".equalsIgnoreCase(editUser.getGender()) ? "selected" : "" %>>MALE</option>
              <option value="FEMALE" <%= "FEMALE".equalsIgnoreCase(editUser.getGender()) ? "selected" : "" %>>FEMALE</option>
            </select>
          </div>
          <div class="field field-full">
            <label for="clinicId">Assigned Clinic</label>
            <select id="clinicId" name="clinicId">
              <option value="">Not applicable</option>
              <% for (ClinicService clinic : activeClinics) { %>
              <option value="<%= clinic.getClinicId() %>" <%= editUser.getClinicId() != null && editUser.getClinicId().equals(clinic.getClinicId()) ? "selected" : "" %>><%= escapeHtml(clinic.getClinicName()) %></option>
              <% } %>
            </select>
            <p class="muted">Current clinic assignment: <%= clinicNameById(editUser.getClinicId(), clinicNamesById) %></p>
          </div>
          <div class="field field-full form-actions">
            <button class="btn btn-primary" type="submit">Save Changes</button>
            <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/users/list">Back</a>
          </div>
        </form>

        <div class="danger-zone" id="password-reset" style="margin-top: 18px;">
          <h3>Reset Password</h3>
          <form class="form-grid" action="<%= request.getContextPath() %>/admin/users/reset-password" method="post">
            <input type="hidden" name="userId" value="<%= editUser.getUserId() == null ? "" : editUser.getUserId() %>">
            <div class="field">
              <label for="newPassword">New Password</label>
              <input id="newPassword" name="newPassword" type="password" minlength="8" required>
            </div>
            <div class="field">
              <label for="confirmPassword">Confirm Password</label>
              <input id="confirmPassword" name="confirmPassword" type="password" minlength="8" required>
            </div>
            <div class="field field-full form-actions">
              <button class="btn btn-warning" type="submit">Reset Password</button>
            </div>
          </form>

          <h3>Deactivate User</h3>
          <form action="<%= request.getContextPath() %>/admin/users/delete" method="post">
            <input type="hidden" name="userId" value="<%= editUser.getUserId() == null ? "" : editUser.getUserId() %>">
            <button class="btn btn-danger" type="submit" onclick="return confirm('Deactivate this user?');">Deactivate User</button>
          </form>
        </div>
      </div>
    </section>
  </main>

  <footer class="site-footer"><div class="container">Edit user sample page.</div></footer>
</body>
</html>