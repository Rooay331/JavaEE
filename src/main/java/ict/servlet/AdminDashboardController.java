package ict.servlet;

import ict.bean.ClinicService;
import ict.db.ClinicServiceDB;
import ict.db.PolicyDB;
import ict.db.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(urlPatterns = {"/admin/dashboard"})
public class AdminDashboardController extends AdminControllerSupport {

    private UserDB userDB;
    private PolicyDB policyDB;
    private ClinicServiceDB clinicServiceDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        userDB = new UserDB(dbUrl, dbUser, dbPassword);
        policyDB = new PolicyDB(dbUrl, dbUser, dbPassword);
        clinicServiceDB = new ClinicServiceDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (getLoggedInAdminUser(request, response) == null) {
            return;
        }

        try {
            List<ClinicService> activeClinics = clinicServiceDB.findActiveClinics();
            request.setAttribute("activeClinics", activeClinics == null ? Collections.emptyList() : activeClinics);
            request.setAttribute("totalUsers", userDB.countUsers(null, null, null));
            request.setAttribute("activeUsers", userDB.countUsers(null, null, "ACTIVE"));
            request.setAttribute("activeStaffUsers", userDB.countUsers(null, "STAFF", "ACTIVE"));
            request.setAttribute("activeAdminUsers", userDB.countUsers(null, "ADMIN", "ACTIVE"));
            request.setAttribute("activePatientUsers", userDB.countUsers(null, "PATIENT", "ACTIVE"));
            request.setAttribute("maxBookingsPerPatient", policyDB.findIntPolicy("MAX_ACTIVE_APPOINTMENTS", 3));
            request.setAttribute("maxBookingsPerSlot", policyDB.findIntPolicy("MAX_BOOKINGS_PER_SLOT", 1));
            request.setAttribute("cancellationCutoffHours", policyDB.findIntPolicy("CANCELLATION_CUTOFF_HOURS", 2));
            request.setAttribute("queueEnabled", policyDB.findBooleanPolicy("QUEUE_ENABLED", true));
        } catch (Exception ex) {
            request.setAttribute("dashboardError", "Unable to load admin dashboard data from the database.");
            ex.printStackTrace();
        }

        request.setAttribute("activeAdminPath", "/admin/dashboard");
        request.getRequestDispatcher("/admin/dashboard.jsp").forward(request, response);
    }
}