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

    if (request.getAttribute("users") == null && request.getAttribute("totalUsers") == null) {
        response.sendRedirect(request.getContextPath() + "/admin/users/list");
        return;
    }

    List<User> users = (List<User>) request.getAttribute("users");
    if (users == null) {
        users = Collections.emptyList();
    }

    List<ClinicService> activeClinics = (List<ClinicService>) request.getAttribute("activeClinics");
    if (activeClinics == null) {
        activeClinics = Collections.emptyList();
    }

    Map<Integer, String> clinicNamesById = (Map<Integer, String>) request.getAttribute("clinicNamesById");

    String selectedKeyword = (String) request.getAttribute("selectedKeyword");
    String selectedRole = (String) request.getAttribute("selectedRole");
    String selectedStatus = (String) request.getAttribute("selectedStatus");

    Integer currentPage = (Integer) request.getAttribute("currentPage");
    if (currentPage == null || currentPage < 1) {
        currentPage = 1;
    }

    Integer totalPages = (Integer) request.getAttribute("totalPages");
    if (totalPages == null) {
        totalPages = 0;
    }

    Integer totalUsers = (Integer) request.getAttribute("totalUsers");
    if (totalUsers == null) {
        totalUsers = 0;
    }

    String queryKeyword = selectedKeyword == null ? "" : urlEncode(selectedKeyword);
    String queryRole = selectedRole == null ? "" : urlEncode(selectedRole);
    String queryStatus = selectedStatus == null ? "" : urlEncode(selectedStatus);
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Admin - User Management</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="/admin/common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>User Management</h1>
      <p class="section-subtitle">Search, page through, and maintain patient, staff, and admin accounts.</p>
    </section>

    <section class="action-bar">
      <a class="btn btn-primary" href="<%= request.getContextPath() %>/admin/users/create">Create New User</a>
      <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/dashboard">Dashboard</a>
    </section>

    <section class="card">
      <h2 class="section-title">Search users</h2>
      <form class="filter-form" action="<%= request.getContextPath() %>/admin/users/list" method="get">
        <div class="field">
          <label for="q">Keyword</label>
          <input id="q" name="q" type="search" value="<%= selectedKeyword == null ? "" : escapeHtml(selectedKeyword) %>" placeholder="Name, email, or phone">
        </div>
        <div class="field">
          <label for="role">Role</label>
          <select id="role" name="role">
            <option value="" <%= selectedRole == null || selectedRole.isEmpty() ? "selected" : "" %>>All roles</option>
            <option value="PATIENT" <%= "PATIENT".equalsIgnoreCase(selectedRole) ? "selected" : "" %>>PATIENT</option>
            <option value="STAFF" <%= "STAFF".equalsIgnoreCase(selectedRole) ? "selected" : "" %>>STAFF</option>
            <option value="ADMIN" <%= "ADMIN".equalsIgnoreCase(selectedRole) ? "selected" : "" %>>ADMIN</option>
          </select>
        </div>
        <div class="field">
          <label for="status">Status</label>
          <select id="status" name="status">
            <option value="" <%= selectedStatus == null || selectedStatus.isEmpty() ? "selected" : "" %>>All statuses</option>
            <option value="ACTIVE" <%= "ACTIVE".equalsIgnoreCase(selectedStatus) ? "selected" : "" %>>ACTIVE</option>
            <option value="SUSPENDED" <%= "SUSPENDED".equalsIgnoreCase(selectedStatus) ? "selected" : "" %>>SUSPENDED</option>
          </select>
        </div>
        <div class="filter-actions">
          <button class="btn btn-primary" type="submit">Search</button>
          <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/users/list">Reset</a>
        </div>
      </form>
      <p class="muted">Showing <%= users.size() %> of <%= totalUsers %> users.</p>
    </section>

    <section class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>User</th>
            <th>Name</th>
            <th>Role</th>
            <th>Clinic</th>
            <th>Email</th>
            <th>Status</th>
            <th>Last Login</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <% if (users.isEmpty()) { %>
          <tr>
            <td colspan="8">No users matched the current search.</td>
          </tr>
          <% } else {
               for (User user : users) {
          %>
          <tr>
            <td><%= userCode(user.getUserId()) %></td>
            <td><%= safeText(user.getFullName()) %></td>
            <td><span class="status-chip <%= roleChipClass(user.getRole()) %>"><%= safeText(user.getRole()) %></span></td>
            <td><%= clinicNameById(user.getClinicId(), clinicNamesById) %></td>
            <td><%= safeText(user.getEmail()) %></td>
            <td><span class="status-chip <%= userStatusClass(user.getIsActive()) %>"><%= user.getIsActive() == 1 ? "ACTIVE" : "SUSPENDED" %></span></td>
            <td><%= formatDateTime(user.getLastLoginAt()) %></td>
            <td>
              <div class="actions">
                <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/users/edit?userId=<%= user.getUserId() %>">Edit</a>
                <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/users/edit?userId=<%= user.getUserId() %>#password-reset">Reset Password</a>
                <form action="<%= request.getContextPath() %>/admin/users/delete" method="post" style="display:inline;">
                  <input type="hidden" name="userId" value="<%= user.getUserId() %>">
                  <button class="btn btn-warning" type="submit" onclick="return confirm('Deactivate this user?');">Deactivate</button>
                </form>
              </div>
            </td>
          </tr>
          <%     }
             } %>
        </tbody>
      </table>
    </section>

    <% if (totalPages > 1) { %>
    <section class="action-bar" style="margin-top: 16px;">
      <% if (currentPage > 1) { %>
      <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/users/list?page=<%= currentPage - 1 %>&q=<%= queryKeyword %>&role=<%= queryRole %>&status=<%= queryStatus %>">Previous</a>
      <% } %>

      <% for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) { %>
      <a class="btn <%= pageNumber == currentPage ? "btn-primary" : "btn-secondary" %>" href="<%= request.getContextPath() %>/admin/users/list?page=<%= pageNumber %>&q=<%= queryKeyword %>&role=<%= queryRole %>&status=<%= queryStatus %>"><%= pageNumber %></a>
      <% } %>

      <% if (currentPage < totalPages) { %>
      <a class="btn btn-secondary" href="<%= request.getContextPath() %>/admin/users/list?page=<%= currentPage + 1 %>&q=<%= queryKeyword %>&role=<%= queryRole %>&status=<%= queryStatus %>">Next</a>
      <% } %>
    </section>
    <% } %>
  </main>

  <footer class="site-footer"><div class="container">User administration sample page.</div></footer>
</body>
</html>