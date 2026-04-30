package ict.servlet;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ict.bean.Appointment;
import ict.bean.ClinicService;
import ict.bean.NoShowSummaryRow;
import ict.bean.ReportSummary;
import ict.bean.ServiceUtilization;
import ict.db.AppointmentDB;
import ict.db.ClinicServiceDB;
import ict.db.ReportDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/admin/reports/dashboard", "/admin/reports/appointments", "/admin/reports/utilization", "/admin/reports/noshow"})
public class AdminReportController extends AdminControllerSupport {

    private ReportDB reportDB;
    private AppointmentDB appointmentDB;
    private ClinicServiceDB clinicServiceDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        reportDB = new ReportDB(dbUrl, dbUser, dbPassword);
        appointmentDB = new AppointmentDB(dbUrl, dbUser, dbPassword);
        clinicServiceDB = new ClinicServiceDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (getLoggedInAdminUser(request, response) == null) {
            return;
        }

        String servletPath = request.getServletPath();

        int selectedYear = resolveYear(request.getParameter("year"));
        int selectedMonth = resolveMonth(request.getParameter("month"));
        YearMonth selectedPeriod = YearMonth.of(selectedYear, selectedMonth);
        LocalDate fromDate = selectedPeriod.atDay(1);
        LocalDate toDate = selectedPeriod.atEndOfMonth();
        String reportMonthLabel = selectedPeriod.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + selectedYear;

        Integer clinicId = parseInteger(request.getParameter("clinicId"));
        String serviceName = normalize(request.getParameter("serviceName"));
        String status = normalizeStatusFilter(request.getParameter("status"));
        String patientName = normalize(request.getParameter("patientName"));

        List<ClinicService> activeClinics = Collections.emptyList();
        List<String> availableServices = Collections.emptyList();
        List<ServiceUtilization> serviceUtilization = Collections.emptyList();
        List<Appointment> appointmentRows = Collections.emptyList();
        List<NoShowSummaryRow> noShowSummaryRows = Collections.emptyList();
        ReportSummary reportSummary = new ReportSummary();
        Integer selectedClinicId = clinicId;
        String selectedClinicName = null;

        try {
            activeClinics = clinicServiceDB.findActiveClinics();
            selectedClinicName = resolveClinicName(selectedClinicId, activeClinics);

            availableServices = (selectedClinicId != null && selectedClinicId > 0) 
                    ? appointmentDB.findServiceNamesByClinic(selectedClinicId)
                    : Collections.emptyList();

            reportSummary = reportDB.findSummary(selectedClinicId, fromDate, toDate);
            serviceUtilization = reportDB.findServiceUtilization(selectedClinicId, fromDate, toDate);

            List<Appointment> allAppointments = appointmentDB.findAppointmentsByClinic(selectedClinicId, null, null, null, fromDate, toDate);
            noShowSummaryRows = buildNoShowSummaryRows(allAppointments, reportMonthLabel);

            if (servletPath != null && (servletPath.endsWith("/appointments") || servletPath.endsWith("/appointments.html"))) {
                appointmentRows = appointmentDB.findAppointmentsByClinic(selectedClinicId, serviceName, status, patientName, fromDate, toDate);
            } else if (servletPath != null && (servletPath.endsWith("/noshow") || servletPath.endsWith("/noshow.html"))) {
                appointmentRows = appointmentDB.findAppointmentsByClinic(selectedClinicId, null, "NO_SHOW", null, fromDate, toDate);
            }
        } catch (Exception ex) {
            request.setAttribute("reportError", "Unable to load report data from the database.");
            ex.printStackTrace();
        }

        request.setAttribute("activeClinics", activeClinics);
        request.setAttribute("availableServices", availableServices);
        request.setAttribute("selectedClinicId", selectedClinicId);
        request.setAttribute("selectedMonth", selectedMonth);
        request.setAttribute("selectedYear", selectedYear);
        request.setAttribute("selectedFromDate", fromDate);
        request.setAttribute("selectedToDate", toDate);
        request.setAttribute("reportMonthLabel", reportMonthLabel);
        request.setAttribute("selectedClinicName", selectedClinicName);
        request.setAttribute("selectedServiceName", serviceName);
        request.setAttribute("selectedStatus", status);
        request.setAttribute("selectedPatientName", patientName);
        request.setAttribute("reportSummary", reportSummary);
        request.setAttribute("serviceUtilization", serviceUtilization);
        request.setAttribute("appointmentRows", appointmentRows);
        request.setAttribute("noShowSummaryRows", noShowSummaryRows);
        request.setAttribute("maxNoShowCount", findMaxValue(noShowSummaryRows));

        request.setAttribute("activeAdminPath", servletPath);
        request.getRequestDispatcher(resolveView(servletPath)).forward(request, response);
    }

    private String resolveView(String servletPath) {
        if (servletPath != null && servletPath.contains("/appointments")) {
            return "/admin/reports/appointments.jsp";
        }
        if (servletPath != null && servletPath.contains("/utilization")) {
            return "/admin/reports/utilization.jsp";
        }
        if (servletPath != null && servletPath.contains("/noshow")) {
            return "/admin/reports/noshow.jsp";
        }
        return "/admin/reports/dashboard.jsp";
    }

    private List<NoShowSummaryRow> buildNoShowSummaryRows(List<Appointment> appointments, String monthLabel) {
        Map<String, NoShowSummaryRow> countsByKey = new LinkedHashMap<>();
        if (appointments == null) {
            return Collections.emptyList();
        }

        for (Appointment appointment : appointments) {
            if (appointment == null || appointment.getStatus() == null || !"NO_SHOW".equalsIgnoreCase(appointment.getStatus())) {
                continue;
            }

            String clinicLabel = safeLabel(appointment.getClinicName(), "Unknown clinic");
            String serviceLabel = safeLabel(appointment.getServiceName(), "Unknown service");
            String summaryKey = clinicLabel + "||" + serviceLabel;

            NoShowSummaryRow summaryRow = countsByKey.get(summaryKey);
            if (summaryRow == null) {
                summaryRow = new NoShowSummaryRow();
                summaryRow.setClinicName(clinicLabel);
                summaryRow.setServiceName(serviceLabel);
                summaryRow.setMonthLabel(monthLabel);
                countsByKey.put(summaryKey, summaryRow);
            }
            summaryRow.setNoShowCount(summaryRow.getNoShowCount() + 1);
        }

        List<NoShowSummaryRow> sortedRows = new ArrayList<>(countsByKey.values());
        sortedRows.sort((left, right) -> Integer.compare(right.getNoShowCount(), left.getNoShowCount()));
        return sortedRows;
    }

    private String resolveClinicName(Integer clinicId, List<ClinicService> activeClinics) {
        if (clinicId == null || clinicId <= 0) {
            return "All Clinics";
        }

        if (activeClinics != null) {
            for (ClinicService clinic : activeClinics) {
                if (clinic != null && clinicId.equals(clinic.getClinicId())) {
                    return clinic.getClinicName();
                }
            }
        }

        return "Clinic ID " + clinicId;
    }

    private String safeLabel(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private int findMaxValue(List<NoShowSummaryRow> rows) {
        int max = 0;
        if (rows == null) {
            return max;
        }

        for (NoShowSummaryRow row : rows) {
            if (row != null && row.getNoShowCount() > max) {
                max = row.getNoShowCount();
            }
        }
        return max;
    }

    private String normalizeStatusFilter(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ENGLISH);
    }

    private int resolveMonth(String value) {
        Integer parsed = parseInteger(value);
        if (parsed == null || parsed < 1 || parsed > 12) {
            return LocalDate.now().getMonthValue();
        }
        return parsed;
    }

    private int resolveYear(String value) {
        Integer parsed = parseInteger(value);
        if (parsed == null || parsed < 2000 || parsed > 2100) {
            return LocalDate.now().getYear();
        }
        return parsed;
    }
}