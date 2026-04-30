package ict.servlet;

import ict.bean.Appointment;
import ict.bean.QueueTicket;
import ict.bean.User;
import ict.db.AppointmentDB;
import ict.db.PatientDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(urlPatterns = {"/patient/dashboard"})
public class PatientDashboardController extends HttpServlet {

    private PatientDB patientDB;
    private AppointmentDB appointmentDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        patientDB = new PatientDB(dbUrl, dbUser, dbPassword);
        appointmentDB = new AppointmentDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User patientUser = getLoggedInPatientUser(request, response);
        if (patientUser == null) {
            return;
        }

        User patientProfile = patientUser;
        List<Appointment> recentAppointments = Collections.emptyList();
        Appointment activeAppointment = null;
        QueueTicket activeQueueTicket = null;
        String dashboardError = null;

        try {
            User persistedProfile = patientDB.findPatientById(patientUser.getUserId());
            if (persistedProfile != null) {
                patientProfile = persistedProfile;
            }

            recentAppointments = appointmentDB.findPatientAppointments(patientUser.getUserId(), 8);
            for (Appointment appointment : recentAppointments) {
                if (appointment != null && isActiveAppointmentStatus(appointment.getStatus())) {
                    activeAppointment = appointment;
                    break;
                }
            }

            activeQueueTicket = patientDB.findAnyActiveQueueTicketByPatient(patientUser.getUserId());
        } catch (Exception ex) {
            dashboardError = "Unable to load your dashboard from the database.";
            ex.printStackTrace();
        }

        int activeAppointmentCount = 0;
        for (Appointment appointment : recentAppointments) {
            if (appointment != null && isActiveAppointmentStatus(appointment.getStatus())) {
                activeAppointmentCount++;
            }
        }

        request.setAttribute("patientUser", patientProfile);
        request.setAttribute("recentAppointments", recentAppointments);
        request.setAttribute("activeAppointment", activeAppointment);
        request.setAttribute("activeQueueTicket", activeQueueTicket);
        request.setAttribute("activeAppointmentCount", activeAppointmentCount);
        request.setAttribute("recentAppointmentCount", recentAppointments.size());
        request.setAttribute("dashboardError", dashboardError);
        request.setAttribute("activePatientPath", "/patient/dashboard");
        request.getRequestDispatcher("/patient/dashboard.jsp").forward(request, response);
    }

    private User getLoggedInPatientUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        Object sessionUser = session.getAttribute("userInfo");
        if (!(sessionUser instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        User patientUser = (User) sessionUser;
        if (!"PATIENT".equalsIgnoreCase(patientUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        return patientUser;
    }

    private boolean isActiveAppointmentStatus(String status) {
        if (status == null) {
            return false;
        }

        switch (status.toUpperCase()) {
            case "PENDING":
            case "CONFIRMED":
            case "ARRIVED":
                return true;
            default:
                return false;
        }
    }
}
