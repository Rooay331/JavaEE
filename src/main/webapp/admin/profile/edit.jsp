<%@page import="ict.bean.User"%>
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

    User profileUser = (User) request.getAttribute("profileUser");
    if (profileUser == null) {
        response.sendRedirect(request.getContextPath() + "/admin/profile/edit");
        return;
    }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Admin - Edit Profile</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="/admin/common/nav.jspf" %>

  <main class="container">
    <section class="form-wrap">
      <div class="form-card">
        <div class="form-intro">
          <h1>Edit Admin Profile</h1>
          <p>Update your contact details and personal information.</p>
        </div>

        <nav class="sub-nav" aria-label="Admin profile navigation">
          <a href="<%= request.getContextPath() %>/admin/profile/view">View Profile</a>
          <a class="active" href="<%= request.getContextPath() %>/admin/profile/edit">Edit Profile</a>
          <a href="<%= request.getContextPath() %>/admin/profile/change-password">Change Password</a>
        </nav>

        <form class="form-grid" action="<%= request.getContextPath() %>/admin/profile/edit" method="post">
          <div class="field field-full">
            <label for="fullName">Full Name</label>
            <input id="fullName" name="fullName" type="text" value="<%= formValue(profileUser.getFullName()) %>" required>
          </div>
          <div class="field">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" value="<%= formValue(profileUser.getEmail()) %>">
          </div>
          <div class="field">
            <label for="phone">Phone</label>
            <input id="phone" name="phone" type="tel" value="<%= formValue(profileUser.getPhone()) %>">
          </div>
          <div class="field">
            <label for="dateOfBirth">Date of Birth</label>
            <input id="dateOfBirth" name="dateOfBirth" type="date" value="<%= profileUser.getDateOfBirth() == null ? "" : profileUser.getDateOfBirth().toString() %>">
          </div>
          <div class="field">
            <label for="gender">Gender</label>
            <select id="gender" name="gender">
              <option value="" <%= profileUser.getGender() == null || profileUser.getGender().trim().isEmpty() ? "selected" : "" %>>Not specified</option>
              <option value="MALE" <%= "MALE".equalsIgnoreCase(profileUser.getGender()) ? "selected" : "" %>>MALE</option>
              <option value="FEMALE" <%= "FEMALE".equalsIgnoreCase(profileUser.getGender()) ? "selected" : "" %>>FEMALE</option>
            </select>
          </div>
          <div class="field field-full form-actions">
            <button class="btn btn-primary" type="submit">Save Profile</button>
            <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/profile/view">Cancel</a>
          </div>
        </form>
      </div>
    </section>
  </main>

  <footer class="site-footer"><div class="container">Admin profile edit sample page.</div></footer>
</body>
</html>