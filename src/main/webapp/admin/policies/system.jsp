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

    Integer maxBookingsPerPatient = (Integer) request.getAttribute("maxBookingsPerPatient");
    if (maxBookingsPerPatient == null) {
        maxBookingsPerPatient = 3;
    }

    Integer maxBookingsPerSlot = (Integer) request.getAttribute("maxBookingsPerSlot");
    if (maxBookingsPerSlot == null) {
      maxBookingsPerSlot = 1;
    }

    Integer cancellationCutoffHours = (Integer) request.getAttribute("cancellationCutoffHours");
    if (cancellationCutoffHours == null) {
        cancellationCutoffHours = 2;
    }

    Boolean queueEnabled = (Boolean) request.getAttribute("queueEnabled");
    if (queueEnabled == null) {
        queueEnabled = Boolean.TRUE;
    }

%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Admin - System Policies</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="/admin/common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>System Policies</h1>
      <p class="section-subtitle">Set booking and queue defaults used across the application.</p>
    </section>

    <nav class="sub-nav" aria-label="Policy navigation">
      <a class="active" href="<%= request.getContextPath() %>/admin/policies/system">System Policies</a>
    </nav>

    <section class="card">
      <form class="form-grid" action="<%= request.getContextPath() %>/admin/policies/system" method="post">
        <div class="field">
          <label for="maxBookingsPerPatient">Max bookings per user</label>
          <input id="maxBookingsPerPatient" name="maxBookingsPerPatient" type="number" min="1" value="<%= maxBookingsPerPatient %>">
        </div>
        <div class="field">
          <label for="maxBookingsPerSlot">Max bookings per slot</label>
          <input id="maxBookingsPerSlot" name="maxBookingsPerSlot" type="number" min="1" value="<%= maxBookingsPerSlot %>">
        </div>
        <div class="field">
          <label for="cancellationCutoffHours">Cancellation cutoff (hours)</label>
          <input id="cancellationCutoffHours" name="cancellationCutoffHours" type="number" min="0" value="<%= cancellationCutoffHours %>">
        </div>
        <div class="field">
          <label for="queueEnabled">Queue enabled</label>
          <div class="inline-row" style="padding: 10px 12px; border: 1px solid #b8cce0; border-radius: 10px; background: #fcfeff;">
            <input id="queueEnabled" name="queueEnabled" type="checkbox" value="1" <%= queueEnabled.booleanValue() ? "checked" : "" %>>
            <span>Allow same-day queue ticket creation</span>
          </div>
        </div>
        <div class="field field-full form-actions">
          <button class="btn btn-primary" type="submit">Save System Policies</button>
        </div>
      </form>
    </section>
  </main>

  <footer class="site-footer"><div class="container">System policy configuration sample page.</div></footer>
</body>
</html>