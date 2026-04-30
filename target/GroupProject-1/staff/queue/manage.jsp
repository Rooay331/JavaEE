<%@page import="ict.bean.QueueTicket"%>
<%@page import="ict.bean.User"%>
<%@page import="java.time.LocalDateTime"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Locale"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!
  private boolean isSelected(String current, String option) {
    return current != null && current.equals(option);
  }

  private String queueStatusClass(String status) {
    if (status == null) {
      return "pill-neutral";
    }

    switch (status.trim().toUpperCase()) {
      case "WAITING":
        return "status-waiting";
      case "CALLED":
        return "status-called";
      case "SKIPPED":
        return "status-skipped";
      case "SERVED":
        return "status-served";
      case "EXPIRED":
        return "status-expired";
      case "CANCELLED":
        return "status-cancelled";
      default:
        return "pill-neutral";
    }
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

  if (request.getAttribute("queueTickets") == null && request.getAttribute("queueError") == null) {
      response.sendRedirect(request.getContextPath() + "/staff/queue/manage");
      return;
  }

  List<QueueTicket> queueTickets = (List<QueueTicket>) request.getAttribute("queueTickets");
  if (queueTickets == null) {
      queueTickets = Collections.emptyList();
  }

  List<String> serviceOptions = (List<String>) request.getAttribute("serviceOptions");
  if (serviceOptions == null) {
      serviceOptions = Collections.emptyList();
  }

  String selectedService = (String) request.getAttribute("selectedService");
  String selectedStatus = (String) request.getAttribute("selectedStatus");
  String selectedPatientNameFilter = (String) request.getAttribute("selectedPatientNameFilter");
  if (selectedPatientNameFilter == null) {
      selectedPatientNameFilter = request.getParameter("patientName");
  }
  String selectedDate = (String) request.getAttribute("selectedDate");
  String queueError = (String) request.getAttribute("queueError");
  String flashMessage = (String) request.getAttribute("flashMessage");
  String flashType = (String) request.getAttribute("flashType");
  Integer totalQueueCount = (Integer) request.getAttribute("totalQueueCount");
  Integer waitingQueueCount = (Integer) request.getAttribute("waitingQueueCount");
  Integer calledQueueCount = (Integer) request.getAttribute("calledQueueCount");
  Integer servedQueueCount = (Integer) request.getAttribute("servedQueueCount");
  Integer averageWaitMinutes = (Integer) request.getAttribute("averageWaitMinutes");
  String currentServingLabel = (String) request.getAttribute("currentServingLabel");
  String nextWaitingLabel = (String) request.getAttribute("nextWaitingLabel");
  Integer assignedClinicId = (Integer) request.getAttribute("assignedClinicId");
  if (assignedClinicId == null) {
      assignedClinicId = loggedInUser.getClinicId();
  }
  Object assignedClinicName = request.getAttribute("assignedClinicName");
  LocalDateTime pageDateTime = LocalDateTime.now();
  String footerDate = pageDateTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH));
  String footerTime = pageDateTime.format(DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH));
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CCHC Staff - Queue Management</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
  <style>
    body.queue-live-page {
      background:
        radial-gradient(circle at 10% 10%, rgba(217, 237, 245, 0.95) 0%, transparent 30%),
        radial-gradient(circle at 90% 0%, rgba(226, 237, 250, 0.95) 0%, transparent 35%),
        linear-gradient(180deg, #f7fbfe 0%, #edf5fb 55%, #e9f2f8 100%);
      color: var(--text-main);
    }

    body.queue-live-page main {
      padding: 26px 0 0;
    }

    body.queue-live-page h1,
    body.queue-live-page h2,
    body.queue-live-page h3,
    body.queue-live-page h4 {
      margin-top: 0;
    }

    body.queue-live-page .site-header {
      background: rgba(247, 251, 254, 0.92);
      border-bottom: 1px solid var(--border);
      box-shadow: 0 10px 24px rgba(16, 42, 67, 0.08);
    }

    body.queue-live-page .brand,
    body.queue-live-page .nav-links a {
      color: var(--text-main);
    }

    body.queue-live-page .nav-links a:hover,
    body.queue-live-page .nav-links a.active {
      background: #d9edf5;
      color: var(--brand-dark);
    }

    body.queue-live-page .queue-live-shell {
      padding-bottom: 0;
    }

    body.queue-live-page .queue-live-header {
      text-align: center;
      margin-bottom: 24px;
    }

    body.queue-live-page .queue-live-header h1 {
      color: var(--text-main);
      font-size: clamp(2rem, 3.3vw, 3rem);
      letter-spacing: 0.02em;
      margin-bottom: 10px;
    }

    body.queue-live-page .section-subtitle {
      color: var(--text-muted);
      margin-bottom: 0;
      font-size: 1.06rem;
    }

    body.queue-live-page .queue-stage {
      display: grid;
      gap: 16px;
      margin: 0;
    }

    body.queue-live-page .queue-board {
      padding: 16px;
      border-radius: 28px;
    }

    body.queue-live-page .queue-board-grid {
      display: grid;
      grid-template-columns: minmax(0, 1.45fr) minmax(240px, 0.8fr);
      gap: 16px;
      align-items: stretch;
    }

    body.queue-live-page .queue-board-left {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    body.queue-live-page .queue-actions-panel {
      display: flex;
      flex-direction: column;
      gap: 8px;
      align-self: stretch;
      padding: 0;
    }

    body.queue-live-page .queue-actions-panel .section-title {
      margin-bottom: 0;
      text-align: left;
      font-size: 0.98rem;
    }

    body.queue-live-page .queue-hero {
      text-align: center;
      padding: 18px 16px 16px;
      border-radius: 20px;
      border: 1px solid var(--border);
      background: linear-gradient(180deg, #ffffff 0%, #f6fbfe 100%);
      box-shadow: 0 8px 18px rgba(16, 42, 67, 0.07);
    }

    body.queue-live-page .queue-label {
      margin: 0 0 12px;
      color: var(--accent);
      font-size: clamp(0.82rem, 1vw, 0.95rem);
      font-weight: 800;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      font-family: 'Montserrat', sans-serif;
    }

    body.queue-live-page .queue-number {
      margin: 0;
      color: var(--brand-dark);
      font-size: clamp(2rem, 4vw, 3rem);
      line-height: 0.96;
      font-weight: 800;
      letter-spacing: 0.05em;
      text-shadow: 0 1px 0 rgba(255, 255, 255, 0.95), 0 5px 10px rgba(31, 122, 140, 0.08);
      font-family: 'Montserrat', sans-serif;
    }

    body.queue-live-page .queue-hint {
      margin: 8px 0 0;
      color: var(--text-muted);
      font-size: 0.84rem;
      font-weight: 600;
    }

    body.queue-live-page .queue-mini-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 10px;
    }

    body.queue-live-page .queue-mini-card {
      padding: 12px 12px 10px;
      border-radius: 14px;
      border: 1px solid var(--border);
      background: rgba(255, 255, 255, 0.94);
      box-shadow: 0 5px 12px rgba(16, 42, 67, 0.05);
      text-align: center;
    }

    body.queue-live-page .queue-mini-card.next-line {
      background: #edf7f2;
    }

    body.queue-live-page .queue-mini-card.waiting {
      background: #e7f3fb;
    }

    body.queue-live-page .queue-mini-card.average {
      background: #f7f1ff;
    }

    body.queue-live-page .queue-mini-badge {
      margin-bottom: 5px;
      font-size: 0.72rem;
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      line-height: 1.1;
    }

    body.queue-live-page .queue-mini-card.next-line .queue-mini-badge {
      color: #1e7f5f;
    }

    body.queue-live-page .queue-mini-card.waiting .queue-mini-badge {
      color: var(--brand);
    }

    body.queue-live-page .queue-mini-card.average .queue-mini-badge {
      color: var(--accent);
    }

    body.queue-live-page .queue-mini-value {
      margin: 0;
      font-size: clamp(1.3rem, 2.3vw, 1.8rem);
      line-height: 1;
      font-weight: 800;
      letter-spacing: 0.04em;
      color: var(--text-main);
    }

    body.queue-live-page .queue-mini-card.next-line .queue-mini-value {
      color: #1e7f5f;
    }

    body.queue-live-page .queue-mini-card.waiting .queue-mini-value {
      color: var(--brand-dark);
    }

    body.queue-live-page .queue-mini-card.average .queue-mini-value {
      color: var(--accent);
    }

    body.queue-live-page .queue-mini-hint {
      margin-top: 3px;
      color: var(--text-muted);
      font-size: 0.72rem;
    }

    body.queue-live-page .notice,
    body.queue-live-page .card,
    body.queue-live-page .filter-form,
    body.queue-live-page .table-wrap {
      background: rgba(255, 255, 255, 0.94);
      color: var(--text-main);
      border-color: var(--border);
      box-shadow: 0 12px 26px rgba(16, 42, 67, 0.08);
      border-radius: 24px;
    }

    body.queue-live-page .notice {
      color: var(--text-main);
    }

    body.queue-live-page .section-title {
      color: var(--text-main);
    }

    body.queue-live-page .action-bar {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    body.queue-live-page .queue-actions-panel .action-bar {
      flex-direction: column;
      flex-wrap: nowrap;
      gap: 8px;
    }

    body.queue-live-page .action-bar form {
      flex: 1 1 180px;
    }

    body.queue-live-page .queue-actions-panel .action-bar form {
      flex: 0 0 auto;
      width: 100%;
    }

    body.queue-live-page .action-bar .btn {
      width: 100%;
      min-height: 38px;
      padding: 9px 12px;
      border-radius: 11px;
      font-family: 'Montserrat', sans-serif;
      font-weight: 700;
      transition: transform 180ms ease, box-shadow 180ms ease, background-color 180ms ease, border-color 180ms ease, color 180ms ease;
    }

    body.queue-live-page .action-bar .btn:hover {
      transform: translateY(-1px);
    }

    body.queue-live-page .filter-form {
      padding: 14px;
      border-radius: 16px;
      margin-bottom: 16px;
    }

    body.queue-live-page .field label {
      color: #2a4d69;
    }

    body.queue-live-page input,
    body.queue-live-page select,
    body.queue-live-page textarea {
      background: #fcfeff;
      color: var(--text-main);
      border-color: #b8cce0;
    }

    body.queue-live-page input::placeholder,
    body.queue-live-page select::placeholder,
    body.queue-live-page textarea::placeholder {
      color: var(--text-muted);
    }

    body.queue-live-page table {
      width: 100%;
      border-collapse: collapse;
      color: var(--text-main);
    }

    body.queue-live-page thead th {
      color: var(--brand-dark);
      background: #f7fbfe;
      border-bottom: 1px solid var(--border);
    }

    body.queue-live-page tbody td {
      border-bottom: 1px solid var(--border);
    }

    body.queue-live-page tbody tr:hover {
      background: #f7fbfe;
    }

    body.queue-live-page .status-chip {
      color: #425466;
      background: #edf2f7;
      border-color: #d3dbe6;
    }

    body.queue-live-page .status-chip.status-waiting {
      color: #8b5d00;
      background: #fff7e3;
      border-color: #f6d487;
    }

    body.queue-live-page .status-chip.status-called {
      color: #1f5fbf;
      background: #e6f1ff;
      border-color: #b8cff3;
    }

    body.queue-live-page .status-chip.status-skipped {
      color: #a05a00;
      background: #fef3e6;
      border-color: #f5c573;
    }

    body.queue-live-page .status-chip.status-served {
      color: #1e7f5f;
      background: #edf7f2;
      border-color: #bae7d4;
    }

    body.queue-live-page .status-chip.status-expired {
      color: #9f2b2b;
      background: #fdeeee;
      border-color: #f1b6b6;
    }

    body.queue-live-page .status-chip.status-cancelled {
      color: #6941c6;
      background: #f0e8ff;
      border-color: #d5c6f9;
    }

    body.queue-live-page .queue-footer {
      max-width: 980px;
      margin: 26px auto 0;
      padding: 0 0 28px;
    }

    body.queue-live-page .queue-footer-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      padding: 16px 20px;
      border-radius: 20px;
      background: rgba(255, 255, 255, 0.94);
      border: 1px solid var(--border);
      box-shadow: 0 10px 22px rgba(16, 42, 67, 0.08);
      color: var(--text-main);
    }

    body.queue-live-page .queue-footer-date {
      font-weight: 700;
    }

    body.queue-live-page .queue-footer-time {
      font-size: 1.2rem;
      font-weight: 800;
      color: var(--accent);
      letter-spacing: 0.08em;
    }

    body.queue-live-page .queue-footer-note {
      color: var(--text-muted);
    }

    @media (max-width: 900px) {
      body.queue-live-page .queue-board-grid {
        grid-template-columns: 1fr;
      }

      body.queue-live-page .queue-mini-grid {
        grid-template-columns: 1fr;
      }

      body.queue-live-page .queue-hero {
        padding: 28px 22px 24px;
      }

      body.queue-live-page .queue-board {
        padding: 18px;
      }

      body.queue-live-page .queue-board-left {
        gap: 18px;
      }

      body.queue-live-page .queue-actions-panel {
        gap: 14px;
        padding: 0;
      }

      body.queue-live-page .notice,
      body.queue-live-page .card,
      body.queue-live-page .filter-form,
      body.queue-live-page .table-wrap,
      body.queue-live-page .queue-footer-bar {
        border-radius: 20px;
      }
    }
  </style>
</head>
<body class="queue-live-page">
  <%@ include file="../common/nav.jspf" %>

  <main class="container queue-live-shell">

    <section class="notice" style="margin-bottom: 20px; padding: 18px 20px; border-radius: 18px;">
      Queue is scoped to <strong><%= assignedClinicName == null ? ("Clinic ID " + assignedClinicId) : assignedClinicName %></strong>. When a service filter is selected, queue actions stay within that service for the chosen date.
    </section>

    <% if (flashMessage != null && !flashMessage.trim().isEmpty()) { %>
      <% if ("error".equalsIgnoreCase(flashType)) { %>
    <section class="notice" style="margin-bottom: 18px; padding: 18px 20px; border-radius: 18px; border-color:#f1b6b6;background:#fdeeee;color:#9f2b2b;">
      <%= flashMessage %>
    </section>
      <% } else { %>
    <section class="notice" style="margin-bottom: 18px; padding: 18px 20px; border-radius: 18px; border-color:#bae7d4;background:#edf7f2;color:#1e7f5f;">
      <%= flashMessage %>
    </section>
      <% } %>
    <% } %>

    <% if (queueError != null && !queueError.trim().isEmpty()) { %>
    <section class="notice" style="margin-bottom: 18px; padding: 18px 20px; border-radius: 18px; border-color:#f1b6b6;background:#fdeeee;color:#9f2b2b;">
      <%= queueError %>
    </section>
    <% } %>

    <section class="card queue-board">
      <div class="queue-board-grid">
        <div class="queue-board-left">
          <section class="queue-stage">
            <div class="queue-hero">
              <h2 class="queue-label">Now Serving</h2>
              <p class="queue-number"><%= currentServingLabel == null ? "None" : currentServingLabel %></p>
              <p class="queue-hint">Please proceed to counter</p>
            </div>

            <div class="queue-mini-grid">
              <article class="queue-mini-card next-line">
                <div class="queue-mini-badge">Next in line</div>
                <p class="queue-mini-value"><%= nextWaitingLabel == null ? "None" : nextWaitingLabel %></p>
                <div class="queue-mini-hint">Ready to be called</div>
              </article>

              <article class="queue-mini-card waiting">
                <div class="queue-mini-badge">Waiting</div>
                <p class="queue-mini-value"><%= waitingQueueCount == null ? 0 : waitingQueueCount %></p>
                <div class="queue-mini-hint">customers in queue</div>
              </article>

              <article class="queue-mini-card average">
                <div class="queue-mini-badge">Average wait</div>
                <p class="queue-mini-value"><%= averageWaitMinutes == null ? 0 : averageWaitMinutes %> min</p>
                <div class="queue-mini-hint">estimated time</div>
              </article>
            </div>
          </section>
        </div>

        <aside class="queue-actions-panel">
          <h2 class="section-title">Queue actions</h2>
          <div class="action-bar">
            <form action="<%= request.getContextPath() %>/staff/queue/manage" method="post">
              <input type="hidden" name="queueAction" value="CALL_NEXT">
              <input type="hidden" name="patientName" value="<%= selectedPatientNameFilter == null ? "" : selectedPatientNameFilter %>">
              <input type="hidden" name="service" value="<%= selectedService == null ? "" : selectedService %>">
              <input type="hidden" name="status" value="<%= selectedStatus == null ? "" : selectedStatus %>">
              <input type="hidden" name="date" value="<%= selectedDate == null ? "" : selectedDate %>">
              <button class="btn btn-primary" type="submit">Call Next</button>
            </form>
            <form action="<%= request.getContextPath() %>/staff/queue/manage" method="post">
              <input type="hidden" name="queueAction" value="SKIP_CURRENT">
              <input type="hidden" name="patientName" value="<%= selectedPatientNameFilter == null ? "" : selectedPatientNameFilter %>">
              <input type="hidden" name="service" value="<%= selectedService == null ? "" : selectedService %>">
              <input type="hidden" name="status" value="<%= selectedStatus == null ? "" : selectedStatus %>">
              <input type="hidden" name="date" value="<%= selectedDate == null ? "" : selectedDate %>">
              <button class="btn btn-warning" type="submit">Skip Current</button>
            </form>
            <form action="<%= request.getContextPath() %>/staff/queue/manage" method="post">
              <input type="hidden" name="queueAction" value="EXPIRE_CURRENT">
              <input type="hidden" name="patientName" value="<%= selectedPatientNameFilter == null ? "" : selectedPatientNameFilter %>">
              <input type="hidden" name="service" value="<%= selectedService == null ? "" : selectedService %>">
              <input type="hidden" name="status" value="<%= selectedStatus == null ? "" : selectedStatus %>">
              <input type="hidden" name="date" value="<%= selectedDate == null ? "" : selectedDate %>">
              <button class="btn btn-warning" type="submit">Expire Current</button>
            </form>
            <form action="<%= request.getContextPath() %>/staff/queue/manage" method="post">
              <input type="hidden" name="queueAction" value="MARK_SERVED">
              <input type="hidden" name="patientName" value="<%= selectedPatientNameFilter == null ? "" : selectedPatientNameFilter %>">
              <input type="hidden" name="service" value="<%= selectedService == null ? "" : selectedService %>">
              <input type="hidden" name="status" value="<%= selectedStatus == null ? "" : selectedStatus %>">
              <input type="hidden" name="date" value="<%= selectedDate == null ? "" : selectedDate %>">
              <button class="btn btn-secondary" type="submit">Mark Served</button>
            </form>
            <form action="<%= request.getContextPath() %>/staff/queue/manage" method="get">
              <input type="hidden" name="patientName" value="<%= selectedPatientNameFilter == null ? "" : selectedPatientNameFilter %>">
              <input type="hidden" name="service" value="<%= selectedService == null ? "" : selectedService %>">
              <input type="hidden" name="status" value="<%= selectedStatus == null ? "" : selectedStatus %>">
              <input type="hidden" name="date" value="<%= selectedDate == null ? "" : selectedDate %>">
              <button class="btn btn-secondary" type="submit">Refresh</button>
            </form>
          </div>
        </aside>
      </div>
    </section>

    <section class="card">
      <h2 class="section-title">Filter queue</h2>
      <form class="filter-form" id="queueFilterForm" action="<%= request.getContextPath() %>/staff/queue/manage" method="get">
        <div class="field">
          <label for="patientName">Patient name</label>
          <input id="patientName" name="patientName" type="search" value="<%= selectedPatientNameFilter == null ? "" : selectedPatientNameFilter %>" placeholder="Search patient name">
        </div>
        <div class="field">
          <label for="service">Service</label>
          <select id="service" name="service">
            <option value="" <%= selectedService == null ? "selected" : "" %>>All services</option>
            <% for (String serviceName : serviceOptions) { %>
            <option value="<%= serviceName %>" <%= isSelected(selectedService, serviceName) ? "selected" : "" %>><%= serviceName %></option>
            <% } %>
          </select>
        </div>
        <div class="field">
          <label for="status">Status</label>
          <select id="status" name="status">
            <option value="" <%= selectedStatus == null ? "selected" : "" %>>Any status</option>
            <option value="WAITING" <%= isSelected(selectedStatus, "WAITING") ? "selected" : "" %>>WAITING</option>
            <option value="CALLED" <%= isSelected(selectedStatus, "CALLED") ? "selected" : "" %>>CALLED</option>
            <option value="SKIPPED" <%= isSelected(selectedStatus, "SKIPPED") ? "selected" : "" %>>SKIPPED</option>
            <option value="SERVED" <%= isSelected(selectedStatus, "SERVED") ? "selected" : "" %>>SERVED</option>
          </select>
        </div>
        <div class="field">
          <label for="date">Queue date</label>
          <input id="date" name="date" type="date" value="<%= selectedDate == null ? "" : selectedDate %>">
        </div>
        <div class="filter-actions">
          <button class="btn btn-primary" type="submit">Apply</button>
          <button class="btn btn-secondary" type="button" id="queueFilterReset">Reset</button>
        </div>
      </form>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Ticket</th>
              <th>Patient</th>
              <th>Service</th>
              <th>Queue Date</th>
              <th>Issued</th>
              <th>Wait</th>
              <th>Status</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            <% if (queueTickets.isEmpty()) { %>
            <tr>
              <td colspan="8">No queue tickets match the current filter.</td>
            </tr>
            <% } else {
                for (QueueTicket ticket : queueTickets) {
            %>
            <tr>
              <td><%= ticket.getDisplayCode() %></td>
              <td><%= ticket.getPatientName() %></td>
              <td><%= ticket.getClinicServiceLabel() %></td>
              <td><%= ticket.getQueueLabel() %></td>
              <td><%= ticket.getIssuedAtLabel() %></td>
              <td><%= ticket.getEstimatedWaitLabel() %></td>
              <td><span class="status-chip <%= queueStatusClass(ticket.getStatus()) %>"><%= ticket.getStatus() %></span></td>
              <td><%= ticket.getStateTimestampLabel() == null || ticket.getStateTimestampLabel().isEmpty() ? ticket.getIssuedAtLabel() : ticket.getStateTimestampLabel() %></td>
            </tr>
            <%      }
               } %>
          </tbody>
        </table>
      </div>
    </section>
  </main>

  <footer class="queue-footer">
    <div class="queue-footer-bar">
      <div class="queue-footer-date" id="queueFooterDate"><%= footerDate %></div>
      <div class="queue-footer-time" id="queueFooterTime"><%= footerTime %></div>
      <div class="queue-footer-note">CCHC Community Clinic System</div>
    </div>
  </footer>

  <script>
    (function () {
      var filterForm = document.getElementById('queueFilterForm');
      var filterResetButton = document.getElementById('queueFilterReset');
      var dateElement = document.getElementById('queueFooterDate');
      var timeElement = document.getElementById('queueFooterTime');

      if (filterForm && filterResetButton) {
        filterResetButton.addEventListener('click', function () {
          window.location.href = filterForm.action;
        });
      }

      if (!dateElement || !timeElement) {
        return;
      }

      var dateFormatter = new Intl.DateTimeFormat('en-US', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });

      var timeFormatter = new Intl.DateTimeFormat('en-US', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true
      });

      var updateClock = function () {
        var now = new Date();
        dateElement.textContent = dateFormatter.format(now);
        timeElement.textContent = timeFormatter.format(now);
      };

      updateClock();
      window.setInterval(updateClock, 1000);
    })();
  </script>
</body>
</html>
