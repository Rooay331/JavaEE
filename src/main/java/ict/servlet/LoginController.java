package ict.servlet;

import ict.bean.User;
import ict.db.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;


@WebServlet(urlPatterns = {"/login"})
public class LoginController extends HttpServlet {
    
    private UserDB db;
    
    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        db = new UserDB(dbUrl, dbUser, dbPassword);
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {        

        request.getRequestDispatcher("/login.jsp").forward(request, response);
        
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {        
        String username = request.getParameter("username");
        if (username != null) {
            username = username.trim();
        }
        String password = request.getParameter("password");

        User authenticatedUser = db.findUserByCredentials(username, password);
        if (authenticatedUser != null) {
            try {
                db.updateLastLogin(authenticatedUser.getUserId());
                authenticatedUser.setLastLoginAt(LocalDateTime.now());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            String role = authenticatedUser.getRole();
            if ("STAFF".equalsIgnoreCase(role) && authenticatedUser.getClinicId() == null) {
                User formUser = new User();
                formUser.setFullName(username);
                request.setAttribute("loginUser", formUser);
                request.setAttribute("errorMessage", "Staff account is missing an assigned clinic.");
                request.getRequestDispatcher("/login.jsp").forward(request, response);
                return;
            }

            authenticatedUser.setPassword(null);
            HttpSession existingSession = request.getSession(false);
            if (existingSession != null) {
                existingSession.invalidate();
            }

            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute("userInfo", authenticatedUser);

            String targetURL;
            if ("ADMIN".equalsIgnoreCase(role)) {
                targetURL = "/admin/dashboard";
            } else if ("STAFF".equalsIgnoreCase(role)) {
                targetURL = "/staff/dashboard";
            } else if ("PATIENT".equalsIgnoreCase(role)) {
                targetURL = "/patient/dashboard";
            } else {
                targetURL = "/index.html";
            }

            response.sendRedirect(request.getContextPath() + targetURL);
            return;
        }

        User formUser = new User();
        formUser.setFullName(username);
        request.setAttribute("loginUser", formUser);
        request.setAttribute("errorMessage", "Invalid login credentials.");
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }
}
