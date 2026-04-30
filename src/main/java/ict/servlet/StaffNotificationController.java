package ict.servlet;

import ict.bean.Notification;
import ict.bean.User;
import ict.db.ClinicDB;
import ict.db.NotificationDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(urlPatterns = {"/staff/notifications"})
public class StaffNotificationController extends HttpServlet {

    private NotificationDB notificationDB;
    private ClinicDB clinicDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        notificationDB = new NotificationDB(dbUrl, dbUser, dbPassword);
        clinicDB = new ClinicDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handlePage(request, response, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User staffUser = getLoggedInStaffUser(request, response);
        if (staffUser == null) {
            return;
        }

        String action = request.getParameter("notificationAction");
        String notificationIdText = request.getParameter("notificationId");
        Integer notificationId = parseNotificationId(notificationIdText);
        String flashMessage;
        String flashType;

        try {
            if ("MARK_ALL_READ".equalsIgnoreCase(action)) {
                boolean updated = notificationDB.markAllNotificationsAsRead(staffUser.getClinicId());
                flashMessage = updated ? "All notifications were marked as read." : "No unread notifications were updated.";
                flashType = updated ? "success" : "error";
            } else if ("MARK_READ".equalsIgnoreCase(action) && notificationId != null) {
                boolean updated = notificationDB.markNotificationAsRead(notificationId, staffUser.getClinicId());
                flashMessage = updated ? "Notification marked as read." : "Unable to update the notification.";
                flashType = updated ? "success" : "error";
            } else if ("DELETE".equalsIgnoreCase(action) && notificationId != null) {
                boolean deleted = notificationDB.deleteNotification(notificationId, staffUser.getClinicId());
                flashMessage = deleted ? "Notification deleted." : "Unable to delete the notification.";
                flashType = deleted ? "success" : "error";
            } else {
                flashMessage = "Unsupported notification action.";
                flashType = "error";
            }
        } catch (Exception ex) {
            flashMessage = "Unable to update notifications from the database.";
            flashType = "error";
            ex.printStackTrace();
        }

        handlePage(request, response, flashMessage, flashType);
    }

    private void handlePage(HttpServletRequest request, HttpServletResponse response, String flashMessage, String flashType)
            throws ServletException, IOException {
        User staffUser = getLoggedInStaffUser(request, response);
        if (staffUser == null) {
            return;
        }

        List<Notification> notifications = Collections.emptyList();
        String notificationsError = null;

        try {
            notifications = notificationDB.findNotificationsByClinic(staffUser.getClinicId());
        } catch (Exception ex) {
            notificationsError = "Unable to load notification records from the database.";
            ex.printStackTrace();
        }

        int unreadCount = 0;
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                unreadCount++;
            }
        }

        request.setAttribute("notifications", notifications);
        request.setAttribute("notificationsError", notificationsError);
        request.setAttribute("notificationCount", notifications.size());
        request.setAttribute("unreadNotificationCount", unreadCount);
        request.setAttribute("readNotificationCount", notifications.size() - unreadCount);
        request.setAttribute("assignedClinicId", staffUser.getClinicId());
        request.setAttribute("assignedClinicName", findClinicName(staffUser.getClinicId()));
        request.setAttribute("activeStaffPath", "/staff/notifications");
        if (flashMessage != null) {
            request.setAttribute("flashMessage", flashMessage);
            request.setAttribute("flashType", flashType);
        }
        request.getRequestDispatcher("/staff/notifications.jsp").forward(request, response);
    }

    private User getLoggedInStaffUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

        User staffUser = (User) sessionUser;
        if (!"STAFF".equalsIgnoreCase(staffUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        if (staffUser.getClinicId() == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Staff account is missing an assigned clinic.");
            return null;
        }

        return staffUser;
    }

    private String findClinicName(Integer clinicId) {
        try {
            String clinicName = clinicDB.findClinicNameById(clinicId);
            return clinicName == null ? "Clinic ID " + clinicId : clinicName;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Clinic ID " + clinicId;
        }
    }

    private Integer parseNotificationId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }

        try {
            return Integer.valueOf(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}