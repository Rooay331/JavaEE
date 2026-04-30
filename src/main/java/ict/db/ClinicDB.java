package ict.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClinicDB {

    private final String url;
    private final String username;
    private final String password;

    public ClinicDB(String url, String username, String password) {
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

    public String findClinicNameById(Integer clinicId) throws SQLException {
        if (clinicId == null) {
            return null;
        }

        String sql = "SELECT clinic_name FROM clinics WHERE clinic_id = ?";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("clinic_name");
                }
            }
        }

        return null;
    }

    public Integer findClinicIdByName(String clinicName) throws SQLException {
        if (clinicName == null || clinicName.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT clinic_id FROM clinics WHERE LOWER(clinic_name) = LOWER(?) LIMIT 1";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clinicName.trim());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("clinic_id");
                }
            }
        }

        return null;
    }
}