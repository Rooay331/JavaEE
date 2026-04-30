<%@page import="ict.bean.ClinicService"%>
<%@page import="ict.bean.ClinicServiceStatus"%>
<%@page import="ict.bean.QueueTicket"%>
<%@page import="ict.bean.User"%>
<%@page import="java.time.LocalDateTime"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d-M-yyyy H:mm");

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

	private String safeText(String value) {
		return value == null || value.trim().isEmpty() ? "-" : escapeHtml(value);
	}

	private String formatDateTime(LocalDateTime value) {
		if (value == null) {
			return "-";
		}
		return DATE_TIME_FORMATTER.format(value);
	}

	private String selected(Integer current, Integer option) {
		if (current == null || option == null) {
			return "";
		}
		return current.equals(option) ? "selected" : "";
	}

	private String highlightedRow(Integer highlightedTicketId, QueueTicket ticket) {
		if (highlightedTicketId == null || ticket == null || ticket.getTicketId() == null) {
			return "";
		}
		return highlightedTicketId.equals(ticket.getTicketId()) ? "queue-row-highlighted" : "";
	}
%>
<%
	User loggedInUser = null;
	Object sessionUser = session.getAttribute("userInfo");
	if (sessionUser instanceof User) {
		loggedInUser = (User) sessionUser;
	}

	if (loggedInUser == null || !"PATIENT".equalsIgnoreCase(loggedInUser.getRole())) {
		response.sendRedirect(request.getContextPath() + "/login");
		return;
	}

	if (request.getAttribute("availableClinics") == null
			&& request.getAttribute("queueMessage") == null
			&& request.getAttribute("queueError") == null) {
		response.sendRedirect(request.getContextPath() + "/patient/queue/join");
		return;
	}

	User patientUser = (User) request.getAttribute("patientUser");
	if (patientUser == null) {
		patientUser = loggedInUser;
	}

	List<ClinicService> availableClinics = (List<ClinicService>) request.getAttribute("availableClinics");
	if (availableClinics == null) {
		availableClinics = Collections.emptyList();
	}

	List<ClinicServiceStatus> availableServices = (List<ClinicServiceStatus>) request.getAttribute("availableServices");
	if (availableServices == null) {
		availableServices = Collections.emptyList();
	}

	List<ClinicServiceStatus> allWalkInServices = (List<ClinicServiceStatus>) request.getAttribute("allWalkInServices");
	if (allWalkInServices == null) {
		allWalkInServices = Collections.emptyList();
	}

	List<QueueTicket> patientQueueTickets = (List<QueueTicket>) request.getAttribute("patientQueueTickets");
	if (patientQueueTickets == null) {
		patientQueueTickets = Collections.emptyList();
	}

	QueueTicket activeQueueTicket = (QueueTicket) request.getAttribute("activeQueueTicket");

	Integer selectedClinicId = (Integer) request.getAttribute("selectedClinicId");
	Integer selectedServiceId = (Integer) request.getAttribute("selectedServiceId");
	Integer highlightedTicketId = (Integer) request.getAttribute("highlightedTicketId");

	String queueMessage = (String) request.getAttribute("queueMessage");
	String queueMessageType = (String) request.getAttribute("queueMessageType");
	String queueError = (String) request.getAttribute("queueError");
	String contactPhone = (String) request.getAttribute("contactPhone");
	if (contactPhone == null) {
		contactPhone = "";
	}

	String formAction = request.getContextPath() + "/patient/queue/join";
%>
<!doctype html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<title>CCHC Patient - Join Queue</title>
	<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
	<style>
		body.join-queue-page {
			background:
				radial-gradient(circle at 12% 8%, rgba(195, 229, 245, 0.95) 0%, transparent 32%),
				radial-gradient(circle at 90% 0%, rgba(227, 236, 252, 0.92) 0%, transparent 34%),
				linear-gradient(180deg, #f6fbff 0%, #ebf4fb 58%, #e3eff9 100%);
			color: var(--text-main);
		}

		body.join-queue-page main {
			padding-top: 26px;
		}

		body.join-queue-page .queue-shell {
			display: grid;
			gap: 16px;
		}

		body.join-queue-page .queue-intro {
			background: rgba(255, 255, 255, 0.95);
			border: 1px solid var(--border);
			border-radius: 20px;
			box-shadow: 0 12px 24px rgba(16, 42, 67, 0.08);
			padding: 18px 20px;
		}

		body.join-queue-page .queue-intro h1 {
			margin-top: 0;
			margin-bottom: 8px;
			font-size: clamp(1.7rem, 3.2vw, 2.4rem);
			color: var(--brand-dark);
		}

		body.join-queue-page .queue-intro p {
			margin: 0;
			color: var(--text-muted);
		}

		body.join-queue-page .queue-grid {
			display: grid;
			grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.9fr);
			gap: 16px;
			align-items: start;
		}

		body.join-queue-page .queue-panel,
		body.join-queue-page .queue-summary {
			background: rgba(255, 255, 255, 0.95);
			border: 1px solid var(--border);
			border-radius: 20px;
			box-shadow: 0 12px 24px rgba(16, 42, 67, 0.08);
			padding: 18px;
		}

		body.join-queue-page .queue-panel .section-title,
		body.join-queue-page .queue-summary .section-title {
			margin-top: 0;
			margin-bottom: 12px;
		}

		body.join-queue-page .queue-form {
			display: grid;
			gap: 12px;
		}

		body.join-queue-page .queue-form .field {
			gap: 6px;
		}

		body.join-queue-page .queue-form .form-actions {
			margin-top: 4px;
		}

		body.join-queue-page .queue-tip {
			margin: 0;
			font-size: 0.92rem;
			color: var(--text-muted);
		}

		body.join-queue-page .queue-notice {
			margin-bottom: 0;
			padding: 12px 14px;
			border-radius: 12px;
			border: 1px solid #bcdcef;
			background: #f2f9fd;
			color: #1f4f6a;
		}

		body.join-queue-page .queue-notice-success {
			border-color: #bae7d4;
			background: #edf7f2;
			color: #1e7f5f;
		}

		body.join-queue-page .queue-notice-error {
			border-color: #f1b6b6;
			background: #fdeeee;
			color: #9f2b2b;
		}

		body.join-queue-page .queue-kpi {
			display: grid;
			gap: 10px;
		}

		body.join-queue-page .queue-kpi-card {
			border: 1px solid var(--border);
			border-radius: 14px;
			padding: 14px;
			background: linear-gradient(180deg, #ffffff 0%, #f7fbfe 100%);
		}

		body.join-queue-page .queue-kpi-card h3 {
			margin: 0;
			color: var(--brand-dark);
			font-size: 1.45rem;
		}

		body.join-queue-page .queue-kpi-card p {
			margin: 4px 0 0;
			color: var(--text-muted);
		}

		body.join-queue-page .queue-table-card {
			background: rgba(255, 255, 255, 0.95);
			border: 1px solid var(--border);
			border-radius: 20px;
			box-shadow: 0 12px 24px rgba(16, 42, 67, 0.08);
			padding: 18px;
		}

		body.join-queue-page .queue-table-card .section-title {
			margin-top: 0;
			margin-bottom: 10px;
		}

		body.join-queue-page .queue-table-card .section-subtitle {
			margin-top: 0;
			margin-bottom: 14px;
		}

		body.join-queue-page .queue-row-highlighted {
			background: #edf7f2;
		}

		body.join-queue-page .table-wrap {
			border-radius: 14px;
		}

		@media (max-width: 980px) {
			body.join-queue-page .queue-grid {
				grid-template-columns: 1fr;
			}
		}
	</style>
</head>
<body class="join-queue-page">
	<%@ include file="../common/nav.jspf" %>

	<main class="container queue-shell">
		<section class="queue-intro">
			<h1>Join Walk-In Queue</h1>
			<p>Select clinic and clinic-specific service, then confirm to create a WAITING ticket for staff queue management.</p>
		</section>

		<% if (queueMessage != null && !queueMessage.trim().isEmpty()) { %>
		<section class="queue-notice <%= "success".equalsIgnoreCase(queueMessageType) ? "queue-notice-success" : "" %>">
			<%= escapeHtml(queueMessage) %>
		</section>
		<% } %>

		<% if (queueError != null && !queueError.trim().isEmpty()) { %>
		<section class="queue-notice queue-notice-error">
			<%= escapeHtml(queueError) %>
		</section>
		<% } %>

		<section class="queue-grid">
			<section class="queue-panel">
				<h2 class="section-title">Queue entry form</h2>
				<form class="queue-form" id="joinQueueForm" action="<%= formAction %>" method="post">
					<div class="field">
						<label for="clinicId">Clinic</label>
						<select id="clinicId" name="clinicId" required>
							<option value="">Select clinic</option>
							<% for (ClinicService clinic : availableClinics) { %>
							<option value="<%= clinic.getClinicId() %>" <%= selected(selectedClinicId, clinic.getClinicId()) %>><%= escapeHtml(clinic.getClinicName()) %></option>
							<% } %>
						</select>
					</div>

					<div class="field">
						<label for="serviceId">Service (scoped by clinic)</label>
						<select id="serviceId" name="serviceId" <%= selectedClinicId == null ? "disabled" : "" %> required>
							<option value="">Select service</option>
							<% for (ClinicServiceStatus service : allWalkInServices) {
								   Integer clinicIdForService = service.getClinicId();
								   boolean visible = selectedClinicId != null && selectedClinicId.equals(clinicIdForService);
								   String serviceLabel = service.getServiceName();
								   if (service.getAvgServiceMinutes() > 0) {
									   serviceLabel += " (avg " + service.getAvgServiceMinutes() + " mins)";
								   }
							%>
							<option value="<%= service.getServiceId() %>"
									data-clinic-id="<%= clinicIdForService == null ? "" : clinicIdForService %>"
									<%= selected(selectedServiceId, service.getServiceId()) %>
									<%= visible ? "" : "hidden" %>>
								<%= escapeHtml(serviceLabel) %>
							</option>
							<% } %>
						</select>
					</div>

					<div class="field">
						<label for="contactPhone">Contact Phone (optional update)</label>
						<input id="contactPhone" name="contactPhone" type="tel" value="<%= escapeHtml(contactPhone) %>" placeholder="e.g. +852 9123 4567">
					</div>

					<div class="form-actions">
						<button class="btn btn-primary" type="submit">Confirm Queue Entry</button>
						<button class="btn btn-secondary" id="queueFormReset" type="button">Reset</button>
					</div>
					<p class="queue-tip">Queue date is set automatically by the system. Missing fields will show validation messages and your entered values stay on screen.</p>
				</form>
			</section>

			<aside class="queue-summary">
				<h2 class="section-title">Current status</h2>
				<div class="queue-kpi">
					<article class="queue-kpi-card">
						<h3><%= escapeHtml(patientUser.getFullName()) %></h3>
						<p>Patient account</p>
					</article>

					<% if (activeQueueTicket == null) { %>
					<article class="queue-kpi-card">
						<h3>None</h3>
						<p>No active queue ticket right now.</p>
					</article>
					<% } else { %>
					<article class="queue-kpi-card">
						<h3><%= escapeHtml(activeQueueTicket.getDisplayCode()) %></h3>
						<p><%= escapeHtml(activeQueueTicket.getClinicServiceLabel()) %></p>
						<p><span class="status-chip <%= activeQueueTicket.getStatusChipClass() %>"><%= escapeHtml(activeQueueTicket.getStatus()) %></span></p>
					</article>
					<% } %>

					<article class="queue-kpi-card">
						<h3><%= patientQueueTickets.size() %></h3>
						<p>Total records shown in the table below.</p>
					</article>
				</div>
			</aside>
		</section>

		<!-- <section class="queue-table-card">
			<h2 class="section-title">Entered queue records</h2>
			<p class="section-subtitle">After Confirm, the new WAITING row appears here immediately with created_at and updated_at timestamps.</p>
			<div class="table-wrap">
				<table>
					<thead>
						<tr>
							<th>Ticket</th>
							<th>Patient</th>
							<th>Phone</th>
							<th>Clinic</th>
							<th>Service</th>
							<th>Queue Date</th>
							<th>Status</th>
							<th>Created At</th>
							<th>Updated At</th>
						</tr>
					</thead>
					<tbody>
						<% if (patientQueueTickets.isEmpty()) { %>
						<tr>
							<td colspan="9">No queue record yet. Fill the form and click Confirm Queue Entry.</td>
						</tr>
						<% } else {
							   for (QueueTicket ticket : patientQueueTickets) {
						%>
						<tr class="<%= highlightedRow(highlightedTicketId, ticket) %>">
							<td><%= escapeHtml(ticket.getDisplayCode()) %></td>
							<td><%= safeText(ticket.getPatientName()) %></td>
							<td><%= safeText(patientUser.getPhone()) %></td>
							<td><%= safeText(ticket.getClinicName()) %></td>
							<td><%= safeText(ticket.getServiceName()) %></td>
							<td><%= escapeHtml(ticket.getQueueLabel()) %></td>
							<td><span class="status-chip <%= ticket.getStatusChipClass() %>"><%= safeText(ticket.getStatus()) %></span></td>
							<td><%= formatDateTime(ticket.getCreatedAt()) %></td>
							<td><%= formatDateTime(ticket.getUpdatedAt() == null ? ticket.getCreatedAt() : ticket.getUpdatedAt()) %></td>
						</tr>
						<%     }
						   } %>
					</tbody>
				</table>
			</div>
		</section> -->
	</main>

	<script>
		(function () {
			var clinicSelect = document.getElementById('clinicId');
			var serviceSelect = document.getElementById('serviceId');
			var resetButton = document.getElementById('queueFormReset');
			var formElement = document.getElementById('joinQueueForm');

			var refreshServiceVisibility = function () {
				if (!clinicSelect || !serviceSelect) {
					return;
				}

				var clinicId = clinicSelect.value;
				var previousValue = serviceSelect.value;
				var options = serviceSelect.querySelectorAll('option[data-clinic-id]');
				var hasVisibleSelectedOption = false;

				options.forEach(function (option) {
					var isVisible = clinicId && option.getAttribute('data-clinic-id') === clinicId;
					option.hidden = !isVisible;
					if (isVisible && option.value === previousValue) {
						hasVisibleSelectedOption = true;
					}
				});

				serviceSelect.disabled = !clinicId;
				if (!hasVisibleSelectedOption) {
					serviceSelect.value = '';
				}
			};

			if (clinicSelect) {
				clinicSelect.addEventListener('change', refreshServiceVisibility);
			}

			if (resetButton && formElement) {
				resetButton.addEventListener('click', function () {
					window.location.href = formElement.action;
				});
			}

			refreshServiceVisibility();
		})();
	</script>
</body>
</html>
