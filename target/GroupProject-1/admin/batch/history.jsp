<%@page import="ict.bean.BatchImportResult"%>
<%@page import="ict.bean.ClinicService"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/admin/common/util.jspf" %>
<%
    Object sessionUser = session.getAttribute("userInfo");
    if (!(sessionUser instanceof ict.bean.User) || !"ADMIN".equalsIgnoreCase(((ict.bean.User) sessionUser).getRole())) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    List<ClinicService> activeClinics = (List<ClinicService>) request.getAttribute("activeClinics");
    if (activeClinics == null) {
        activeClinics = Collections.emptyList();
    }

    List<BatchImportResult> batchImportHistory = (List<BatchImportResult>) request.getAttribute("batchImportHistory");
    if (batchImportHistory == null) {
        batchImportHistory = Collections.emptyList();
    }

    int historySuccessCount = 0;
    int historyFailureCount = 0;
    for (BatchImportResult result : batchImportHistory) {
      if (result != null && result.isSuccess()) {
        historySuccessCount++;
      } else {
        historyFailureCount++;
      }
    }
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Admin - Batch History</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
  <%@ include file="/admin/common/nav.jspf" %>

  <main class="container">
    <section>
      <h1>Batch History</h1>
      <p class="section-subtitle">Review the outcome of the most recent upload session.</p>
    </section>

    <nav class="sub-nav" aria-label="Batch import navigation">
      <a href="<%= request.getContextPath() %>/admin/batch/import">Import</a>
      <a class="active" href="<%= request.getContextPath() %>/admin/batch/history">History</a>
    </nav>

    <section class="summary-strip">
      <article class="summary-box"><h4><%= batchImportHistory.size() %></h4><p>Stored rows</p></article>
      <article class="summary-box"><h4><%= activeClinics.size() %></h4><p>Active clinics</p></article>
      <article class="summary-box"><h4><%= historySuccessCount %></h4><p>Successful rows</p></article>
      <article class="summary-box"><h4><%= historyFailureCount %></h4><p>Failed rows</p></article>
    </section>

    <section class="card">
      <h2 class="section-title">Last upload session</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Row</th>
              <th>Type</th>
              <th>Identifier</th>
              <th>Action</th>
              <th>Status</th>
              <th>Message</th>
            </tr>
          </thead>
          <tbody>
            <% if (batchImportHistory.isEmpty()) { %>
            <tr><td colspan="6">There is no batch history yet.</td></tr>
            <% } else {
                 for (BatchImportResult result : batchImportHistory) {
            %>
            <tr>
              <td><%= result.getRowLabel() %></td>
              <td><%= safeText(result.getEntityType()) %></td>
              <td><%= safeText(result.getIdentifier()) %></td>
              <td><%= safeText(result.getAction()) %></td>
              <td><span class="status-chip <%= result.getStatusChipClass() %>"><%= result.getStatusLabel() %></span></td>
              <td><%= safeText(result.getMessage()) %></td>
            </tr>
            <%   }
               } %>
          </tbody>
        </table>
      </div>
    </section>
  </main>

  <footer class="site-footer"><div class="container">Batch history sample page.</div></footer>
</body>
</html>