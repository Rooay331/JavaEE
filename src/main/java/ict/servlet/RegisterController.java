package ict.servlet;

import ict.db.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


@WebServlet(urlPatterns = {"/register"})
public class RegisterController extends HttpServlet {

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("d-M-yyyy");
    
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
        
        request.getRequestDispatcher("/register.jsp").forward(request, response);
        
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {        
        String fullName = request.getParameter("fullName");
        String dobStr = request.getParameter("dob");
        String gender = request.getParameter("gender");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String agree = request.getParameter("agree");
        
        // Server-side validation
        String errorMessage = null;
        String fullNameError = null;
        String dobError = null;
        String genderError = null;
        String emailError = null;
        String phoneError = null;
        String passwordError = null;
        String confirmPasswordError = null;
        String agreeError = null;
        
        if (fullName == null || fullName.trim().isEmpty()) {
            fullNameError = "Please enter your full name.";
        }
        if (dobStr == null || dobStr.trim().isEmpty()) {
            dobError = "Please enter your date of birth.";
        } else if (parseDate(dobStr) == null) {
            dobError = "Please enter a valid date of birth in 12-6-2026 format.";
        }
        // Gender is optional
        if (email == null || email.trim().isEmpty()) {
            emailError = "Please enter your email address.";
        } else if (!email.contains("@")) {
            emailError = "Please enter a valid email address.";
        }
        if (phone == null || phone.trim().isEmpty()) {
            phoneError = "Please enter your mobile number.";
        } else if (phone.replaceAll("\\D", "").length() != 8) {
            phoneError = "The mobile number must be exactly 8 digits.";
        }
        if (password == null || password.length() < 8) {
            passwordError = "Password must be at least 8 characters.";
        }
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            confirmPasswordError = "Confirm password does not match.";
        }
        if (!"1".equals(agree)) {
            agreeError = "You must agree to the account and data handling terms.";
        }
        
        if (fullNameError != null || dobError != null || emailError != null || phoneError != null || passwordError != null || confirmPasswordError != null || agreeError != null) {
            errorMessage = "Please correct the errors below.";
        }
        
        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            request.setAttribute("fullName", fullName);
            request.setAttribute("email", email);
            request.setAttribute("phone", phone);
            request.setAttribute("dateOfBirth", dobStr);
            request.setAttribute("gender", gender);
            request.setAttribute("fullNameError", fullNameError);
            request.setAttribute("dobError", dobError);
            request.setAttribute("genderError", genderError);
            request.setAttribute("emailError", emailError);
            request.setAttribute("phoneError", phoneError);
            request.setAttribute("passwordError", passwordError);
            request.setAttribute("confirmPasswordError", confirmPasswordError);
            request.setAttribute("agreeError", agreeError);
            request.setAttribute("agreed", agree);
            request.getRequestDispatcher("/register.jsp").forward(request, response);
            return;
        }
        
        try {
            Date dob = Date.valueOf(parseDate(dobStr));
            if (db.doRegister(fullName, email, phone, password, "PATIENT", dob, gender)) {
                response.sendRedirect("login");
            } else {
                request.setAttribute("errorMessage", "Registration failed. Please try again.");
                request.getRequestDispatcher("/register.jsp").forward(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "An error occurred during registration.");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
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
