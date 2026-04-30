<%@page import="ict.bean.ClinicService" %>
  <%@page import="ict.bean.ReportSummary" %>
    <%@page import="ict.bean.ServiceUtilization" %>
      <%@page import="java.util.Collections" %>
        <%@page import="java.util.List" %>
          <%@page import="java.time.Month" %>
            <%@page import="java.time.format.TextStyle" %>
              <%@page import="java.util.Locale" %>
                <%@page contentType="text/html" pageEncoding="UTF-8" %>
                  <%@ taglib prefix="c" uri="jakarta.tags.core" %>
                    <%@ include file="/admin/common/util.jspf" %>
                      <jsp:useBean id="reportSummary" scope="request" class="ict.bean.ReportSummary" />

                      <% Object sessionUser=session.getAttribute("userInfo"); if (!(sessionUser instanceof
                        ict.bean.User) || !"ADMIN".equalsIgnoreCase(((ict.bean.User) sessionUser).getRole())) {
                        response.sendRedirect(request.getContextPath() + "/login" ); return; } // Prepare attributes for
                        JSTL access List<ClinicService> activeClinics = (List<ClinicService>)
                          request.getAttribute("activeClinics");
                          if (activeClinics == null) activeClinics = Collections.emptyList();
                          pageContext.setAttribute("activeClinics", activeClinics);

                          List<ServiceUtilization> serviceUtilization = (List<ServiceUtilization>)
                              request.getAttribute("serviceUtilization");
                              if (serviceUtilization == null) serviceUtilization = Collections.emptyList();
                              pageContext.setAttribute("serviceUtilization", serviceUtilization);

                              Integer selectedClinicId = (Integer) request.getAttribute("selectedClinicId");
                              pageContext.setAttribute("selectedClinicId", selectedClinicId);

                              Integer selectedMonth = (Integer) request.getAttribute("selectedMonth");
                              if (selectedMonth == null) selectedMonth = java.time.LocalDate.now().getMonthValue();
                              pageContext.setAttribute("selectedMonth", selectedMonth);

                              Integer selectedYear = (Integer) request.getAttribute("selectedYear");
                              if (selectedYear == null) selectedYear = java.time.LocalDate.now().getYear();
                              pageContext.setAttribute("selectedYear", selectedYear);

                              String reportMonthLabel = (String) request.getAttribute("reportMonthLabel");
                              if (reportMonthLabel == null || reportMonthLabel.trim().isEmpty()) {
                              reportMonthLabel = Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                              + " " + selectedYear;
                              }
                              pageContext.setAttribute("reportMonthLabel", reportMonthLabel);

                              String reportError = (String) request.getAttribute("reportError");
                              pageContext.setAttribute("reportError", reportError);
                              %>

                              <!doctype html>
                              <html lang="en">

                              <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>CCHC Admin - Utilization Report</title>
                                <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
                              </head>

                              <body>
                                <%@ include file="/admin/common/nav.jspf" %>

                                  <main class="container">
                                    <section>
                                      <h1>Utilization Report</h1>
                                      <p class="section-subtitle">Inspect service capacity and booking rates for
                                        <strong>
                                          <c:out value="${reportMonthLabel}" />
                                        </strong>. Booking rate = booked slots ÷ total offered slots.</p>
                                    </section>

                                    <c:if test="${not empty reportError}">
                                      <section class="notice" style="margin-bottom: 16px;">
                                        <c:out value="${reportError}" />
                                      </section>
                                    </c:if>

                                    <section class="card" style="margin-bottom: 18px;">
                                      <h2 class="section-title">Utilisation chart</h2>
                                      <div class="report-chart">
                                        <c:choose>
                                          <c:when test="${empty serviceUtilization}">
                                            <p class="muted">No utilisation data is available for the selected clinic
                                              and month.</p>
                                          </c:when>
                                          <c:otherwise>
                                            <c:forEach items="${serviceUtilization}" var="row">
                                              <c:set var="barWidth"
                                                value="${(row.booked > 0 && row.utilisationPercent == 0) ? 1 : (row.utilisationPercent > 100 ? 100 : row.utilisationPercent)}" />
                                              <div class="report-chart-row">
                                                <div class="report-chart-row-head">
                                                  <div>
                                                    <strong>
                                                      <c:out value="${row.serviceName}" />
                                                    </strong>
                                                    <p class="report-chart-row-meta">
                                                      <c:out value="${row.clinicName}" />
                                                    </p>
                                                  </div>
                                                  <span class="status-chip status-confirmed">
                                                    <c:out value="${row.utilisationLabel}" />
                                                  </span>
                                                </div>
                                                <div class="report-chart-bar">
                                                  <span class="report-chart-bar-fill"
                                                    style="width: ${barWidth}%"></span>
                                                </div>
                                                <p class="report-chart-note">
                                                  <c:out value="${row.booked}" /> booked of
                                                  <c:out value="${row.capacity}" /> slots
                                                </p>
                                              </div>
                                            </c:forEach>
                                          </c:otherwise>
                                        </c:choose>
                                      </div>
                                    </section>

                                    <section class="card">
                                      <form class="filter-form"
                                        action="<%= request.getContextPath() %>/admin/reports/utilization" method="get">
                                        <div class="field">
                                          <label for="clinicId">Clinic</label>
                                          <select id="clinicId" name="clinicId">
                                            <option value="0" ${selectedClinicId==0 ? 'selected' : '' }>All Clinics
                                            </option>
                                            <c:forEach items="${activeClinics}" var="clinic">
                                              <option value="${clinic.clinicId}" ${selectedClinicId==clinic.clinicId
                                                ? 'selected' : '' }>
                                                <c:out value="${clinic.clinicName}" />
                                              </option>
                                            </c:forEach>
                                          </select>
                                        </div>
                                        <div class="field">
                                          <label for="month">Month</label>
                                          <select id="month" name="month">
                                            <% for (int m=1; m <=12; m++) { %>
                                              <option value="<%= m %>" <%=selectedMonth==m ? "selected" : "" %>>
                                                <%= Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH) %>
                                              </option>
                                              <% } %>
                                          </select>
                                        </div>
                                        <div class="field">
                                          <label for="year">Year</label>
                                          <select id="year" name="year">
                                            <% int curY=(Integer)pageContext.getAttribute("selectedYear"); for (int
                                              y=curY - 2; y <=curY + 2; y++) { %>
                                              <option value="<%= y %>" <%=curY==y ? "selected" : "" %>><%= y %>
                                              </option>
                                              <% } %>
                                          </select>
                                        </div>
                                        <div class="filter-actions">
                                          <button class="btn btn-primary" type="submit">Refresh</button>
                                          <a class="btn btn-secondary"
                                            href="<%= request.getContextPath() %>/admin/reports/utilization">Reset</a>
                                        </div>
                                      </form>
                                    </section>

                                    <section class="summary-strip">
                                      <article class="summary-box">
                                        <h4>${reportSummary.dailyCompletedAppointments}</h4>
                                        <p>Completed appointments</p>
                                      </article>
                                      <article class="summary-box">
                                        <h4>${reportSummary.dailyNoShowCount}</h4>
                                        <p>No-show appointments</p>
                                      </article>
                                      <article class="summary-box">
                                        <h4>${reportSummary.weeklyServedQueueTickets}</h4>
                                        <p>Served queue tickets</p>
                                      </article>
                                      <article class="summary-box">
                                        <h4>${reportSummary.serviceUtilizationAverage}%</h4>
                                        <p>Average utilization</p>
                                      </article>
                                    </section>

                                    <section class="table-wrap">
                                      <table>
                                        <thead>
                                          <tr>
                                            <th>Clinic</th>
                                            <th>Service</th>
                                            <th>Booked</th>
                                            <th>Capacity</th>
                                            <th>Utilization</th>
                                          </tr>
                                        </thead>
                                        <tbody>
                                          <c:choose>
                                            <c:when test="${empty serviceUtilization}">
                                              <tr>
                                                <td colspan="5">No utilization data matched the current selection.</td>
                                              </tr>
                                            </c:when>
                                            <c:otherwise>
                                              <c:forEach items="${serviceUtilization}" var="row">
                                                <c:set var="tableBarWidth"
                                                  value="${(row.booked > 0 && row.utilisationPercent == 0) ? 1 : (row.utilisationPercent > 100 ? 100 : row.utilisationPercent)}" />
                                                <tr>
                                                  <td>
                                                    <c:out value="${row.clinicName}" />
                                                  </td>
                                                  <td>
                                                    <c:out value="${row.serviceName}" />
                                                  </td>
                                                  <td>
                                                    <c:out value="${row.booked}" />
                                                  </td>
                                                  <td>
                                                    <c:out value="${row.capacity}" />
                                                  </td>
                                                  <td>
                                                    <div class="progress-track">
                                                      <div class="progress-fill" style="width: ${tableBarWidth}%"></div>
                                                    </div>
                                                    <span class="status-chip status-confirmed">
                                                      <c:out value="${row.utilisationLabel}" />
                                                    </span>
                                                  </td>
                                                </tr>
                                              </c:forEach>
                                            </c:otherwise>
                                          </c:choose>
                                        </tbody>
                                      </table>
                                    </section>
                                  </main>

                                  <footer class="site-footer">
                                    <div class="container">Utilization report sample page.</div>
                                  </footer>
                              </body>

                              </html>