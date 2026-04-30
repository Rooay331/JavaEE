package ict.servlet;

import ict.bean.ClinicService;
import ict.bean.ClinicServiceStatus;
import ict.db.ClinicServiceDB;
import ict.db.PolicyDB;
import ict.db.ServiceDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(urlPatterns = {"/admin/policies/system", "/admin/policies/clinic"})
public class AdminPolicyController extends AdminControllerSupport {

    private PolicyDB policyDB;
    private ClinicServiceDB clinicServiceDB;
    private ServiceDB serviceDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        policyDB = new PolicyDB(dbUrl, dbUser, dbPassword);
        clinicServiceDB = new ClinicServiceDB(dbUrl, dbUser, dbPassword);
        serviceDB = new ServiceDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (getLoggedInAdminUser(request, response) == null) {
            return;
        }

        String servletPath = request.getServletPath();
        if ("/admin/policies/system".equals(servletPath)) {
            showSystemPolicies(request, response);
            return;
        }
        if ("/admin/policies/clinic".equals(servletPath)) {
            showClinicPolicies(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (getLoggedInAdminUser(request, response) == null) {
            return;
        }

        String servletPath = request.getServletPath();
        if ("/admin/policies/system".equals(servletPath)) {
            handleSystemPolicyUpdate(request, response);
            return;
        }
        if ("/admin/policies/clinic".equals(servletPath)) {
            handleClinicPolicyUpdate(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void showSystemPolicies(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            request.setAttribute("maxBookingsPerPatient", policyDB.findIntPolicy("MAX_ACTIVE_APPOINTMENTS", 3));
            request.setAttribute("maxBookingsPerSlot", policyDB.findIntPolicy("MAX_BOOKINGS_PER_SLOT", 1));
            request.setAttribute("cancellationCutoffHours", policyDB.findIntPolicy("CANCELLATION_CUTOFF_HOURS", 2));
            request.setAttribute("queueEnabled", policyDB.findBooleanPolicy("QUEUE_ENABLED", true));
        } catch (Exception ex) {
            request.setAttribute("flashMessage", "Unable to load the system policy settings.");
            request.setAttribute("flashType", "error");
            ex.printStackTrace();
        }

        request.setAttribute("activeAdminPath", "/admin/policies/system");
        request.getRequestDispatcher("/admin/policies/system.jsp").forward(request, response);
    }

    private void showClinicPolicies(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer clinicId = parseInteger(request.getParameter("clinicId"));
        Integer serviceId = parseInteger(request.getParameter("serviceId"));

        List<ClinicService> clinics = Collections.emptyList();
        List<ClinicServiceStatus> services = Collections.emptyList();

        try {
            clinics = clinicServiceDB.findActiveClinics();
            if ((clinicId == null || clinicId <= 0) && clinics != null && !clinics.isEmpty()) {
                clinicId = clinics.get(0).getClinicId();
            }

            services = clinicId == null ? Collections.emptyList() : serviceDB.findClinicServiceStatuses(clinicId);
            if ((serviceId == null || serviceId <= 0) && services != null && !services.isEmpty()) {
                serviceId = services.get(0).getServiceId();
            }

            String policyPrefix = buildClinicServicePolicyPrefix(clinicId, serviceId);
            request.setAttribute("defaultCapacity", policyDB.findIntPolicy(policyPrefix + "CAPACITY", 25));
            request.setAttribute("walkInEnabled", policyDB.findBooleanPolicy(policyPrefix + "WALKIN", true));
            request.setAttribute("approvalRequired", policyDB.findBooleanPolicy(policyPrefix + "APPROVAL", false));
            request.setAttribute("adminNote", policyDB.findStringPolicy(policyPrefix + "NOTE", ""));
        } catch (Exception ex) {
            request.setAttribute("flashMessage", "Unable to load the clinic policy settings.");
            request.setAttribute("flashType", "error");
            ex.printStackTrace();
        }

        request.setAttribute("activeClinics", clinics == null ? Collections.emptyList() : clinics);
        request.setAttribute("availableServices", services == null ? Collections.emptyList() : services);
        request.setAttribute("selectedClinicId", clinicId);
        request.setAttribute("selectedServiceId", serviceId);
        request.setAttribute("activeAdminPath", "/admin/policies/clinic");
        request.getRequestDispatcher("/admin/policies/clinic.jsp").forward(request, response);
    }

    private void handleSystemPolicyUpdate(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int maxBookings = parseInteger(request.getParameter("maxBookingsPerPatient")) == null ? 3 : parseInteger(request.getParameter("maxBookingsPerPatient"));
        int maxBookingsPerSlot = parseInteger(request.getParameter("maxBookingsPerSlot")) == null ? 1 : parseInteger(request.getParameter("maxBookingsPerSlot"));
        int cancellationCutoff = parseInteger(request.getParameter("cancellationCutoffHours")) == null ? 2 : parseInteger(request.getParameter("cancellationCutoffHours"));
        boolean queueEnabled = request.getParameter("queueEnabled") != null;

        try {
            policyDB.saveIntPolicy("MAX_ACTIVE_APPOINTMENTS", Math.max(maxBookings, 1));
            policyDB.saveIntPolicy("MAX_BOOKINGS_PER_SLOT", Math.max(maxBookingsPerSlot, 1));
            policyDB.saveIntPolicy("CANCELLATION_CUTOFF_HOURS", Math.max(cancellationCutoff, 0));
            policyDB.saveBooleanPolicy("QUEUE_ENABLED", queueEnabled);
            request.setAttribute("flashMessage", "System policies saved successfully.");
            request.setAttribute("flashType", "success");
        } catch (Exception ex) {
            request.setAttribute("flashMessage", "Unable to save the system policy settings.");
            request.setAttribute("flashType", "error");
            ex.printStackTrace();
        }

        showSystemPolicies(request, response);
    }

    private void handleClinicPolicyUpdate(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer clinicId = parseInteger(request.getParameter("clinicId"));
        Integer serviceId = parseInteger(request.getParameter("serviceId"));
        int defaultCapacity = parseInteger(request.getParameter("defaultCapacity")) == null ? 25 : parseInteger(request.getParameter("defaultCapacity"));
        boolean walkInEnabled = request.getParameter("walkInEnabled") != null;
        boolean approvalRequired = request.getParameter("approvalRequired") != null;
        String note = normalize(request.getParameter("adminNote"));

        if (clinicId == null || serviceId == null) {
            request.setAttribute("flashMessage", "Please choose both a clinic and a service.");
            request.setAttribute("flashType", "error");
            showClinicPolicies(request, response);
            return;
        }

        String policyPrefix = buildClinicServicePolicyPrefix(clinicId, serviceId);
        try {
            policyDB.saveIntPolicy(policyPrefix + "CAPACITY", Math.max(defaultCapacity, 1));
            policyDB.saveBooleanPolicy(policyPrefix + "WALKIN", walkInEnabled);
            policyDB.saveBooleanPolicy(policyPrefix + "APPROVAL", approvalRequired);
            policyDB.savePolicyValue(policyPrefix + "NOTE", note == null ? "" : note);
            request.setAttribute("flashMessage", "Clinic policy saved successfully.");
            request.setAttribute("flashType", "success");
        } catch (Exception ex) {
            request.setAttribute("flashMessage", "Unable to save the clinic policy settings.");
            request.setAttribute("flashType", "error");
            ex.printStackTrace();
        }

        showClinicPolicies(request, response);
    }

    private String buildClinicServicePolicyPrefix(Integer clinicId, Integer serviceId) {
        return "CLINIC_" + (clinicId == null ? "0" : clinicId) + "_SERVICE_" + (serviceId == null ? "0" : serviceId) + "_";
    }
}