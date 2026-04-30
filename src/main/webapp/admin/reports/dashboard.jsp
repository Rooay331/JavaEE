<%@page import="ict.bean.Appointment" %>
  <%@page import="ict.bean.ClinicService" %>
    <%@page import="ict.bean.NoShowSummaryRow" %>
      <%@page import="ict.bean.ReportSummary" %>
        <%@page import="ict.bean.ServiceUtilization" %>
          <%@page import="java.util.Collections" %>
            <%@page import="java.util.List" %>
              <%@page contentType="text/html" pageEncoding="UTF-8" %>
              
                  <%@ include file="/admin/common/util.jspf" %>
                    <jsp:useBean id="reportSummary" scope="request" class="ict.bean.ReportSummary" />
                    <% Object sessionUser=session.getAttribute("userInfo"); if (!(sessionUser instanceof ict.bean.User)
                      || !"ADMIN".equalsIgnoreCase(((ict.bean.User) sessionUser).getRole())) {
                      response.sendRedirect(request.getContextPath() + "/login" ); return; } List<ClinicService>
                      activeClinics = (List<ClinicService>) request.getAttribute("activeClinics");
                        if (activeClinics == null) activeClinics = Collections.emptyList();
                        pageContext.setAttribute("activeClinics", activeClinics);

                        List<ServiceUtilization> serviceUtilization = (List<ServiceUtilization>)
                            request.getAttribute("serviceUtilization");
                            if (serviceUtilization == null) serviceUtilization = Collections.emptyList();
                            pageContext.setAttribute("serviceUtilization", serviceUtilization);

                            List<NoShowSummaryRow> noShowSummaryRows = (List<NoShowSummaryRow>)
                                request.getAttribute("noShowSummaryRows");
                                if (noShowSummaryRows == null) noShowSummaryRows = Collections.emptyList();
                                pageContext.setAttribute("noShowSummaryRows", noShowSummaryRows);

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
                                reportMonthLabel = monthName(selectedMonth) + " " + selectedYear;
                                }
                                pageContext.setAttribute("reportMonthLabel", reportMonthLabel);

                                Integer maxNoShowCount = (Integer) request.getAttribute("maxNoShowCount");
                                if (maxNoShowCount == null) maxNoShowCount = 0;
                                pageContext.setAttribute("maxNoShowCount", maxNoShowCount);

                                String reportError = (String) request.getAttribute("reportError");
                                pageContext.setAttribute("reportError", reportError);
                                %>

                                <!doctype html>
                                <html lang="en">

                                <head>
                                  <meta charset="UTF-8">
                                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                  <title>CCHC Admin - Reports Dashboard</title>
                                  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
                                  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                                  <script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2.0.0"></script>
                                </head>

                                <body>
                                  <%@ include file="/admin/common/nav.jspf" %>

                                    <main class="container">
                                      <section>
                                        <h1>Reports Dashboard</h1>
                                        <p class="section-subtitle">
                                          Track service utilization, appointment activity, and no-show trends for
                                          <strong>
                                            <c:out value="${reportMonthLabel}" />
                                          </strong>.
                                        </p>
                                      </section>

                                      <section class="card" style="margin-bottom: 16px;">
                                        <form class="filter-form"
                                          action="<%= request.getContextPath() %>/admin/reports/dashboard" method="get">
                                          <div class="field">
                                            <label for="clinicId">Clinic</label>
                                            <select id="clinicId" name="clinicId">
                                              <option value="0" ${selectedClinicId==0 ? 'selected' : '' }>All
                                                Clinics
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
                                                <option value="<%= m %>" <%=selectedMonth !=null && selectedMonth==m
                                                  ? "selected" : "" %>>
                                                  <%= Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH) %>
                                                </option>
                                                <% } %>
                                            </select>
                                          </div>
                                          <div class="field">
                                            <label for="year">Year</label>
                                            <select id="year" name="year">
                                              <% for (int y=selectedYear - 2; y <=selectedYear + 2; y++) { %>
                                                <option value="<%= y %>" <%=selectedYear !=null && selectedYear==y
                                                  ? "selected" : "" %>>
                                                  <%= y %>
                                                </option>
                                                <% } %>
                                            </select>
                                          </div>
                                          <div class="filter-actions">
                                            <button class="btn btn-primary" type="submit">Refresh</button>
                                          </div>
                                        </form>
                                      </section>

                                      <c:if test="${not empty reportError}">
                                        <section class="notice" style="margin-bottom: 16px;">
                                          <c:out value="${reportError}" />
                                        </section>
                                      </c:if>

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

                                      <section class="card" style="margin-bottom: 18px;">
                                        <h2 class="section-title">Popularity Distribution</h2>
                                        <p class="section-subtitle">Based on booking volume.</p>
                                        <div style="max-width: 400px; margin: 0 auto;">
                                          <canvas id="popularityChart"></canvas>
                                        </div>
                                      </section>

                                      <section class="layout-split">
                                        <div class="card">
                                          <h2 class="section-title">Utilization by service</h2>
                                          <div class="list-group">
                                            <c:choose>
                                              <c:when test="${empty serviceUtilization}">
                                                <div class="list-item">
                                                  <p>No service utilization data available for the selected clinic
                                                    and date
                                                    range.</p>
                                                </div>
                                              </c:when>
                                              <c:otherwise>
                                                <c:forEach items="${serviceUtilization}" var="row">
                                                  <div class="list-item">
                                                    <h4>
                                                      <c:out value="${row.serviceName}" />
                                                    </h4>
                                                    <p>
                                                      <c:out value="${row.clinicName}" />
                                                    </p>
                                                    <div class="progress-track">
                                                      <div class="progress-fill"
                                                        style="width:${row.utilisationPercent > 100 ? 100 : row.utilisationPercent}%">
                                                      </div>
                                                    </div>
                                                    <p>
                                                      ${row.booked} booked of ${row.capacity} slots
                                                      <span
                                                        class="status-chip status-confirmed">${row.utilisationLabel}</span>
                                                    </p>
                                                  </div>
                                                </c:forEach>
                                              </c:otherwise>
                                            </c:choose>
                                          </div>
                                        </div>

                                        <aside class="card">
                                          <h2 class="section-title">No-show ranking</h2>
                                          <div class="list-group">
                                            <c:choose>
                                              <c:when test="${empty noShowSummaryRows}">
                                                <div class="list-item">
                                                  <p>No no-show appointments were recorded in this period.</p>
                                                </div>
                                              </c:when>
                                              <c:otherwise>
                                                <c:forEach items="${noShowSummaryRows}" var="row">
                                                  <div class="list-item">
                                                    <h4>
                                                      <c:out value="${row.displayLabel}" />
                                                    </h4>
                                                    <p>
                                                      <c:out value="${row.monthLabel}" />
                                                    </p>
                                                    <c:set var="width"
                                                      value="${maxNoShowCount == 0 ? 0 : (row.noShowCount * 100 / maxNoShowCount)}" />
                                                    <div class="progress-track">
                                                      <div class="progress-fill progress-fill-warm"
                                                        style="width:${width > 100 ? 100 : width}%"></div>
                                                    </div>
                                                    <p>${row.noShowCount} no-shows</p>
                                                  </div>
                                                </c:forEach>
                                              </c:otherwise>
                                            </c:choose>
                                          </div>
                                        </aside>
                                      </section>

                                      <section class="action-bar" style="margin-top: 18px;">
                                        <a class="btn btn-secondary"
                                          href="<%= request.getContextPath() %>/admin/reports/appointments">Appointment
                                          details</a>
                                        <a class="btn btn-secondary"
                                          href="<%= request.getContextPath() %>/admin/reports/utilization">Utilization
                                          details</a>
                                        <a class="btn btn-secondary"
                                          href="<%= request.getContextPath() %>/admin/reports/noshow">No-show
                                          details</a>
                                      </section>
                                    </main>

                                    <footer class="site-footer">
                                      <div class="container">Admin reporting sample dashboard.</div>
                                    </footer>

                                    <div id="chart-data" style="display:none;"
                                      data-labels="<c:set var='firstLabel' value='true'/><c:forEach items='${serviceUtilization}' var='row'><c:if test='${row.booked > 0}'><c:if test='${!firstLabel}'>|</c:if><c:out value='${row.serviceName}' /> (<c:out value='${row.clinicName}' />)<c:set var='firstLabel' value='false'/></c:if></c:forEach>"
                                      data-values="<c:set var='firstValue' value='true'/><c:forEach items='${serviceUtilization}' var='row'><c:if test='${row.booked > 0}'><c:if test='${!firstValue}'>,</c:if>${row.booked}<c:set var='firstValue' value='false'/></c:if></c:forEach>">
                                    </div>

                                    <script>
                                      document.addEventListener('DOMContentLoaded', function () {
                                        const chartCanvas = document.getElementById('popularityChart');
                                        const dataEl = document.getElementById('chart-data');
                                        if (!chartCanvas || !dataEl) return;

                                        const ctx = chartCanvas.getContext('2d');
                                        const rawLabels = dataEl.getAttribute('data-labels');
                                        const rawValues = dataEl.getAttribute('data-values');

                                        const labels = (rawLabels && rawLabels.trim() !== '') ? rawLabels.split('|') : [];
                                        const data = (rawValues && rawValues.trim() !== '') ? rawValues.split(',').map(v => parseInt(v)).filter(n => !isNaN(n)) : [];

                                        const totalBookings = data.reduce((a, b) => a + b, 0);

                                        if (data.length === 0 || (totalBookings === 0 && data.every(v => v === 0))) {
                                          ctx.font = '16px sans-serif';
                                          ctx.textAlign = 'center';
                                          ctx.fillStyle = '#666';
                                          ctx.fillText('No booking data available', chartCanvas.width / 2, chartCanvas.height / 2);
                                          return;
                                        }

                                        const plugins = [];
                                        if (typeof ChartDataLabels !== 'undefined') {
                                          plugins.push(ChartDataLabels);
                                        }

                                        new Chart(ctx, {
                                          type: 'pie',
                                          plugins: plugins,
                                          data: {
                                            labels: labels,
                                            datasets: [{
                                              label: 'Bookings',
                                              data: data,
                                              backgroundColor: [
                                                '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6',
                                                '#ec4899', '#6366f1', '#14b8a6', '#f97316', '#06b6d4'
                                              ],
                                              borderWidth: 1
                                            }]
                                          },
                                          options: {
                                            responsive: true,
                                            maintainAspectRatio: true,
                                            plugins: {
                                              legend: { position: 'bottom' },
                                              datalabels: {
                                                color: '#fff',
                                                font: { weight: 'bold', size: 12 },
                                                formatter: (value) => {
                                                  const percentage = totalBookings > 0 ? Math.round((value / totalBookings) * 100) : 0;
                                                  return value + '\n(' + percentage + '%)';
                                                },
                                                anchor: 'center',
                                                align: 'center',
                                                display: true
                                              },
                                              tooltip: {
                                                callbacks: {
                                                  label: function (context) {
                                                    const label = context.label || '';
                                                    const value = context.parsed || 0;
                                                    const percentage = totalBookings > 0 ? Math.round((value / totalBookings) * 100) : 0;
                                                    return label + ': ' + value + ' bookings (' + percentage + '%)';
                                                  }
                                                }
                                              }
                                            }
                                          }
                                        });
                                      });
                                    </script>
                                </body>

                                </html>