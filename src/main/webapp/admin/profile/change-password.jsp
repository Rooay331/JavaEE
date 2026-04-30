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
        response.sendRedirect(request.getContextPath() + "/admin/profile/change-password");
        return;
    }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Admin - Change Password</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="/admin/common/nav.jspf" %>

  <main class="container">
    <section class="form-wrap">
      <div class="form-card">
        <div class="form-intro">
          <h1>Change Password</h1>
          <p>Keep your administrator account secure by updating the password regularly.</p>
        </div>

        <nav class="sub-nav" aria-label="Admin profile navigation">
          <a href="<%= request.getContextPath() %>/admin/profile/view">View Profile</a>
          <a href="<%= request.getContextPath() %>/admin/profile/edit">Edit Profile</a>
          <a class="active" href="<%= request.getContextPath() %>/admin/profile/change-password">Change Password</a>
        </nav>

        <form class="form-grid" action="<%= request.getContextPath() %>/admin/profile/change-password" method="post">
          <div class="field field-full">
            <label for="currentPassword">Current Password</label>
            <input id="currentPassword" name="currentPassword" type="password" required>
          </div>
          <div class="field">
            <label for="newPassword">New Password</label>
            <input id="newPassword" name="newPassword" type="password" minlength="8" required>
          </div>
          <div class="field">
            <label for="confirmPassword">Confirm Password</label>
            <input id="confirmPassword" name="confirmPassword" type="password" minlength="8" required>
          </div>
          <div class="field field-full">
            <div class="quick-note">Use at least 8 characters with uppercase, number, and symbol.</div>
          </div>
          <div class="field field-full form-actions">
            <button class="btn btn-primary" type="submit">Update Password</button>
            <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/profile/view">Back</a>
          </div>
        </form>
      </div>
    </section>
  </main>

  <footer class="site-footer"><div class="container">Admin password update sample page.</div></footer>
</body>
</html>