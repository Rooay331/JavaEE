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
        response.sendRedirect(request.getContextPath() + "/admin/profile/view");
        return;
    }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Admin - Profile View</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="/admin/common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Admin Profile</h1>
      <p class="section-subtitle">View your administrator account details and security posture.</p>
    </section>

    <nav class="sub-nav" aria-label="Admin profile navigation">
      <a class="active" href="<%= request.getContextPath() %>/admin/profile/view">View Profile</a>
      <a href="<%= request.getContextPath() %>/admin/profile/edit">Edit Profile</a>
      <a href="<%= request.getContextPath() %>/admin/profile/change-password">Change Password</a>
    </nav>

    <section class="layout-split">
      <div class="card">
        <h2 class="section-title">Account details</h2>
        <div class="list-group">
          <div class="list-item"><h4>Admin ID</h4><p><%= userCode(profileUser.getUserId()) %></p></div>
          <div class="list-item"><h4>Full name</h4><p><%= safeText(profileUser.getFullName()) %></p></div>
          <div class="list-item"><h4>Email</h4><p><%= safeText(profileUser.getEmail()) %></p></div>
          <div class="list-item"><h4>Phone</h4><p><%= safeText(profileUser.getPhone()) %></p></div>
          <div class="list-item"><h4>Date of birth</h4><p><%= formatDate(profileUser.getDateOfBirth()) %></p></div>
          <div class="list-item"><h4>Gender</h4><p><%= safeText(profileUser.getGender()) %></p></div>
          <div class="list-item"><h4>Role</h4><p><span class="status-chip <%= roleChipClass(profileUser.getRole()) %>"><%= safeText(profileUser.getRole()) %></span></p></div>
          <div class="list-item"><h4>Status</h4><p><span class="status-chip <%= userStatusClass(profileUser.getIsActive()) %>"><%= profileUser.getIsActive() == 1 ? "ACTIVE" : "SUSPENDED" %></span></p></div>
        </div>
      </div>

      <aside class="card">
        <h2 class="section-title">Security</h2>
        <div class="list-group">
          <div class="list-item"><h4>Last login</h4><p><%= formatDateTime(profileUser.getLastLoginAt()) %></p></div>
          <div class="list-item"><h4>Created at</h4><p><%= formatDateTime(profileUser.getCreatedAt()) %></p></div>
          <div class="list-item"><h4>Updated at</h4><p><%= formatDateTime(profileUser.getUpdatedAt()) %></p></div>
          <div class="list-item"><h4>Password</h4><p>Change this regularly to keep the account secure.</p></div>
        </div>

        <div class="action-bar" style="margin-top: 16px;">
          <a class="btn btn-primary" href="<%= request.getContextPath() %>/admin/profile/edit">Edit Profile</a>
          <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/profile/change-password">Change Password</a>
        </div>
      </aside>
    </section>
  </main>

  <footer class="site-footer"><div class="container">Admin profile sample view page.</div></footer>
</body>
</html>