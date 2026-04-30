<%@page import="ict.bean.Notification" %>
<%@page import="ict.bean.User" %>
<%@page import="java.util.Collections" %>
<%@page import="java.util.List" %>
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

  if (request.getAttribute("notifications") == null && request.getAttribute("notificationsError") == null) {
      response.sendRedirect(request.getContextPath() + "/staff/notifications");
      return;
  }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Staff Notifications</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Notifications <span class="badge-pill"><%= request.getAttribute("unreadNotificationCount") == null ? "0" : request.getAttribute("unreadNotificationCount") %></span></h1>
      <p class="section-subtitle">Clinic-specific operational alerts, approval reminders, and service updates for <strong><%= request.getAttribute("assignedClinicName") == null ? ("Clinic ID " + loggedInUser.getClinicId()) : request.getAttribute("assignedClinicName") %></strong>.</p>
    </section>

    <%
      String notificationsError = (String) request.getAttribute("notificationsError");
      List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
      if (notifications == null) {
          notifications = Collections.emptyList();
      }
      Integer notificationCount = (Integer) request.getAttribute("notificationCount");
      if (notificationCount == null) {
          notificationCount = notifications.size();
      }
      Integer unreadNotificationCount = (Integer) request.getAttribute("unreadNotificationCount");
      if (unreadNotificationCount == null) {
          unreadNotificationCount = 0;
      }
      Integer readNotificationCount = (Integer) request.getAttribute("readNotificationCount");
      if (readNotificationCount == null) {
          readNotificationCount = notificationCount - unreadNotificationCount;
      }
      Integer assignedClinicId = (Integer) request.getAttribute("assignedClinicId");
      if (assignedClinicId == null) {
          assignedClinicId = loggedInUser.getClinicId();
      }
      Object assignedClinicName = request.getAttribute("assignedClinicName");
    %>

    <% if (notificationsError != null && !notificationsError.trim().isEmpty()) { %>
    <section class="notice" style="margin-bottom: 16px;">
      <%= notificationsError %>
    </section>
    <% } %>

    <section class="summary-strip">
      <div class="summary-box">
        <h4><%= notificationCount %></h4>
        <p>Total notifications</p>
      </div>
      <div class="summary-box">
        <h4><%= unreadNotificationCount %></h4>
        <p>Unread</p>
      </div>
      <div class="summary-box">
        <h4><%= readNotificationCount %></h4>
        <p>Read</p>
      </div>
      <div class="summary-box">
        <h4><%= assignedClinicId == null ? "-" : assignedClinicId %></h4>
        <p>Clinic ID</p>
      </div>
      <div class="summary-box">
        <h4><%= assignedClinicName == null ? "Assigned clinic" : assignedClinicName %></h4>
        <p>Visibility scope</p>
      </div>
    </section>

    <% if (unreadNotificationCount > 0) { %>
    <div class="action-bar" style="margin-bottom: 16px;">
      <form action="<%= request.getContextPath() %>/staff/notifications" method="post">
        <input type="hidden" name="notificationAction" value="MARK_ALL_READ">
        <button class="btn btn-primary" type="submit">Mark All Read</button>
      </form>
    </div>
    <% } %>

    <section class="list-group">
      <% if (notifications.isEmpty()) { %>
      <article class="list-item">
        <h4>No clinic notifications yet</h4>
        <p>When the assigned clinic receives approvals, queue alerts, or service updates, they will appear here.</p>
      </article>
      <% } else {
          for (Notification notification : notifications) {
      %>
      <article class="list-item" style="border-left: 4px solid <%= notification.isRead() ? "#d3dbe6" : "#7bb4cf" %>;">
        <div class="action-bar" style="justify-content: space-between; align-items: center; margin-bottom: 10px;">
          <span class="status-chip <%= notification.getTypeBadgeClass() %>"><%= notification.getTypeLabel() %></span>
          <span class="status-chip <%= notification.getReadStateClass() %>"><%= notification.getReadStateLabel() %></span>
        </div>
        <h4><%= notification.getTitle() %></h4>
        <p style="margin-bottom: 10px;"><%= notification.getBody() %></p>
        <div class="sub-nav" style="margin-bottom: 0;">
          <span class="tag"><%= notification.getDisplayCode() %></span>
          <span class="tag">Created <%= notification.getCreatedAtLabel() %></span>
          <% if (notification.getReadAtLabel() != null && !notification.getReadAtLabel().isEmpty()) { %>
          <span class="tag">Read <%= notification.getReadAtLabel() %></span>
          <% } %>
          <% if (notification.getRelatedContextLabel() != null && !notification.getRelatedContextLabel().isEmpty()) { %>
          <span class="tag"><%= notification.getRelatedContextLabel() %></span>
          <% } %>
        </div>
        <div class="action-bar" style="margin-top: 12px;">
          <% if (!notification.isRead()) { %>
          <form action="<%= request.getContextPath() %>/staff/notifications" method="post">
            <input type="hidden" name="notificationAction" value="MARK_READ">
            <input type="hidden" name="notificationId" value="<%= notification.getNotificationId() %>">
            <button class="btn btn-primary" type="submit">Mark as Read</button>
          </form>
          <% } %>
          <form action="<%= request.getContextPath() %>/staff/notifications" method="post" onsubmit="return confirm('Delete this notification?');">
            <input type="hidden" name="notificationAction" value="DELETE">
            <input type="hidden" name="notificationId" value="<%= notification.getNotificationId() %>">
            <button class="btn btn-secondary" type="submit">Delete</button>
          </form>
        </div>
      </article>
      <%    }
         } %>
    </section>
  </main>

  <footer class="site-footer">
    <div class="container">CCHC Community Clinic System</div>
  </footer>
</body>
</html>