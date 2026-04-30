package ict.servlet;

import ict.bean.User;
import ict.db.ClinicDB;
import ict.db.StaffProfileDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;

@WebServlet(urlPatterns = {"/staff/profile/view", "/staff/profile/edit", "/staff/profile/change-password"})
public class StaffProfileController extends HttpServlet {

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("d-M-yyyy");

    private StaffProfileDB profileDB;
    private ClinicDB clinicDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        profileDB = new StaffProfileDB(dbUrl, dbUser, dbPassword);
        clinicDB = new ClinicDB(dbUrl, dbUser, dbPassword);
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
        User staffUser = getLoggedInStaffUser(request, response);
        if (staffUser == null) {
            return;
        }

        HttpSession session = request.getSession(false);

        String servletPath = request.getServletPath();
        String targetJsp;
        String profilePageMode;

        if ("/staff/profile/view".equals(servletPath)) {
            targetJsp = "/staff/profile/view.jsp";
            profilePageMode = "view";
        } else if ("/staff/profile/edit".equals(servletPath)) {
            targetJsp = "/staff/profile/edit.jsp";
            profilePageMode = "edit";
        } else if ("/staff/profile/change-password".equals(servletPath)) {
            targetJsp = "/staff/profile/change-password.jsp";
            profilePageMode = "change-password";
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        User profileUser;
        try {
            profileUser = profileDB.findStaffProfileByUserId(staffUser.getUserId(), staffUser.getClinicId());
        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load profile record from the database.");
            ex.printStackTrace();
            return;
        }

        if (profileUser == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Profile record not found for the logged-in staff user.");
            return;
        }

        String flashMessage = null;
        String flashType = null;

        if (isPost && "/staff/profile/edit".equals(servletPath)) {
            String fullName = normalize(request.getParameter("fullName"));
            String email = normalize(request.getParameter("email"));
            String phone = normalize(request.getParameter("phone"));
            String dateOfBirthText = normalize(request.getParameter("dateOfBirth"));
            String gender = normalize(request.getParameter("gender"));

            LocalDate dateOfBirth = parseDate(dateOfBirthText);
            boolean validationFailed = false;

            if (fullName == null) {
                flashMessage = "Full name is required.";
                flashType = "error";
                validationFailed = true;
            } else if (email == null && phone == null) {
                flashMessage = "At least one contact method is required.";
                flashType = "error";
                validationFailed = true;
            } else if (gender != null
                    && !"MALE".equalsIgnoreCase(gender)
                    && !"FEMALE".equalsIgnoreCase(gender)) {
                flashMessage = "Gender must be either MALE or FEMALE.";
                flashType = "error";
                validationFailed = true;
            }

            if (!validationFailed) {
                try {
                    boolean updated = profileDB.updateStaffProfile(
                            staffUser.getUserId(),
                            staffUser.getClinicId(),
                            fullName,
                            email,
                            phone,
                            dateOfBirth,
                            gender == null ? null : gender.toUpperCase());

                    if (updated) {
                        flashMessage = "Profile updated successfully.";
                        flashType = "success";
                        User refreshedProfile = profileDB.findStaffProfileByUserId(staffUser.getUserId(), staffUser.getClinicId());
                        if (refreshedProfile != null) {
                            profileUser = refreshedProfile;
                            syncSessionUser(session, profileUser);
                        }
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

            if (!"success".equalsIgnoreCase(flashType)) {
                profileUser.setFullName(fullName == null ? profileUser.getFullName() : fullName);
                profileUser.setEmail(email);
                profileUser.setPhone(phone);
                profileUser.setDateOfBirth(dateOfBirth);
                profileUser.setGender(gender == null ? null : gender.toUpperCase());
            }
        } else if (isPost && "/staff/profile/change-password".equals(servletPath)) {
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
                    boolean updated = profileDB.changePassword(
                            staffUser.getUserId(),
                            staffUser.getClinicId(),
                            currentPassword,
                            newPassword);

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
        request.setAttribute("assignedClinicId", staffUser.getClinicId());
        request.setAttribute("assignedClinicName", findClinicName(staffUser.getClinicId()));
        request.setAttribute("activeStaffPath", servletPath);

        if (flashMessage != null) {
            request.setAttribute("flashMessage", flashMessage);
            request.setAttribute("flashType", flashType);
        }

        request.getRequestDispatcher(targetJsp).forward(request, response);
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

    private void syncSessionUser(HttpSession session, User profileUser) {
        if (session == null || profileUser == null) {
            return;
        }

        Object sessionUser = session.getAttribute("userInfo");
        if (sessionUser instanceof User) {
            User loggedInUser = (User) sessionUser;
            loggedInUser.setFullName(profileUser.getFullName());
            loggedInUser.setEmail(profileUser.getEmail());
            loggedInUser.setPhone(profileUser.getPhone());
            loggedInUser.setDateOfBirth(profileUser.getDateOfBirth());
            loggedInUser.setGender(profileUser.getGender());
            session.setAttribute("userInfo", loggedInUser);
        }
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

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(value.trim(), DISPLAY_DATE_FORMAT);
            } catch (DateTimeParseException secondEx) {
                return null;
            }
        }
    }
}