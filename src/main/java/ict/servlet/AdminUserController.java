package ict.servlet;

import ict.bean.ClinicService;
import ict.bean.User;
import ict.db.ClinicServiceDB;
import ict.db.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/admin/users/list", "/admin/users/create", "/admin/users/edit", "/admin/users/delete", "/admin/users/reset-password"})
public class AdminUserController extends AdminControllerSupport {

    private static final int PAGE_SIZE = 10;

    private UserDB userDB;
    private ClinicServiceDB clinicServiceDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        userDB = new UserDB(dbUrl, dbUser, dbPassword);
        clinicServiceDB = new ClinicServiceDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User adminUser = getLoggedInAdminUser(request, response);
        if (adminUser == null) {
            return;
        }

        String servletPath = request.getServletPath();
        if ("/admin/users/create".equals(servletPath)) {
            showForm(request, response, new User(), "create", null, null);
            return;
        }

        if ("/admin/users/edit".equals(servletPath)) {
            Integer userId = parseInteger(request.getParameter("userId"));
            if (userId == null) {
                showList(request, response, "Please choose a user to edit.", "error");
                return;
            }

            try {
                User editUser = userDB.findUserById(userId);
                if (editUser == null) {
                    showList(request, response, "User record not found.", "error");
                    return;
                }

                showForm(request, response, editUser, "edit", null, null);
                return;
            } catch (Exception ex) {
                showList(request, response, "Unable to load the selected user.", "error");
                ex.printStackTrace();
                return;
            }
        }

        showList(request, response, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User adminUser = getLoggedInAdminUser(request, response);
        if (adminUser == null) {
            return;
        }

        String servletPath = request.getServletPath();
        if ("/admin/users/create".equals(servletPath)) {
            handleCreate(request, response);
            return;
        }
        if ("/admin/users/edit".equals(servletPath)) {
            handleUpdate(request, response);
            return;
        }
        if ("/admin/users/delete".equals(servletPath)) {
            handleDelete(request, response);
            return;
        }
        if ("/admin/users/reset-password".equals(servletPath)) {
            handleResetPassword(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fullName = normalize(request.getParameter("fullName"));
        String email = normalize(request.getParameter("email"));
        String phone = normalize(request.getParameter("phone"));
        LocalDate dateOfBirth = parseDate(request.getParameter("dateOfBirth"));
        String gender = normalize(request.getParameter("gender"));
        String role = normalize(request.getParameter("role"));
        Integer clinicId = parseInteger(request.getParameter("clinicId"));
        String password = normalize(request.getParameter("password"));

        User draft = buildDraftUser(null, role, fullName, email, phone, dateOfBirth, gender, clinicId, password, request.getParameter("isActive"));
        String errorMessage = validateUserDraft(draft, true);
        if (errorMessage != null) {
            showForm(request, response, draft, "create", errorMessage, "error");
            return;
        }

        try {
            if (userDB.createUser(draft)) {
                showList(request, response, "User created successfully.", "success");
            } else {
                showForm(request, response, draft, "create", "Unable to create the user record.", "error");
            }
        } catch (Exception ex) {
            showForm(request, response, draft, "create", "Unable to create the user record.", "error");
            ex.printStackTrace();
        }
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer userId = parseInteger(request.getParameter("userId"));
        String fullName = normalize(request.getParameter("fullName"));
        String email = normalize(request.getParameter("email"));
        String phone = normalize(request.getParameter("phone"));
        LocalDate dateOfBirth = parseDate(request.getParameter("dateOfBirth"));
        String gender = normalize(request.getParameter("gender"));
        String role = normalize(request.getParameter("role"));
        Integer clinicId = parseInteger(request.getParameter("clinicId"));
        String isActiveValue = request.getParameter("isActive");

        if (userId == null) {
            showList(request, response, "Please choose a valid user to update.", "error");
            return;
        }

        try {
            User existing = userDB.findUserById(userId);
            if (existing == null) {
                showList(request, response, "User record not found.", "error");
                return;
            }

            User draft = buildDraftUser(userId, role == null ? existing.getRole() : role, fullName, email, phone, dateOfBirth, gender, clinicId, existing.getPassword(), isActiveValue);
            String errorMessage = validateUserDraft(draft, false);
            if (errorMessage != null) {
                showForm(request, response, draft, "edit", errorMessage, "error");
                return;
            }

            if (userDB.updateUser(draft)) {
                if (existing.getUserId() != null && existing.getUserId().equals(((User) request.getSession(false).getAttribute("userInfo")).getUserId())) {
                    syncSessionUser(request, draft);
                }
                showList(request, response, "User updated successfully.", "success");
            } else {
                showForm(request, response, draft, "edit", "Unable to update the user record.", "error");
            }
        } catch (Exception ex) {
            User draft = buildDraftUser(userId, role, fullName, email, phone, dateOfBirth, gender, clinicId, null, isActiveValue);
            showForm(request, response, draft, "edit", "Unable to update the user record.", "error");
            ex.printStackTrace();
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer userId = parseInteger(request.getParameter("userId"));
        if (userId == null) {
            showList(request, response, "Please choose a valid user to delete.", "error");
            return;
        }

        try {
            if (userDB.deleteUser(userId)) {
                showList(request, response, "User deactivated successfully.", "success");
            } else {
                showList(request, response, "Unable to deactivate the user.", "error");
            }
        } catch (Exception ex) {
            showList(request, response, "Unable to deactivate the user.", "error");
            ex.printStackTrace();
        }
    }

    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer userId = parseInteger(request.getParameter("userId"));
        String newPassword = normalize(request.getParameter("newPassword"));
        String confirmPassword = normalize(request.getParameter("confirmPassword"));

        if (userId == null) {
            showList(request, response, "Please choose a valid user to reset.", "error");
            return;
        }

        if (newPassword == null || newPassword.length() < 8) {
            showEditWithMessage(request, response, userId, "Reset password must contain at least 8 characters.", "error");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showEditWithMessage(request, response, userId, "Password confirmation does not match.", "error");
            return;
        }

        try {
            if (userDB.resetPassword(userId, newPassword)) {
                showEditWithMessage(request, response, userId, "Password reset successfully.", "success");
            } else {
                showEditWithMessage(request, response, userId, "Unable to reset the password.", "error");
            }
        } catch (Exception ex) {
            showEditWithMessage(request, response, userId, "Unable to reset the password.", "error");
            ex.printStackTrace();
        }
    }

    private void showEditWithMessage(HttpServletRequest request, HttpServletResponse response, Integer userId,
            String message, String type) throws ServletException, IOException {
        try {
            User editUser = userDB.findUserById(userId);
            if (editUser == null) {
                showList(request, response, "User record not found.", "error");
                return;
            }
            showForm(request, response, editUser, "edit", message, type);
        } catch (Exception ex) {
            showList(request, response, message, type);
            ex.printStackTrace();
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response, String flashMessage, String flashType)
            throws ServletException, IOException {
        String keyword = normalize(request.getParameter("q"));
        String role = normalize(request.getParameter("role"));
        String status = normalize(request.getParameter("status"));
        Integer pageNumber = parseInteger(request.getParameter("page"));
        int currentPage = pageNumber == null || pageNumber < 1 ? 1 : pageNumber;

        List<User> users = Collections.emptyList();
        int totalUsers = 0;
        List<ClinicService> clinics = Collections.emptyList();
        Map<Integer, String> clinicNamesById = new LinkedHashMap<>();

        try {
            totalUsers = userDB.countUsers(keyword, role, status);
            users = userDB.findUsers(keyword, role, status, (currentPage - 1) * PAGE_SIZE, PAGE_SIZE);
            clinics = clinicServiceDB.findActiveClinics();
            if (clinics != null) {
                for (ClinicService clinic : clinics) {
                    if (clinic != null && clinic.getClinicId() != null) {
                        clinicNamesById.put(clinic.getClinicId(), clinic.getClinicName());
                    }
                }
            }
        } catch (Exception ex) {
            flashMessage = "Unable to load the user list from the database.";
            flashType = "error";
            ex.printStackTrace();
        }

        request.setAttribute("users", users == null ? Collections.emptyList() : users);
        request.setAttribute("activeClinics", clinics == null ? Collections.emptyList() : clinics);
        request.setAttribute("clinicNamesById", clinicNamesById);
        request.setAttribute("selectedKeyword", keyword);
        request.setAttribute("selectedRole", role);
        request.setAttribute("selectedStatus", status);
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("pageSize", PAGE_SIZE);
        request.setAttribute("totalUsers", totalUsers);
        request.setAttribute("totalPages", totalUsers <= 0 ? 0 : (int) Math.ceil(totalUsers / (double) PAGE_SIZE));
        request.setAttribute("flashMessage", flashMessage);
        request.setAttribute("flashType", flashType);
        request.setAttribute("activeAdminPath", "/admin/users/list");
        request.getRequestDispatcher("/admin/users/list.jsp").forward(request, response);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, User user, String mode,
            String flashMessage, String flashType) throws ServletException, IOException {
        List<ClinicService> clinics = Collections.emptyList();
        Map<Integer, String> clinicNamesById = new LinkedHashMap<>();
        try {
            clinics = clinicServiceDB.findActiveClinics();
            if (clinics != null) {
                for (ClinicService clinic : clinics) {
                    if (clinic != null && clinic.getClinicId() != null) {
                        clinicNamesById.put(clinic.getClinicId(), clinic.getClinicName());
                    }
                }
            }
        } catch (Exception ex) {
            flashMessage = flashMessage == null ? "Unable to load clinic options." : flashMessage;
            flashType = flashType == null ? "error" : flashType;
            ex.printStackTrace();
        }

        request.setAttribute("editUser", user == null ? new User() : user);
        request.setAttribute("activeClinics", clinics == null ? Collections.emptyList() : clinics);
        request.setAttribute("clinicNamesById", clinicNamesById);
        request.setAttribute("userFormMode", mode);
        request.setAttribute("flashMessage", flashMessage);
        request.setAttribute("flashType", flashType);
        request.setAttribute("activeAdminPath", "/admin/users/" + mode);
        request.getRequestDispatcher("/admin/users/" + mode + ".jsp").forward(request, response);
    }

    private User buildDraftUser(Integer userId, String role, String fullName, String email, String phone, LocalDate dateOfBirth,
            String gender, Integer clinicId, String password, String activeValue) {
        User user = new User();
        user.setUserId(userId);
        user.setRole(role == null ? null : role.trim().toUpperCase());
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setDateOfBirth(dateOfBirth);
        user.setGender(gender == null ? null : gender.trim().toUpperCase());
        user.setPassword(password);
        user.setClinicId("STAFF".equalsIgnoreCase(user.getRole()) ? clinicId : null);
        user.setIsActive("0".equals(activeValue) ? 0 : 1);
        return user;
    }

    private String validateUserDraft(User user, boolean requirePassword) {
        if (user.getFullName() == null) {
            return "Full name is required.";
        }
        if (user.getRole() == null) {
            return "Role is required.";
        }
        if (requirePassword && (user.getPassword() == null || user.getPassword().length() < 8)) {
            return "Temporary password must contain at least 8 characters.";
        }
        if ("STAFF".equalsIgnoreCase(user.getRole()) && user.getClinicId() == null) {
            return "Staff users require an assigned clinic.";
        }
        if (!"STAFF".equalsIgnoreCase(user.getRole()) && user.getClinicId() != null) {
            user.setClinicId(null);
        }
        return null;
    }
}