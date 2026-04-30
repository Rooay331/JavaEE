package ict.db;

import ict.bean.User;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserDB {

    private String url;
    private String username;
    private String password;

    public UserDB(String url, String username, String password) {
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

    public boolean isValidUser(String user, String pwd) {
        return findUserByCredentials(user, pwd) != null;
    }

    public User findUserByCredentials(String user, String pwd) {
        String sql = "SELECT user_id, role, clinic_id, full_name, email, phone, date_of_birth, gender, password, is_active, last_login_at, created_at, updated_at "
                + "FROM users WHERE (full_name = ? OR email = ? OR phone = ?) AND password = ? AND is_active = 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user);
            statement.setString(2, user);
            statement.setString(3, user);
            statement.setString(4, pwd);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return mapUser(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean updateLastLogin(Integer userId) throws SQLException {
        if (userId == null) {
            return false;
        }

        String sql = "UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE user_id = ? AND is_active = 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean doRegister(String fullName, String email, String phone, 
                          String Password, String role, 
                          Date dateOfBirth, String gender) {
        boolean isValid = false;
        Connection c;
        PreparedStatement ps;
        String sql = "INSERT INTO users "
                   + "(role, full_name, email, phone, date_of_birth, gender, password, is_active, last_login_at, created_at, updated_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";        
        try {
            c = getConnection();
            ps = c.prepareStatement(sql);
            ps.setString(1, role);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.setDate(5, dateOfBirth);
            ps.setString(6, gender);
            ps.setString(7, Password);
            ps.setBoolean(8, true);
            ps.setTimestamp(9, null);
            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            ps.setTimestamp(10, now);
            ps.setTimestamp(11, now);
            int row = ps.executeUpdate();
            if (row >= 1) {
                isValid = true;
            }
            ps.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isValid;
    }

    public User findUserById(Integer userId) throws SQLException {
        if (userId == null) {
            return null;
        }

        String sql = "SELECT user_id, role, clinic_id, full_name, email, phone, date_of_birth, gender, password, is_active, last_login_at, created_at, updated_at "
                + "FROM users WHERE user_id = ? LIMIT 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }

        return null;
    }

    public User findUserByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT user_id, role, clinic_id, full_name, email, phone, date_of_birth, gender, password, is_active, last_login_at, created_at, updated_at "
                + "FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }

        return null;
    }

    public List<User> findUsers(String keyword, String role, String status, int offset, int limit) throws SQLException {
        List<User> users = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT user_id, role, clinic_id, full_name, email, phone, date_of_birth, gender, password, is_active, last_login_at, created_at, updated_at ");
        sql.append("FROM users WHERE 1=1 ");

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasRole = role != null && !role.trim().isEmpty();
        Integer activeFilter = parseActiveStatus(status);

        if (hasKeyword) {
            sql.append("AND (LOWER(full_name) LIKE ? OR LOWER(email) LIKE ? OR LOWER(phone) LIKE ?) ");
        }
        if (hasRole) {
            sql.append("AND role = ? ");
        }
        if (activeFilter != null) {
            sql.append("AND is_active = ? ");
        }

        sql.append("ORDER BY created_at DESC, user_id DESC LIMIT ? OFFSET ?");

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            if (hasKeyword) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                statement.setString(parameterIndex++, pattern);
                statement.setString(parameterIndex++, pattern);
                statement.setString(parameterIndex++, pattern);
            }
            if (hasRole) {
                statement.setString(parameterIndex++, role.trim().toUpperCase());
            }
            if (activeFilter != null) {
                statement.setInt(parameterIndex++, activeFilter);
            }
            statement.setInt(parameterIndex++, Math.max(limit, 1));
            statement.setInt(parameterIndex, Math.max(offset, 0));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        }

        return users;
    }

    public int countUsers(String keyword, String role, String status) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) AS total FROM users WHERE 1=1 ");

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasRole = role != null && !role.trim().isEmpty();
        Integer activeFilter = parseActiveStatus(status);

        if (hasKeyword) {
            sql.append("AND (LOWER(full_name) LIKE ? OR LOWER(email) LIKE ? OR LOWER(phone) LIKE ?) ");
        }
        if (hasRole) {
            sql.append("AND role = ? ");
        }
        if (activeFilter != null) {
            sql.append("AND is_active = ? ");
        }

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            if (hasKeyword) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                statement.setString(parameterIndex++, pattern);
                statement.setString(parameterIndex++, pattern);
                statement.setString(parameterIndex++, pattern);
            }
            if (hasRole) {
                statement.setString(parameterIndex++, role.trim().toUpperCase());
            }
            if (activeFilter != null) {
                statement.setInt(parameterIndex, activeFilter);
            }

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }

    public boolean createUser(User user) throws SQLException {
        if (user == null || user.getRole() == null || user.getFullName() == null || user.getPassword() == null) {
            return false;
        }

        String sql = "INSERT INTO users (role, clinic_id, full_name, email, phone, date_of_birth, gender, password, is_active, last_login_at, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?)";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            statement.setString(parameterIndex++, user.getRole().trim().toUpperCase());
            setNullableInteger(statement, parameterIndex++, user.getClinicId());
            statement.setString(parameterIndex++, user.getFullName());
            setNullableString(statement, parameterIndex++, user.getEmail());
            setNullableString(statement, parameterIndex++, user.getPhone());
            setNullableDate(statement, parameterIndex++, user.getDateOfBirth());
            setNullableString(statement, parameterIndex++, user.getGender());
            statement.setString(parameterIndex++, user.getPassword());
            statement.setInt(parameterIndex++, user.getIsActive() == 0 ? 0 : 1);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            statement.setTimestamp(parameterIndex++, now);
            statement.setTimestamp(parameterIndex, now);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateUser(User user) throws SQLException {
        if (user == null || user.getUserId() == null || user.getRole() == null || user.getFullName() == null) {
            return false;
        }

        String sql = "UPDATE users SET role = ?, clinic_id = ?, full_name = ?, email = ?, phone = ?, date_of_birth = ?, gender = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ?";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            statement.setString(parameterIndex++, user.getRole().trim().toUpperCase());
            setNullableInteger(statement, parameterIndex++, user.getClinicId());
            statement.setString(parameterIndex++, user.getFullName());
            setNullableString(statement, parameterIndex++, user.getEmail());
            setNullableString(statement, parameterIndex++, user.getPhone());
            setNullableDate(statement, parameterIndex++, user.getDateOfBirth());
            setNullableString(statement, parameterIndex++, user.getGender());
            statement.setInt(parameterIndex++, user.getIsActive() == 0 ? 0 : 1);
            statement.setInt(parameterIndex, user.getUserId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean setUserActive(Integer userId, boolean active) throws SQLException {
        if (userId == null) {
            return false;
        }

        String sql = "UPDATE users SET is_active = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, active ? 1 : 0);
            statement.setInt(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteUser(Integer userId) throws SQLException {
        return setUserActive(userId, false);
    }

    public boolean updatePassword(Integer userId, String currentPassword, String newPassword) throws SQLException {
        if (userId == null || currentPassword == null || newPassword == null) {
            return false;
        }

        String sql = "UPDATE users SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND password = ? AND is_active = 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPassword);
            statement.setInt(2, userId);
            statement.setString(3, currentPassword);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean resetPassword(Integer userId, String newPassword) throws SQLException {
        if (userId == null || newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }

        String sql = "UPDATE users SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND is_active = 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPassword);
            statement.setInt(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateProfile(Integer userId, String fullName, String email, String phone, LocalDate dateOfBirth, String gender)
            throws SQLException {
        User user = findUserById(userId);
        if (user == null) {
            return false;
        }

        user.setFullName(fullName == null ? user.getFullName() : fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setDateOfBirth(dateOfBirth);
        user.setGender(gender == null ? null : gender.trim().toUpperCase());
        return updateUser(user);
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
        if (value == null || value.trim().isEmpty()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private void setNullableDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }

    private Integer parseActiveStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        String normalized = status.trim().toUpperCase();
        if ("ACTIVE".equals(normalized)) {
            return 1;
        }
        if ("SUSPENDED".equals(normalized) || "INACTIVE".equals(normalized)) {
            return 0;
        }

        return null;
    }
}
