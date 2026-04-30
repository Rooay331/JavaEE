package ict.db;

import ict.bean.User;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;

public class StaffProfileDB {

    private final String url;
    private final String username;
    private final String password;

    public StaffProfileDB(String url, String username, String password) {
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

    public User findStaffProfileByUserId(Integer userId, Integer clinicId) throws SQLException {
        if (userId == null || clinicId == null) {
            return null;
        }

        String sql = "SELECT user_id, role, clinic_id, full_name, email, phone, date_of_birth, gender, password, is_active, last_login_at, created_at, updated_at "
                + "FROM users WHERE user_id = ? AND clinic_id = ? AND role = 'STAFF' AND is_active = 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, clinicId);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUser(rs);
            }
        }
    }

    public boolean updateStaffProfile(Integer userId, Integer clinicId, String fullName, String email, String phone,
            LocalDate dateOfBirth, String gender) throws SQLException {
        if (userId == null || clinicId == null) {
            return false;
        }

        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, date_of_birth = ?, gender = ? "
                + "WHERE user_id = ? AND clinic_id = ? AND role = 'STAFF' AND is_active = 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fullName);
            setNullableString(statement, 2, email);
            setNullableString(statement, 3, phone);
            setNullableDate(statement, 4, dateOfBirth);
            setNullableString(statement, 5, gender);
            statement.setInt(6, userId);
            statement.setInt(7, clinicId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean changePassword(Integer userId, Integer clinicId, String currentPassword, String newPassword)
            throws SQLException {
        if (userId == null || clinicId == null || currentPassword == null || newPassword == null) {
            return false;
        }

        String sql = "UPDATE users SET password = ? WHERE user_id = ? AND clinic_id = ? AND role = 'STAFF' AND password = ? AND is_active = 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPassword);
            statement.setInt(2, userId);
            statement.setInt(3, clinicId);
            statement.setString(4, currentPassword);
            return statement.executeUpdate() > 0;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User bean = new User();
        bean.setUserId(rs.getInt("user_id"));
        bean.setRole(rs.getString("role"));

        int clinicId = rs.getInt("clinic_id");
        if (!rs.wasNull()) {
            bean.setClinicId(clinicId);
        }

        bean.setFullName(rs.getString("full_name"));
        bean.setEmail(rs.getString("email"));
        bean.setPhone(rs.getString("phone"));

        Date dob = rs.getDate("date_of_birth");
        if (dob != null) {
            bean.setDateOfBirth(dob.toLocalDate());
        }

        bean.setGender(rs.getString("gender"));
        bean.setPassword(rs.getString("password"));
        bean.setIsActive(rs.getInt("is_active"));

        Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
        if (lastLoginAt != null) {
            bean.setLastLoginAt(lastLoginAt.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            bean.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            bean.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return bean;
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private void setNullableDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }
}