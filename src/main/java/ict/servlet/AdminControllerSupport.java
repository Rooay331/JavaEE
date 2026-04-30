package ict.servlet;

import ict.bean.User;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public abstract class AdminControllerSupport extends HttpServlet {

    protected User getLoggedInAdminUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

        User adminUser = (User) sessionUser;
        if (!"ADMIN".equalsIgnoreCase(adminUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        session.setMaxInactiveInterval(30 * 60);
        return adminUser;
    }

    protected void syncSessionUser(HttpServletRequest request, User updatedUser) {
        if (request == null || updatedUser == null) {
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        Object sessionUser = session.getAttribute("userInfo");
        if (sessionUser instanceof User) {
            session.setAttribute("userInfo", updatedUser);
        }
    }

    protected String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.valueOf(value.trim().replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    protected LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    protected LocalTime parseTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    protected boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase();
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized) || "enabled".equals(normalized) || "on".equals(normalized);
    }
}