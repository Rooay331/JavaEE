<%@page import="ict.bean.User"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
  User loggedInUser = null;
  Object sessionUser = session.getAttribute("userInfo");
  if (sessionUser instanceof User) {
      loggedInUser = (User) sessionUser;
  }

  if (loggedInUser == null || !"STAFF".equalsIgnoreCase(loggedInUser.getRole()) || loggedInUser.getClinicId() == null) {
      response.sendRedirect(request.getContextPath() + "/login");
      return;
  }

  if (request.getAttribute("profileUser") == null) {
      response.sendRedirect(request.getContextPath() + "/staff/profile/change-password");
      return;
  }

  User profileUser = (User) request.getAttribute("profileUser");
  String assignedClinicName = (String) request.getAttribute("assignedClinicName");
  if (assignedClinicName == null) {
      assignedClinicName = "Clinic ID " + loggedInUser.getClinicId();
  }

  String servletPath = request.getServletPath();
  boolean viewActive = "/staff/profile/view".equals(servletPath) || "/staff/profile/view.jsp".equals(servletPath);
  boolean editActive = "/staff/profile/edit".equals(servletPath) || "/staff/profile/edit.jsp".equals(servletPath);
  boolean passwordActive = "/staff/profile/change-password".equals(servletPath) || "/staff/profile/change-password.jsp".equals(servletPath);
%>
<%!
  private String escapeHtml(String value) {
    if (value == null) {
      return "";
    }

    return value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;");
  }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Staff Profile - Change Password</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="../common/nav.jspf" %>

  <main class="container">
    <section class="form-wrap">
      <div class="form-card">
        <div class="form-intro">
          <h1>Change Password</h1>
          <p>Use a strong password to keep your staff account secure.</p>
        </div>

        <section class="notice" style="margin-bottom: 18px;">
          Password changes apply to <strong><%= profileUser.getFullName() == null ? "your account" : escapeHtml(profileUser.getFullName()) %></strong> at <strong><%= escapeHtml(assignedClinicName) %></strong>.
        </section>

        <nav class="sub-nav" aria-label="Staff profile navigation">
          <a class="<%= viewActive ? "active" : "" %>" href="<%= request.getContextPath() %>/staff/profile/view">View Profile</a>
          <a class="<%= editActive ? "active" : "" %>" href="<%= request.getContextPath() %>/staff/profile/edit">Edit Profile</a>
          <a class="<%= passwordActive ? "active" : "" %>" href="<%= request.getContextPath() %>/staff/profile/change-password">Change Password</a>
        </nav>

        <form class="form-grid" action="<%= request.getContextPath() %>/staff/profile/change-password" method="post">
          <div class="field field-full">
            <label for="currentPassword">Current password</label>
            <input id="currentPassword" name="currentPassword" type="password" required autocomplete="current-password">
          </div>
          <div class="field">
            <label for="newPassword">New password</label>
            <input id="newPassword" name="newPassword" type="password" minlength="8" required autocomplete="new-password">
          </div>
          <div class="field">
            <label for="confirmPassword">Confirm new password</label>
            <input id="confirmPassword" name="confirmPassword" type="password" minlength="8" required autocomplete="new-password">
          </div>
          <div class="field field-full">
            <div class="quick-note">Tip: Use at least 8 characters with mixed letters, numbers, and symbols.</div>
          </div>
          <div class="field field-full form-actions">
            <button class="btn btn-primary" type="submit">Update Password</button>
            <a class="btn btn-secondary" href="<%= request.getContextPath() %>/staff/profile/view">Back to Profile</a>
          </div>
        </form>
      </div>
    </section>
  </main>

  <footer class="site-footer">
    <div class="container">CCHC Community Clinic System</div>
  </footer>
</body>
</html>