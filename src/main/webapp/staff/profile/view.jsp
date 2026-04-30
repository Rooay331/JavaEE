<%@page import="ict.bean.User"%>
<%@page import="java.time.LocalDate"%>
<%@page import="java.time.LocalDateTime"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!
  private static final DateTimeFormatter PROFILE_DATE = DateTimeFormatter.ofPattern("d-M-yyyy");
  private static final DateTimeFormatter PROFILE_DATE_TIME = DateTimeFormatter.ofPattern("d-M-yyyy H:mm");

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

  private String formatDate(LocalDate value) {
    return value == null ? "Not recorded" : PROFILE_DATE.format(value);
  }

  private String formatDateTime(LocalDateTime value) {
    return value == null ? "Not recorded" : PROFILE_DATE_TIME.format(value);
  }

  private String safeText(String value) {
    return value == null || value.trim().isEmpty() ? "Not recorded" : escapeHtml(value);
  }

  private String staffIdLabel(Integer userId) {
    return userId == null ? "Staff account" : String.format("S-%04d", userId);
  }
%>
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
      response.sendRedirect(request.getContextPath() + "/staff/profile/view");
      return;
  }

  User profileUser = (User) request.getAttribute("profileUser");
  String assignedClinicName = (String) request.getAttribute("assignedClinicName");
  if (assignedClinicName == null) {
      assignedClinicName = "Clinic ID " + loggedInUser.getClinicId();
  }

  String profilePageMode = (String) request.getAttribute("profilePageMode");
  if (profilePageMode == null) {
      profilePageMode = "view";
  }

  String servletPath = request.getServletPath();
  boolean viewActive = "/staff/profile/view".equals(servletPath) || "/staff/profile/view.jsp".equals(servletPath);
  boolean editActive = "/staff/profile/edit".equals(servletPath) || "/staff/profile/edit.jsp".equals(servletPath);
  boolean passwordActive = "/staff/profile/change-password".equals(servletPath) || "/staff/profile/change-password.jsp".equals(servletPath);
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Staff Profile - View</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="../common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Staff Profile</h1>
      <p class="section-subtitle">View your staff account details and assigned clinic information.</p>
    </section>

    <section class="notice" style="margin-bottom: 18px;">
      Clinic-specific profile access for <strong><%= escapeHtml(assignedClinicName) %></strong>.
    </section>

    <nav class="sub-nav" aria-label="Staff profile navigation">
      <a class="<%= viewActive ? "active" : "" %>" href="<%= request.getContextPath() %>/staff/profile/view">View Profile</a>
      <a class="<%= editActive ? "active" : "" %>" href="<%= request.getContextPath() %>/staff/profile/edit">Edit Profile</a>
      <a class="<%= passwordActive ? "active" : "" %>" href="<%= request.getContextPath() %>/staff/profile/change-password">Change Password</a>
    </nav>

    <section class="layout-split">
      <div class="card">
        <h2 class="section-title">Account details</h2>
        <div class="list-group">
          <div class="list-item"><h4>Staff ID</h4><p><%= staffIdLabel(profileUser.getUserId()) %></p></div>
          <div class="list-item"><h4>Full name</h4><p><%= safeText(profileUser.getFullName()) %></p></div>
          <div class="list-item"><h4>Email</h4><p><%= safeText(profileUser.getEmail()) %></p></div>
          <div class="list-item"><h4>Phone</h4><p><%= safeText(profileUser.getPhone()) %></p></div>
          <div class="list-item"><h4>Date of birth</h4><p><%= formatDate(profileUser.getDateOfBirth()) %></p></div>
          <div class="list-item"><h4>Gender</h4><p><%= safeText(profileUser.getGender()) %></p></div>
          <div class="list-item"><h4>Role</h4><p><%= safeText(profileUser.getRole()) %></p></div>
          <div class="list-item"><h4>Account status</h4><p><%= profileUser.getIsActive() == 1 ? "Active" : "Inactive" %></p></div>
        </div>
      </div>

      <aside class="card">
        <h2 class="section-title">Assign clinic</h2>
        <div class="list-group">
          <div class="list-item">
            <h4>Primary clinic</h4>
            <p><%= escapeHtml(assignedClinicName) %></p>
          </div>
          <div class="list-item">
            <h4>Last login</h4>
            <p><%= formatDateTime(profileUser.getLastLoginAt()) %></p>
          </div>
          <div class="list-item">
            <h4>Created at</h4>
            <p><%= formatDateTime(profileUser.getCreatedAt()) %></p>
          </div>
          <div class="list-item">
            <h4>Updated at</h4>
            <p><%= formatDateTime(profileUser.getUpdatedAt()) %></p>
          </div>
        </div>
      </aside>
    </section>
  </main>

  <footer class="site-footer">
    <div class="container">CCHC Community Clinic System</div>
  </footer>
</body>
</html>