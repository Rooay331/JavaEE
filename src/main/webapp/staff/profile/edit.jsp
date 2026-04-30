<%@page import="ict.bean.User"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.time.format.DateTimeFormatter"%>
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
      response.sendRedirect(request.getContextPath() + "/staff/profile/edit");
      return;
  }

  User profileUser = (User) request.getAttribute("profileUser");
  String assignedClinicName = (String) request.getAttribute("assignedClinicName");
  if (assignedClinicName == null) {
      assignedClinicName = "Clinic ID " + loggedInUser.getClinicId();
  }

  String profilePageMode = (String) request.getAttribute("profilePageMode");
  if (profilePageMode == null) {
      profilePageMode = "edit";
  }

  String servletPath = request.getServletPath();
  boolean viewActive = "/staff/profile/view".equals(servletPath) || "/staff/profile/view.jsp".equals(servletPath);
  boolean editActive = "/staff/profile/edit".equals(servletPath) || "/staff/profile/edit.jsp".equals(servletPath);
  boolean passwordActive = "/staff/profile/change-password".equals(servletPath) || "/staff/profile/change-password.jsp".equals(servletPath);
%>
<%!
  private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("d-M-yyyy");

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

  private String formatDateInput(LocalDate value) {
    return value == null ? "" : DISPLAY_DATE_FORMAT.format(value);
  }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Staff Profile - Edit</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="../common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Edit Profile</h1>
      <p class="section-subtitle">Update your own staff profile fields. Clinic assignment is managed separately.</p>
    </section>

    <section class="notice" style="margin-bottom: 18px;">
      Editing the profile for <strong><%= escapeHtml(assignedClinicName) %></strong>.
    </section>

    <nav class="sub-nav" aria-label="Staff profile navigation">
      <a class="<%= viewActive ? "active" : "" %>" href="<%= request.getContextPath() %>/staff/profile/view">View Profile</a>
      <a class="<%= editActive ? "active" : "" %>" href="<%= request.getContextPath() %>/staff/profile/edit">Edit Profile</a>
      <a class="<%= passwordActive ? "active" : "" %>" href="<%= request.getContextPath() %>/staff/profile/change-password">Change Password</a>
    </nav>

    <section class="form-wrap">
      <div class="form-card">
        <div class="form-intro">
          <h2 class="section-title">Profile details</h2>
          <p>Keep your contact details accurate so clinic staff can reach you when needed.</p>
        </div>

        <form class="form-grid" action="<%= request.getContextPath() %>/staff/profile/edit" method="post">
          <div class="field">
            <label for="fullName">Full name</label>
            <input id="fullName" name="fullName" type="text" value="<%= profileUser.getFullName() == null ? "" : escapeHtml(profileUser.getFullName()) %>" required>
          </div>
          <div class="field">
            <label for="phone">Phone</label>
            <input id="phone" name="phone" type="tel" value="<%= profileUser.getPhone() == null ? "" : escapeHtml(profileUser.getPhone()) %>">
          </div>
          <div class="field field-full">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" value="<%= profileUser.getEmail() == null ? "" : escapeHtml(profileUser.getEmail()) %>">
          </div>
          <div class="field">
            <label for="dateOfBirth">Date of birth</label>
            <input id="dateOfBirth" name="dateOfBirth" type="text" placeholder="12-6-2026" pattern="\d{1,2}-\d{1,2}-\d{4}" inputmode="numeric" value="<%= formatDateInput(profileUser.getDateOfBirth()) %>">
          </div>
          <div class="field">
            <label for="gender">Gender</label>
            <select id="gender" name="gender">
              <option value="" <%= profileUser.getGender() == null ? "selected" : "" %>>Select gender</option>
              <option value="MALE" <%= "MALE".equalsIgnoreCase(profileUser.getGender()) ? "selected" : "" %>>Male</option>
              <option value="FEMALE" <%= "FEMALE".equalsIgnoreCase(profileUser.getGender()) ? "selected" : "" %>>Female</option>
            </select>
          </div>
          <div class="field field-full">
            <div class="quick-note">Clinic assignment remains read-only. Updates are limited to your own profile fields.</div>
          </div>
          <div class="field field-full form-actions">
            <button class="btn btn-primary" type="submit">Save Changes</button>
            <a class="btn btn-secondary" href="<%= request.getContextPath() %>/staff/profile/view">Cancel</a>
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