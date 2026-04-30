package ict.db;

import ict.bean.ClinicService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.sql.Time;
import java.time.LocalDate;

public class ClinicServiceDB {

    private final String url;
    private final String username;
    private final String password;

    public ClinicServiceDB(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
        }
        return DriverManager.getConnection(url, username, password);
    }

    public ArrayList<ClinicService> findActiveClinics() throws SQLException {
        Map<Integer, ClinicService> clinics = new LinkedHashMap<>();

        String sql = "SELECT c.clinic_id, c.clinic_name, c.address, c.phone, c.is_active, s.service_name "
                + "FROM clinics c "
                + "LEFT JOIN clinic_services cs ON c.clinic_id = cs.clinic_id AND cs.is_active = 1 "
                + "LEFT JOIN services s ON cs.service_id = s.service_id AND s.is_active = 1 "
                + "WHERE c.is_active = 1 "
                + "ORDER BY c.clinic_name, s.service_name";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                int clinicId = rs.getInt("clinic_id");
                ClinicService clinic = clinics.get(clinicId);
                if (clinic == null) {
                    clinic = new ClinicService();
                    clinic.setClinicId(clinicId);
                    clinic.setClinicName(rs.getString("clinic_name"));
                    clinic.setAddress(rs.getString("address"));
                    clinic.setPhone(rs.getString("phone"));
                    clinic.setActive(rs.getInt("is_active") == 1);
                    clinic.setDistrict(deriveDistrict(clinic.getClinicName(), clinic.getAddress()));
                    clinics.put(clinicId, clinic);
                }

                String serviceName = rs.getString("service_name");
                if (serviceName != null && !serviceName.trim().isEmpty()) {
                    clinic.addService(serviceName);
                }
            }
        }

        return new ArrayList<>(clinics.values());
    }

    public String findServiceOpeningHoursLabel(Integer serviceId, LocalDate slotDate) throws SQLException {
        if (serviceId == null || slotDate == null) {
            return null;
        }

        String sql = "SELECT open_time, close_time FROM opening_hours WHERE service_id = ? AND day_of_week = ? ORDER BY open_time";
        StringBuilder label = new StringBuilder();

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, serviceId);
            statement.setInt(2, slotDate.getDayOfWeek().getValue() - 1);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    do {
                        Time openTime = rs.getTime("open_time");
                        Time closeTime = rs.getTime("close_time");
                        if (openTime == null || closeTime == null) {
                            continue;
                        }

                        if (label.length() > 0) {
                            label.append(", ");
                        }
                        label.append(openTime.toLocalTime()).append(" - ").append(closeTime.toLocalTime());
                    } while (rs.next());
                }
            }
        }

        return label.length() == 0 ? "Closed" : label.toString();
    }

    public ArrayList<ClinicService> allCinic() throws SQLException {
        return findActiveClinics();
    }

    private String deriveDistrict(String clinicName, String address) {
        String fromName = stripClinicSuffix(clinicName);
        if (fromName != null) {
            return fromName;
        }

        if (address != null) {
            String[] parts = address.split(",");
            if (parts.length >= 2) {
                String district = parts[parts.length - 2].trim();
                if (!district.isEmpty()) {
                    return district;
                }
            }
        }

        return "Hong Kong";
    }

    private String stripClinicSuffix(String clinicName) {
        if (clinicName == null) {
            return null;
        }

        String value = clinicName.trim();
        if (value.isEmpty()) {
            return null;
        }

        value = value.replaceFirst("(?i)^CCHC\\s+", "");
        value = value.replaceFirst("(?i)\\s+Clinic$", "");
        value = value.trim();
        return value.isEmpty() ? null : value;
    }
}
