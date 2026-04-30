package ict.servlet;

import ict.bean.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(urlPatterns = {"/admin", "/admin/*", "/staff", "/staff/*", "/patient", "/patient/*"})
public class AdminAccessFilter implements Filter {

    private static final int SESSION_TIMEOUT_SECONDS = 30 * 60;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        HttpSession session = httpRequest.getSession(false);
        User loggedInUser = null;
        if (session != null) {
            Object sessionUser = session.getAttribute("userInfo");
            if (sessionUser instanceof User) {
                loggedInUser = (User) sessionUser;
            }
        }

        if (loggedInUser == null || loggedInUser.getRole() == null) {
            redirectToLogin(httpRequest, httpResponse);
            return;
        }

        String role = loggedInUser.getRole().trim().toUpperCase();
        if (path.startsWith("/admin") && !"ADMIN".equals(role)) {
            redirectToRoleHome(httpRequest, httpResponse, role);
            return;
        }
        if (path.startsWith("/staff") && !"STAFF".equals(role)) {
            redirectToRoleHome(httpRequest, httpResponse, role);
            return;
        }
        if (path.startsWith("/patient") && !"PATIENT".equals(role)) {
            redirectToRoleHome(httpRequest, httpResponse, role);
            return;
        }

        if (session != null) {
            session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
        }

        httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        httpResponse.setHeader("Pragma", "no-cache");
        httpResponse.setDateHeader("Expires", 0);

        if ("/admin".equals(path)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/admin/dashboard");
            return;
        }
        if ("/staff".equals(path)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/staff/dashboard");
            return;
        }
        if ("/patient".equals(path)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/patient/dashboard");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/login");
    }

    private void redirectToRoleHome(HttpServletRequest request, HttpServletResponse response, String role) throws IOException {
        String target;
        if ("ADMIN".equals(role)) {
            target = "/admin/dashboard";
        } else if ("STAFF".equals(role)) {
            target = "/staff/dashboard";
        } else if ("PATIENT".equals(role)) {
            target = "/patient/dashboard";
        } else {
            target = "/login";
        }

        response.sendRedirect(request.getContextPath() + target);
    }
}