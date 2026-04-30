package ict.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class PolicyDB {

    private final String url;
    private final String username;
    private final String password;

    public PolicyDB(String url, String username, String password) {
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

    public int findIntPolicy(String policyKey, int defaultValue) throws SQLException {
        if (policyKey == null || policyKey.trim().isEmpty()) {
            return defaultValue;
        }

        String sql = "SELECT policy_value FROM policy_settings WHERE policy_key = ? LIMIT 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, policyKey.trim());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString("policy_value");
                    if (value != null && !value.trim().isEmpty()) {
                        try {
                            return Integer.parseInt(value.trim());
                        } catch (NumberFormatException ex) {
                            return defaultValue;
                        }
                    }
                }
            }
        }

        return defaultValue;
    }

    public String findStringPolicy(String policyKey, String defaultValue) throws SQLException {
        if (policyKey == null || policyKey.trim().isEmpty()) {
            return defaultValue;
        }

        String sql = "SELECT policy_value FROM policy_settings WHERE policy_key = ? LIMIT 1";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, policyKey.trim());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString("policy_value");
                    if (value != null && !value.trim().isEmpty()) {
                        return value.trim();
                    }
                }
            }
        }

        return defaultValue;
    }

    public boolean findBooleanPolicy(String policyKey, boolean defaultValue) throws SQLException {
        String rawValue = findStringPolicy(policyKey, null);
        if (rawValue == null) {
            return defaultValue;
        }

        String normalized = rawValue.trim().toLowerCase();
        if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized) || "enabled".equals(normalized)) {
            return true;
        }
        if ("0".equals(normalized) || "false".equals(normalized) || "no".equals(normalized) || "disabled".equals(normalized)) {
            return false;
        }

        return defaultValue;
    }

    public boolean savePolicyValue(String policyKey, String policyValue) throws SQLException {
        if (policyKey == null || policyKey.trim().isEmpty()) {
            return false;
        }

        String sql = "INSERT INTO policy_settings (policy_key, policy_value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE policy_value = VALUES(policy_value)";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, policyKey.trim());
            statement.setString(2, policyValue == null ? null : policyValue.trim());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean saveIntPolicy(String policyKey, int policyValue) throws SQLException {
        return savePolicyValue(policyKey, Integer.toString(policyValue));
    }

    public boolean saveBooleanPolicy(String policyKey, boolean policyValue) throws SQLException {
        return savePolicyValue(policyKey, policyValue ? "1" : "0");
    }

    public Map<String, String> findPolicies(String... policyKeys) throws SQLException {
        Map<String, String> policies = new LinkedHashMap<>();
        if (policyKeys == null) {
            return policies;
        }

        for (String policyKey : policyKeys) {
            if (policyKey == null || policyKey.trim().isEmpty()) {
                continue;
            }
            policies.put(policyKey.trim(), findStringPolicy(policyKey, null));
        }

        return policies;
    }
}