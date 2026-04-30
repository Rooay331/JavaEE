<%@page import="ict.bean.ClinicService"%>
<%@page import="ict.bean.User"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.LinkedHashSet"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Set"%>
<%@page contentType="text/html" pageEncoding="UTF-8" %>
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

    if (request.getAttribute("clinics") == null && request.getAttribute("clinicError") == null) {
        response.sendRedirect(request.getContextPath() + "/patient/clinics");
        return;
    }

    List<ClinicService> clinics = (List<ClinicService>) request.getAttribute("clinics");
    if (clinics == null) {
        clinics = Collections.emptyList();
    }

    String clinicError = (String) request.getAttribute("clinicError");
    int clinicCount = clinics.size();
    int serviceCount = 0;
    Set<String> districts = new LinkedHashSet<>();
    for (ClinicService clinic : clinics) {
        if (clinic == null) {
            continue;
        }
        serviceCount += clinic.getServices().size();
        if (clinic.getDistrict() != null && !clinic.getDistrict().trim().isEmpty()) {
            districts.add(clinic.getDistrict().trim());
        }
    }
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CCHC Patient - Clinic List</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
</head>
<body>
    <%
        request.setAttribute("activePatientPath", "/patient/clinics");
    %>
    <%@ include file="common/nav.jspf" %>

    <main class="container">
        <section>
            <h1 class="section-title">Clinic list</h1>
            <p class="section-subtitle">Browse available CCHC clinic locations and their current service lists.</p>
        </section>

        <% if (clinicError != null && !clinicError.trim().isEmpty()) { %>
        <section class="notice" style="margin-bottom: 16px;">
            <%= clinicError %>
        </section>
        <% } %>

        <section class="summary-strip">
            <div class="summary-box">
                <h4><%= clinicCount %></h4>
                <p>Active clinics</p>
            </div>
            <div class="summary-box">
                <h4><%= serviceCount %></h4>
                <p>Active services</p>
            </div>
            <div class="summary-box">
                <h4><%= districts.size() %></h4>
                <p>Districts covered</p>
            </div>
            <div class="summary-box">
                <h4>Live</h4>
                <p>Database source</p>
            </div>
        </section>

        <section class="table-wrap" aria-label="Clinic table">
            <table>
                <thead>
                    <tr>
                        <th>Clinic Name</th>
                        <th>District</th>
                        <th>Address</th>
                        <th>Phone</th>
                        <th>Services</th>
                    </tr>
                </thead>
                <tbody>
                    <% if (clinics.isEmpty()) { %>
                    <tr>
                        <td colspan="5">No clinic data available.</td>
                    </tr>
                    <% } else {
                        for (ClinicService clinic : clinics) {
                    %>
                    <tr>
                        <td><%= clinic.getClinicName() %></td>
                        <td><%= clinic.getDistrict() == null ? "-" : clinic.getDistrict() %></td>
                        <td><%= clinic.getAddress() == null ? "-" : clinic.getAddress() %></td>
                        <td><%= clinic.getPhone() == null ? "-" : clinic.getPhone() %></td>
                        <td><%= clinic.getServicesLabel() %></td>
                    </tr>
                    <%      }
                       } %>
                </tbody>
            </table>
        </section>

        <section style="margin-top: 18px;">
            <div class="grid grid-3">
                <article class="card">
                    <h3>General consultation</h3>
                    <p>Suitable for routine symptoms and follow-up care.</p>
                </article>
                <article class="card">
                    <h3>Vaccination</h3>
                    <p>Includes common preventive vaccines by appointment or walk-in rules.</p>
                </article>
                <article class="card">
                    <h3>Basic screening</h3>
                    <p>Health checks with simple reporting and referral recommendations.</p>
                </article>
            </div>
        </section>
    </main>

    <footer class="site-footer">
        <div class="container">Use the clinic list to plan appointments and queues.</div>
    </footer>
</body>
</html>
