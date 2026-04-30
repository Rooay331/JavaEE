package ict.servlet;

import ict.bean.BatchImportResult;
import ict.bean.User;
import ict.db.ClinicDB;
import ict.db.ClinicServiceDB;
import ict.db.ServiceDB;
import ict.db.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@MultipartConfig
@WebServlet(urlPatterns = {"/admin/batch/import", "/admin/batch/history"})
public class AdminBatchController extends AdminControllerSupport {

    private UserDB userDB;
    private ClinicDB clinicDB;
    private ServiceDB serviceDB;
    private ClinicServiceDB clinicServiceDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        userDB = new UserDB(dbUrl, dbUser, dbPassword);
        clinicDB = new ClinicDB(dbUrl, dbUser, dbPassword);
        serviceDB = new ServiceDB(dbUrl, dbUser, dbPassword);
        clinicServiceDB = new ClinicServiceDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (getLoggedInAdminUser(request, response) == null) {
            return;
        }

        String servletPath = request.getServletPath();
        try {
            request.setAttribute("activeClinics", clinicServiceDB.findActiveClinics());
        } catch (Exception ex) {
            request.setAttribute("activeClinics", Collections.emptyList());
            request.setAttribute("importError", "Unable to load the clinic reference list.");
            ex.printStackTrace();
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object history = session.getAttribute("batchImportHistory");
            if (history instanceof List) {
                List<BatchImportResult> storedHistory = new ArrayList<>();
                for (Object item : (List<?>) history) {
                    if (item instanceof BatchImportResult) {
                        storedHistory.add((BatchImportResult) item);
                    }
                }

                request.setAttribute("batchImportHistory", storedHistory);
                request.setAttribute("importResults", storedHistory);
                request.setAttribute("importSummaryTotal", storedHistory.size());
                request.setAttribute("importSummarySuccess", countSuccess(storedHistory));
                request.setAttribute("importSummaryFailed", countFailed(storedHistory));
            }
        }

        request.setAttribute("activeAdminPath", servletPath);
        request.getRequestDispatcher(resolveView(servletPath)).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (getLoggedInAdminUser(request, response) == null) {
            return;
        }

        String servletPath = request.getServletPath();
        if (!servletPath.endsWith("/import") && !servletPath.endsWith("/import.html")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        List<BatchImportResult> results = new ArrayList<>();
        try {
            request.setAttribute("activeClinics", clinicServiceDB.findActiveClinics());

            String importType = normalize(request.getParameter("importType"));
            String mode = normalize(request.getParameter("mode"));
            if (importType == null) {
                request.setAttribute("importError", "Choose what you want to import.");
                forwardToImport(request, response);
                return;
            }

            Part csvFile = request.getPart("csvFile");
            if (csvFile == null || csvFile.getSize() == 0) {
                request.setAttribute("importError", "Choose a CSV file before importing.");
                forwardToImport(request, response);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int rowNumber = 0;
                while ((line = reader.readLine()) != null) {
                    rowNumber++;
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    List<String> columns = parseCsvLine(line);
                    if (rowNumber == 1 && looksLikeHeaderRow(columns, importType)) {
                        continue;
                    }

                    results.add(processRow(importType, mode, columns, rowNumber));
                }
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("batchImportHistory", results);

            request.setAttribute("importResults", results);
            request.setAttribute("importSummaryTotal", results.size());
            request.setAttribute("importSummarySuccess", countSuccess(results));
            request.setAttribute("importSummaryFailed", countFailed(results));
            request.setAttribute("importMessage", "Processed " + results.size() + " rows.");
        } catch (Exception ex) {
            request.setAttribute("importError", "Unable to process the CSV file.");
            ex.printStackTrace();
        }

        request.setAttribute("activeAdminPath", servletPath);
        forwardToImport(request, response);
    }

    private String resolveView(String servletPath) {
        if (servletPath != null && (servletPath.endsWith("/history") || servletPath.endsWith("/history.html"))) {
            return "/admin/batch/history.jsp";
        }
        return "/admin/batch/import.jsp";
    }

    private void forwardToImport(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/admin/batch/import.jsp").forward(request, response);
    }

    private BatchImportResult processRow(String importType, String mode, List<String> columns, int rowNumber) {
        String normalizedMode = mode == null ? "UPSERT" : mode.trim().toUpperCase();
        try {
            switch (importType.toLowerCase()) {
                case "users":
                    return processUserRow(normalizedMode, columns, rowNumber);
                case "services":
                    return processServiceRow(normalizedMode, columns, rowNumber);
                case "timeslots":
                    return processTimeslotRow(normalizedMode, columns, rowNumber);
                default:
                    return errorResult("IMPORT", "Row " + rowNumber, rowNumber, "Unsupported import type.");
            }
        } catch (Exception ex) {
            return errorResult(importType.toUpperCase(), "Row " + rowNumber, rowNumber, ex.getMessage() == null ? "Import failed." : ex.getMessage());
        }
    }

    private BatchImportResult processUserRow(String mode, List<String> columns, int rowNumber) throws Exception {
        Integer userId = parseColumnInteger(columns, 0);
        String fullName = getColumn(columns, 1);
        String email = getColumn(columns, 2);
        String phone = getColumn(columns, 3);
        String role = normalize(getColumn(columns, 4));
        String status = normalize(getColumn(columns, 5));
        String clinicRef = getColumn(columns, 6);
        LocalDate dateOfBirth = parseDate(getColumn(columns, 7));
        String gender = normalize(getColumn(columns, 8));
        String password = getColumn(columns, 9);

        if (fullName == null || email == null || role == null || status == null) {
            return errorResult("USER", email == null ? fullName : email, rowNumber, "Full name, email, role, and status are required.");
        }

        User existing = userId != null ? userDB.findUserById(userId) : userDB.findUserByEmail(email);
        if (existing != null && "INSERT".equals(mode)) {
            return errorResult("USER", userCode(existing.getUserId()), rowNumber, "User already exists.");
        }

        User draft = existing == null ? new User() : existing;
        if (userId != null) {
            draft.setUserId(userId);
        }
        draft.setFullName(fullName);
        draft.setEmail(email);
        draft.setPhone(phone);
        draft.setRole(role.toUpperCase());
        draft.setDateOfBirth(dateOfBirth);
        draft.setGender(gender == null ? null : gender.toUpperCase());
        draft.setIsActive("ACTIVE".equalsIgnoreCase(status) ? 1 : 0);

        Integer clinicId = resolveClinicRef(clinicRef);
        if ("STAFF".equalsIgnoreCase(role)) {
            if (clinicId == null) {
                return errorResult("USER", email, rowNumber, "Staff users need a clinic reference.");
            }
            draft.setClinicId(clinicId);
        } else {
            draft.setClinicId(null);
        }

        if (existing == null) {
            draft.setPassword(password == null || password.trim().isEmpty() ? "Temp@1234!" : password.trim());
            boolean created = userDB.createUser(draft);
            if (!created) {
                return errorResult("USER", email, rowNumber, "Unable to create the user record.");
            }

            User createdUser = userDB.findUserByEmail(email);
            if (createdUser != null) {
                draft.setUserId(createdUser.getUserId());
            }

            return successResult("USER", createdUser == null ? email : userCode(createdUser.getUserId()), rowNumber, "Created", "User imported successfully.");
        }

        draft.setPassword(existing.getPassword());
        boolean updated = userDB.updateUser(draft);
        if (!updated) {
            return errorResult("USER", userCode(existing.getUserId()), rowNumber, "Unable to update the user record.");
        }

        if (password != null && !password.trim().isEmpty()) {
            userDB.resetPassword(draft.getUserId(), password.trim());
        }

        return successResult("USER", userCode(draft.getUserId()), rowNumber, "Updated", "User imported successfully.");
    }

    private BatchImportResult processServiceRow(String mode, List<String> columns, int rowNumber) throws Exception {
        Integer serviceId = parseColumnInteger(columns, 0);
        String serviceName = getColumn(columns, 1);
        String description = getColumn(columns, 2);
        Integer avgMinutes = parseColumnInteger(columns, 3);
        boolean walkInEnabled = parseBoolean(getColumn(columns, 4));
        boolean requiresApproval = parseBoolean(getColumn(columns, 5));
        String status = normalize(getColumn(columns, 6));

        if (serviceName == null) {
            return errorResult("SERVICE", "Row " + rowNumber, rowNumber, "Service name is required.");
        }

        Integer existingId = serviceId != null ? serviceId : serviceDB.findServiceIdByName(serviceName);
        if (existingId != null && "INSERT".equals(mode)) {
            return errorResult("SERVICE", serviceName, rowNumber, "Service already exists.");
        }

        Integer savedServiceId = serviceDB.saveServiceRecord(serviceId, serviceName, description, avgMinutes, walkInEnabled, requiresApproval, status == null || "ACTIVE".equalsIgnoreCase(status));
        if (savedServiceId == null) {
            return errorResult("SERVICE", serviceName, rowNumber, "Unable to save the service record.");
        }

        return successResult("SERVICE", "S-" + savedServiceId, rowNumber, existingId == null ? "Created" : "Updated", "Service imported successfully.");
    }

    private BatchImportResult processTimeslotRow(String mode, List<String> columns, int rowNumber) throws Exception {
        String clinicRef = getColumn(columns, 0);
        String serviceRef = getColumn(columns, 1);
        LocalDate slotDate = parseDate(getColumn(columns, 2));
        LocalTime startTime = parseTime(getColumn(columns, 3));
        LocalTime endTime = parseTime(getColumn(columns, 4));
        Integer capacity = parseColumnInteger(columns, 5);

        if (slotDate == null || startTime == null || endTime == null) {
            return errorResult("TIMESLOT", "Row " + rowNumber, rowNumber, "Date, start time, and end time are required.");
        }

        Integer clinicId = resolveClinicRef(clinicRef);
        Integer serviceId = resolveServiceRef(serviceRef);
        if (clinicId == null || serviceId == null) {
            return errorResult("TIMESLOT", clinicRef + " / " + serviceRef, rowNumber, "Clinic or service could not be resolved.");
        }

        boolean capacitySaved = serviceDB.upsertClinicServiceCapacity(clinicId, serviceId, capacity);
        boolean hoursSaved = serviceDB.upsertOpeningHour(serviceId, slotDate.getDayOfWeek().getValue() - 1, startTime, endTime);

        if (!capacitySaved || !hoursSaved) {
            return errorResult("TIMESLOT", clinicRef + " / " + serviceRef, rowNumber, "Unable to save the timeslot record.");
        }

        return successResult("TIMESLOT", clinicRef + " / " + serviceRef, rowNumber, "Updated", "Timeslot imported successfully.");
    }

    private BatchImportResult successResult(String entityType, String identifier, int rowNumber, String action, String message) {
        BatchImportResult result = new BatchImportResult();
        result.setEntityType(entityType);
        result.setIdentifier(identifier);
        result.setRowNumber(rowNumber);
        result.setAction(action);
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    private BatchImportResult errorResult(String entityType, String identifier, int rowNumber, String message) {
        BatchImportResult result = new BatchImportResult();
        result.setEntityType(entityType);
        result.setIdentifier(identifier);
        result.setRowNumber(rowNumber);
        result.setAction("Failed");
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    private Integer resolveClinicRef(String ref) throws Exception {
        Integer clinicId = parseInteger(ref);
        if (clinicId != null) {
            return clinicDB.findClinicNameById(clinicId) == null ? null : clinicId;
        }
        return clinicDB.findClinicIdByName(ref);
    }

    private Integer resolveServiceRef(String ref) throws Exception {
        Integer serviceId = parseInteger(ref);
        if (serviceId != null) {
            return serviceId;
        }
        return serviceDB.findServiceIdByName(ref);
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (currentChar == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private boolean looksLikeHeaderRow(List<String> columns, String importType) {
        if (columns == null || columns.isEmpty() || importType == null) {
            return false;
        }

        String row = String.join(" ", columns).toLowerCase();
        switch (importType.toLowerCase()) {
            case "users":
                return row.contains("user_id") || row.contains("full_name") || row.contains("email") || row.contains("phone") || row.contains("date_of_birth") || row.contains("gender") || row.contains("password");
            case "services":
                return row.contains("service_id") || row.contains("service_name") || row.contains("avg_minutes") || row.contains("walkin_enabled") || row.contains("approval_required");
            case "timeslots":
                return row.contains("clinic_ref") || row.contains("clinic_id") || row.contains("service_ref") || row.contains("service_id") || row.contains("start_time") || row.contains("end_time") || row.contains("capacity");
            default:
                return false;
        }
    }

    private Integer parseColumnInteger(List<String> columns, int index) {
        if (columns == null || index < 0 || index >= columns.size()) {
            return null;
        }

        return parseInteger(columns.get(index));
    }

    private String getColumn(List<String> columns, int index) {
        if (columns == null || index < 0 || index >= columns.size()) {
            return null;
        }

        String value = columns.get(index);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private int countSuccess(List<BatchImportResult> results) {
        int count = 0;
        if (results == null) {
            return count;
        }

        for (BatchImportResult result : results) {
            if (result != null && result.isSuccess()) {
                count++;
            }
        }

        return count;
    }

    private int countFailed(List<BatchImportResult> results) {
        int count = 0;
        if (results == null) {
            return count;
        }

        for (BatchImportResult result : results) {
            if (result != null && !result.isSuccess()) {
                count++;
            }
        }

        return count;
    }

    private String userCode(Integer userId) {
        return userId == null ? "-" : "U-" + userId;
    }
}