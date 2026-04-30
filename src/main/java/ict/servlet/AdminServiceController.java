package ict.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import ict.bean.ClinicServiceStatus;
import ict.db.ServiceDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = {"/admin/services/list", "/admin/services/list.html", "/admin/services/add", "/admin/services/add.html", "/admin/services/edit", "/admin/services/edit.html", "/admin/services/delete", "/admin/services/delete.html"})
public class AdminServiceController extends AdminControllerSupport {

    private ServiceDB serviceDB;

    @Override
    public void init() {
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        serviceDB = new ServiceDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (getLoggedInAdminUser(request, response) == null) {
            return;
        }

        String servletPath = request.getServletPath();

        if (isAddPath(servletPath)) {
            showForm(request, response, new ClinicServiceStatus(), "add", "/admin/services/add", null, null);
            return;
        }

        if (isEditPath(servletPath)) {
            Integer serviceId = parseInteger(request.getParameter("serviceId"));
            if (serviceId == null) {
                showList(request, response, "Please choose a service to edit.", "error");
                return;
            }

            try {
                ClinicServiceStatus editService = serviceDB.findServiceRecordById(serviceId);
                if (editService == null) {
                    showList(request, response, "Service record not found.", "error");
                    return;
                }

                showForm(request, response, editService, "edit", "/admin/services/edit", null, null);
                return;
            } catch (SQLException ex) {
                showList(request, response, "Unable to load the selected service.", "error");
                getServletContext().log("Unable to load the selected service.", ex);
                return;
            }
        }

        if (isDeletePath(servletPath)) {
            Integer serviceId = parseInteger(request.getParameter("serviceId"));
            if (serviceId == null) {
                showList(request, response, "Please choose a service to deactivate.", "error");
                return;
            }

            try {
                ClinicServiceStatus deleteService = serviceDB.findServiceRecordById(serviceId);
                if (deleteService == null) {
                    showList(request, response, "Service record not found.", "error");
                    return;
                }

                showForm(request, response, deleteService, "delete", "/admin/services/delete", null, null);
                return;
            } catch (SQLException ex) {
                showList(request, response, "Unable to load the selected service.", "error");
                getServletContext().log("Unable to load the selected service.", ex);
                return;
            }
        }

        showList(request, response, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (getLoggedInAdminUser(request, response) == null) {
            return;
        }

        String servletPath = request.getServletPath();

        if (isAddPath(servletPath)) {
            handleSave(request, response, null, "add");
            return;
        }

        if (isEditPath(servletPath)) {
            handleSave(request, response, parseInteger(request.getParameter("serviceId")), "edit");
            return;
        }

        if (isDeletePath(servletPath)) {
            handleDelete(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleSave(HttpServletRequest request, HttpServletResponse response, Integer serviceId, String mode)
            throws ServletException, IOException {
        String serviceName = normalize(request.getParameter("serviceName"));
        String description = normalize(request.getParameter("description"));
        Integer avgServiceMinutes = parseInteger(request.getParameter("avgServiceMinutes"));
        boolean walkInEnabled = parseBoolean(request.getParameter("walkInEnabled"));
        boolean requiresApproval = parseBoolean(request.getParameter("requiresApproval"));
        boolean active = parseBoolean(request.getParameter("isActive"));

        if (serviceId != null) {
            try {
                if (serviceDB.findServiceRecordById(serviceId) == null) {
                    showList(request, response, "Service record not found.", "error");
                    return;
                }
            } catch (SQLException ex) {
                showList(request, response, "Unable to load the selected service.", "error");
                getServletContext().log("Unable to load the selected service.", ex);
                return;
            }
        }

        ClinicServiceStatus draft = buildDraftService(serviceId, serviceName, description, avgServiceMinutes,
                walkInEnabled, requiresApproval, active);

        String validationError = validateServiceDraft(draft);
        if (validationError != null) {
            showForm(request, response, draft, mode, "/admin/services/" + mode, validationError, "error");
            return;
        }

        try {
            Integer savedServiceId = serviceDB.saveServiceRecord(serviceId, draft.getServiceName(), draft.getServiceDescription(),
                    draft.getAvgServiceMinutes(), draft.isWalkInEnabled(), draft.isRequiresApproval(), draft.isActive());

            if (savedServiceId != null) {
                showList(request, response, "Service saved successfully.", "success");
            } else {
                showForm(request, response, draft, mode, "/admin/services/" + mode, "Unable to save the service record.", "error");
            }
        } catch (SQLException ex) {
            showForm(request, response, draft, mode, "/admin/services/" + mode, "Unable to save the service record.", "error");
            getServletContext().log("Unable to save the service record.", ex);
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer serviceId = parseInteger(request.getParameter("serviceId"));
        if (serviceId == null) {
            showList(request, response, "Please choose a valid service to deactivate.", "error");
            return;
        }

        try {
            ClinicServiceStatus service = serviceDB.findServiceRecordById(serviceId);
            if (service == null) {
                showList(request, response, "Service record not found.", "error");
                return;
            }

            if (serviceDB.setServiceActive(serviceId, false)) {
                showList(request, response, "Service deactivated successfully.", "success");
            } else {
                showList(request, response, "Unable to deactivate the service.", "error");
            }
        } catch (SQLException ex) {
            showList(request, response, "Unable to deactivate the service.", "error");
            getServletContext().log("Unable to deactivate the service.", ex);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response, String flashMessage, String flashType)
            throws ServletException, IOException {
        List<ClinicServiceStatus> serviceRecords;
        int activeServiceCount = 0;
        int inactiveServiceCount = 0;
        int totalServiceMinutes = 0;

        try {
            serviceRecords = serviceDB.findServiceCatalog();
            if (serviceRecords != null) {
                for (ClinicServiceStatus service : serviceRecords) {
                    if (service == null) {
                        continue;
                    }
                    if (service.isActive()) {
                        activeServiceCount++;
                    } else {
                        inactiveServiceCount++;
                    }
                    totalServiceMinutes += Math.max(service.getAvgServiceMinutes(), 0);
                }
            }
        } catch (SQLException ex) {
            serviceRecords = Collections.emptyList();
            flashMessage = "Unable to load the service list from the database.";
            flashType = "error";
            getServletContext().log("Unable to load the service list from the database.", ex);
        }

        request.setAttribute("serviceRecords", serviceRecords);
        request.setAttribute("serviceCount", serviceRecords == null ? 0 : serviceRecords.size());
        request.setAttribute("activeServiceCount", activeServiceCount);
        request.setAttribute("inactiveServiceCount", inactiveServiceCount);
        request.setAttribute("averageServiceMinutes", serviceRecords == null || serviceRecords.isEmpty() ? 0 : Math.round(totalServiceMinutes / (float) serviceRecords.size()));
        request.setAttribute("selectedServiceId", null);
        request.setAttribute("flashMessage", flashMessage);
        request.setAttribute("flashType", flashType);
        request.setAttribute("activeAdminPath", "/admin/services/list");
        request.getRequestDispatcher("/admin/services/list.jsp").forward(request, response);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, ClinicServiceStatus service, String viewName,
            String activePath, String flashMessage, String flashType) throws ServletException, IOException {
        request.setAttribute("serviceForm", service == null ? new ClinicServiceStatus() : service);
        request.setAttribute("selectedServiceId", service == null ? null : service.getServiceId());
        request.setAttribute("flashMessage", flashMessage);
        request.setAttribute("flashType", flashType);
        request.setAttribute("activeAdminPath", activePath);
        request.getRequestDispatcher("/admin/services/" + viewName + ".jsp").forward(request, response);
    }

    private ClinicServiceStatus buildDraftService(Integer serviceId, String serviceName, String description, Integer avgServiceMinutes,
            boolean walkInEnabled, boolean requiresApproval, boolean active) {
        ClinicServiceStatus service = new ClinicServiceStatus();
        service.setServiceId(serviceId);
        service.setServiceName(serviceName);
        service.setServiceDescription(description);
        service.setAvgServiceMinutes(avgServiceMinutes == null ? 0 : avgServiceMinutes);
        service.setWalkInEnabled(walkInEnabled);
        service.setRequiresApproval(requiresApproval);
        service.setActive(active);
        return service;
    }

    private String validateServiceDraft(ClinicServiceStatus service) {
        if (service.getServiceName() == null || service.getServiceName().trim().isEmpty()) {
            return "Service name is required.";
        }

        if (service.getAvgServiceMinutes() <= 0) {
            return "Average service minutes must be greater than zero.";
        }

        return null;
    }

    private boolean isAddPath(String servletPath) {
        return "/admin/services/add".equals(servletPath) || "/admin/services/add.html".equals(servletPath);
    }

    private boolean isEditPath(String servletPath) {
        return "/admin/services/edit".equals(servletPath) || "/admin/services/edit.html".equals(servletPath);
    }

    private boolean isDeletePath(String servletPath) {
        return "/admin/services/delete".equals(servletPath) || "/admin/services/delete.html".equals(servletPath);
    }
}