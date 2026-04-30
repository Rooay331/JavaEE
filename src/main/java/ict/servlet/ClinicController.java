package ict.servlet;

import ict.bean.ClinicService;
import ict.db.ClinicServiceDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(urlPatterns = {"/clinics", "/patient/clinics"})
public class ClinicController extends HttpServlet {

    private ClinicServiceDB clinicServiceDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        clinicServiceDB = new ClinicServiceDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<ClinicService> clinics = Collections.emptyList();
        String clinicError = null;

        try {
            clinics = clinicServiceDB.findActiveClinics();
        } catch (Exception ex) {
            clinicError = "Unable to load clinic information from the database.";
            ex.printStackTrace();
        }

        request.setAttribute("clinics", clinics);
        request.setAttribute("clinicError", clinicError);

        String targetView = "/clinics.jsp";
        if ("/patient/clinics".equals(request.getServletPath())) {
            targetView = "/patient/clinics.jsp";
        }

        request.getRequestDispatcher(targetView).forward(request, response);
    }
}
