<%@page import="ict.bean.Appointment" %>
  <%@page import="ict.bean.ClinicService" %>
    <%@page import="ict.bean.NoShowSummaryRow" %>
      <%@page import="java.util.Collections" %>
        <%@page import="java.util.List" %>
          <%@page import="java.time.Month" %>
            <%@page import="java.time.format.TextStyle" %>
              <%@page import="java.util.Locale" %>
                <%@page contentType="text/html" pageEncoding="UTF-8" %>
                  <%@ taglib prefix="c" uri="jakarta.tags.core" %>
                    <%@ include file="/admin/common/util.jspf" %>
                      <% Object sessionUser=session.getAttribute("userInfo"); if (!(sessionUser instanceof
                        ict.bean.User) || !"ADMIN".equalsIgnoreCase(((ict.bean.User) sessionUser).getRole())) {
                        response.sendRedirect(request.getContextPath() + "/login" ); return; } List<ClinicService>
                        activeClinics = (List<ClinicService>) request.getAttribute("activeClinics");
                          if (activeClinics == null) activeClinics = Collections.emptyList();
                          pageContext.setAttribute("activeClinics", activeClinics);

                          List<NoShowSummaryRow> noShowSummaryRows = (List<NoShowSummaryRow>)
                              request.getAttribute("noShowSummaryRows");
                              if (noShowSummaryRows == null) noShowSummaryRows = Collections.emptyList();
                              pageContext.setAttribute("noShowSummaryRows", noShowSummaryRows);

                              Integer maxNoShowCount = (Integer) request.getAttribute("maxNoShowCount");
                              if (maxNoShowCount == null) maxNoShowCount = 0;
                              pageContext.setAttribute("maxNoShowCount", maxNoShowCount);

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

                              String selectedClinicName = (String) request.getAttribute("selectedClinicName");
                              if (selectedClinicName == null || selectedClinicName.trim().isEmpty()) {
                              selectedClinicName = "Selected clinic";
                              }
                              pageContext.setAttribute("selectedClinicName", selectedClinicName);

                              int totalNoShowCount = 0;
                              for (NoShowSummaryRow row : noShowSummaryRows) {
                              if (row != null) {
                              totalNoShowCount += row.getNoShowCount();
                              }
                              }
                              pageContext.setAttribute("totalNoShowCount", totalNoShowCount);

                              String reportError = (String) request.getAttribute("reportError");
                              pageContext.setAttribute("reportError", reportError);
                              %>
                              <!doctype html>
                              <html lang="en">

                              <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>CCHC Admin - No-show Report</title>
                                <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
                              </head>

                              <body>
                                <%@ include file="/admin/common/nav.jspf" %>

                                  <main class="container">
                                    <section>
                                      <h1>No-show Report</h1>
                                      <p class="section-subtitle">Review missed appointments by clinic and service for
                                        <strong>
                                          <c:out value="${reportMonthLabel}" />
                                        </strong>.</p>
                                    </section>

                                    <c:if test="${not empty reportError}">
                                      <section class="notice" style="margin-bottom: 16px;">
                                        <c:out value="${reportError}" />
                                      </section>
                                    </c:if>

                                    <section class="card">
                                      <form class="filter-form"
                                        action="<%= request.getContextPath() %>/admin/reports/noshow" method="get">
                                        <div class="field">
                                          <label for="clinicId">Clinic</label>
                                          <select id="clinicId" name="clinicId">
                                            <c:choose>
                                              <c:when test="${empty activeClinics}">
                                                <option value="">No active clinics</option>
                                              </c:when>
                                              <c:otherwise>
                                                <c:forEach items="${activeClinics}" var="clinic">
                                                  <option value="${clinic.clinicId}" ${selectedClinicId==clinic.clinicId
                                                    ? 'selected' : '' }>
                                                    <c:out value="${clinic.clinicName}" />
                                                  </option>
                                                </c:forEach>
                                              </c:otherwise>
                                            </c:choose>
                                          </select>
                                        </div>
                                        <div class="field">
                                          <label for="month">Month</label>
                                          <select id="month" name="month">
                                            <% for (int m=1; m <=12; m++) { %>
                                              <option value="<%= m %>"
                                                <%=(Integer)pageContext.getAttribute("selectedMonth")==m ? "selected"
                                                : "" %>><%= Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                                  %>
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
                                            href="<%= request.getContextPath() %>/admin/reports/noshow">Reset</a>
                                        </div>
                                      </form>
                                    </section>

                                    <section class="summary-strip">
                                      <article class="summary-box">
                                        <h4>
                                          <c:out value="${selectedClinicName}" />
                                        </h4>
                                        <p>Clinic</p>
                                      </article>
                                      <article class="summary-box">
                                        <h4>
                                          <c:out value="${reportMonthLabel}" />
                                        </h4>
                                        <p>Reporting month</p>
                                      </article>
                                      <article class="summary-box">
                                        <h4>${totalNoShowCount}</h4>
                                        <p>No-shows recorded</p>
                                      </article>
                                      <article class="summary-box">
                                        <h4>${noShowSummaryRows.size()}</h4>
                                        <p>Service groups</p>
                                      </article>
                                    </section>

                                    <section class="card" style="margin-bottom: 18px;">
                                      <h2 class="section-title">No-show chart</h2>
                                      <div class="report-chart">
                                        <c:choose>
                                          <c:when test="${empty noShowSummaryRows}">
                                            <p class="muted">No no-show appointments were recorded in the selected
                                              period.</p>
                                          </c:when>
                                          <c:otherwise>
                                            <c:forEach items="${noShowSummaryRows}" var="row">
                                              <c:set var="width"
                                                value="${maxNoShowCount == 0 ? 0 : (row.noShowCount * 100 / maxNoShowCount)}" />
                                              <div class="report-chart-row">
                                                <div class="report-chart-row-head">
                                                  <div>
                                                    <strong>
                                                      <c:out value="${row.displayLabel}" />
                                                    </strong>
                                                    <p class="report-chart-row-meta">
                                                      <c:out value="${row.monthLabel}" />
                                                    </p>
                                                  </div>
                                                  <span class="status-chip status-cancelled">
                                                    <c:out value="${row.noShowCount}" />
                                                  </span>
                                                </div>
                                                <div class="report-chart-bar"><span
                                                    class="report-chart-bar-fill is-warm"
                                                    style="width:${width > 100 ? 100 : width}%"></span></div>
                                                <p class="report-chart-note">
                                                  <c:out value="${row.noShowLabel}" />
                                                </p>
                                              </div>
                                            </c:forEach>
                                          </c:otherwise>
                                        </c:choose>
                                      </div>
                                    </section>

                                    <section class="table-wrap">
                                      <table>
                                        <thead>
                                          <tr>
                                            <th>Clinic</th>
                                            <th>Service</th>
                                            <th>Month</th>
                                            <th>No-shows</th>
                                          </tr>
                                        </thead>
                                        <tbody>
                                          <c:choose>
                                            <c:when test="${empty noShowSummaryRows}">
                                              <tr>
                                                <td colspan="4">No no-show summary rows are available for the selected
                                                  month.</td>
                                              </tr>
                                            </c:when>
                                            <c:otherwise>
                                              <c:forEach items="${noShowSummaryRows}" var="row">
                                                <tr>
                                                  <td>
                                                    <c:out value="${row.clinicName}" />
                                                  </td>
                                                  <td>
                                                    <c:out value="${row.serviceName}" />
                                                  </td>
                                                  <td>
                                                    <c:out value="${row.monthLabel}" />
                                                  </td>
                                                  <td>
                                                    <c:out value="${row.noShowCount}" />
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
                                    <div class="container">No-show report sample page.</div>
                                  </footer>
                              </body>

                              </html>