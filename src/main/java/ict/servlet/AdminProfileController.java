package ict.servlet;

import ict.bean.User;
import ict.db.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

@WebServlet(urlPatterns = {"/admin/profile/view", "/admin/profile/edit", "/admin/profile/change-password"})
public class AdminProfileController extends AdminControllerSupport {

    private UserDB userDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        userDB = new UserDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response, false);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response, true);
    }

    private void handleRequest(HttpServletRequest request, HttpServletResponse response, boolean isPost)
            throws ServletException, IOException {
        User adminUser = getLoggedInAdminUser(request, response);
        if (adminUser == null) {
            return;
        }

        String servletPath = request.getServletPath();
        String targetJsp;
        String profilePageMode;

        if ("/admin/profile/view".equals(servletPath)) {
            targetJsp = "/admin/profile/view.jsp";
            profilePageMode = "view";
        } else if ("/admin/profile/edit".equals(servletPath)) {
            targetJsp = "/admin/profile/edit.jsp";
            profilePageMode = "edit";
        } else if ("/admin/profile/change-password".equals(servletPath)) {
            targetJsp = "/admin/profile/change-password.jsp";
            profilePageMode = "change-password";
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        User profileUser;
        try {
            profileUser = userDB.findUserById(adminUser.getUserId());
        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load the admin profile record.");
            ex.printStackTrace();
            return;
        }

        if (profileUser == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Admin profile record not found.");
            return;
        }

        String flashMessage = null;
        String flashType = null;

        if (isPost && "/admin/profile/edit".equals(servletPath)) {
            String fullName = normalize(request.getParameter("fullName"));
            String email = normalize(request.getParameter("email"));
            String phone = normalize(request.getParameter("phone"));
            LocalDate dateOfBirth = parseDate(request.getParameter("dateOfBirth"));
            String gender = normalize(request.getParameter("gender"));

            if (fullName == null) {
                flashMessage = "Full name is required.";
                flashType = "error";
            } else {
                try {
                    boolean updated = userDB.updateProfile(adminUser.getUserId(), fullName, email, phone, dateOfBirth, gender);
                    if (updated) {
                        profileUser = userDB.findUserById(adminUser.getUserId());
                        syncSessionUser(request, profileUser);
                        flashMessage = "Profile updated successfully.";
                        flashType = "success";
                    } else {
                        flashMessage = "No profile changes were saved.";
                        flashType = "error";
                    }
                } catch (Exception ex) {
                    flashMessage = "Unable to update the profile record.";
                    flashType = "error";
                    ex.printStackTrace();
                }
            }

            if (flashType != null && "error".equalsIgnoreCase(flashType)) {
                profileUser.setFullName(fullName == null ? profileUser.getFullName() : fullName);
                profileUser.setEmail(email);
                profileUser.setPhone(phone);
                profileUser.setDateOfBirth(dateOfBirth);
                profileUser.setGender(gender == null ? null : gender.toUpperCase());
            }
        } else if (isPost && "/admin/profile/change-password".equals(servletPath)) {
            String currentPassword = normalize(request.getParameter("currentPassword"));
            String newPassword = normalize(request.getParameter("newPassword"));
            String confirmPassword = normalize(request.getParameter("confirmPassword"));

            if (currentPassword == null || newPassword == null || confirmPassword == null) {
                flashMessage = "Please complete all password fields.";
                flashType = "error";
            } else if (newPassword.length() < 8) {
                flashMessage = "New password must be at least 8 characters long.";
                flashType = "error";
            } else if (!newPassword.equals(confirmPassword)) {
                flashMessage = "New password and confirmation do not match.";
                flashType = "error";
            } else {
                try {
                    boolean updated = userDB.updatePassword(adminUser.getUserId(), currentPassword, newPassword);
                    if (updated) {
                        flashMessage = "Password updated successfully.";
                        flashType = "success";
                    } else {
                        flashMessage = "Current password is incorrect or password update failed.";
                        flashType = "error";
                    }
                } catch (Exception ex) {
                    flashMessage = "Unable to update the password.";
                    flashType = "error";
                    ex.printStackTrace();
                }
            }
        }

        request.setAttribute("profileUser", profileUser);
        request.setAttribute("profilePageMode", profilePageMode);
        request.setAttribute("activeAdminPath", servletPath);

        if (flashMessage != null) {
            request.setAttribute("flashMessage", flashMessage);
            request.setAttribute("flashType", flashType);
        }

        request.getRequestDispatcher(targetJsp).forward(request, response);
    }
}