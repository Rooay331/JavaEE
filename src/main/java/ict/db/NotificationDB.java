package ict.db;

import ict.bean.Notification;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationDB {

    private final String url;
    private final String username;
    private final String password;

    public NotificationDB(String url, String username, String password) {
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

    public boolean createNotification(Integer recipientUserId, String type, String title, String body,
            Integer relatedAppointmentId, Integer relatedTicketId, Integer relatedIncidentId) throws SQLException {
        if (recipientUserId == null || type == null || title == null || body == null) {
            return false;
        }

        String sql = "INSERT INTO notifications (recipient_user_id, type, title, body, related_appointment_id, related_ticket_id, related_incident_id, is_read, read_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, NULL)";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, recipientUserId);
            statement.setString(2, type.trim().toUpperCase());
            statement.setString(3, title.trim());
            statement.setString(4, body.trim());

            if (relatedAppointmentId == null) {
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                statement.setInt(5, relatedAppointmentId);
            }

            if (relatedTicketId == null) {
                statement.setNull(6, java.sql.Types.INTEGER);
            } else {
                statement.setInt(6, relatedTicketId);
            }

            if (relatedIncidentId == null) {
                statement.setNull(7, java.sql.Types.INTEGER);
            } else {
                statement.setInt(7, relatedIncidentId);
            }

            return statement.executeUpdate() > 0;
        }
    }

    public List<Notification> findNotificationsByClinic(Integer clinicId) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        if (clinicId == null) {
            return notifications;
        }

        String sql = "SELECT n.notification_id, n.recipient_user_id, u.full_name AS recipient_name, u.clinic_id, c.clinic_name, "
                + "n.type, n.title, n.body, n.related_appointment_id, n.related_ticket_id, n.related_incident_id, "
                + "n.is_read, n.read_at, n.created_at "
                + "FROM notifications n "
                + "INNER JOIN users u ON n.recipient_user_id = u.user_id "
                + "LEFT JOIN clinics c ON u.clinic_id = c.clinic_id "
                + "WHERE u.role = 'STAFF' AND u.clinic_id = ? "
                + "ORDER BY n.created_at DESC, n.notification_id DESC";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapNotification(rs));
                }
            }
        }

        return notifications;
    }

    public boolean markNotificationAsRead(Integer notificationId, Integer clinicId) throws SQLException {
        if (notificationId == null || clinicId == null) {
            return false;
        }

        String sql = "UPDATE notifications n "
                + "INNER JOIN users u ON n.recipient_user_id = u.user_id "
                + "SET n.is_read = 1, n.read_at = COALESCE(n.read_at, CURRENT_TIMESTAMP) "
                + "WHERE n.notification_id = ? AND u.role = 'STAFF' AND u.clinic_id = ?";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, notificationId);
            statement.setInt(2, clinicId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean markAllNotificationsAsRead(Integer clinicId) throws SQLException {
        if (clinicId == null) {
            return false;
        }

        String sql = "UPDATE notifications n "
                + "INNER JOIN users u ON n.recipient_user_id = u.user_id "
                + "SET n.is_read = 1, n.read_at = COALESCE(n.read_at, CURRENT_TIMESTAMP) "
                + "WHERE u.role = 'STAFF' AND u.clinic_id = ? AND n.is_read = 0";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, clinicId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteNotification(Integer notificationId, Integer clinicId) throws SQLException {
        if (notificationId == null || clinicId == null) {
            return false;
        }

        String sql = "DELETE n FROM notifications n "
                + "INNER JOIN users u ON n.recipient_user_id = u.user_id "
                + "WHERE n.notification_id = ? AND u.role = 'STAFF' AND u.clinic_id = ?";

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, notificationId);
            statement.setInt(2, clinicId);
            return statement.executeUpdate() > 0;
        }
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setNotificationId(rs.getInt("notification_id"));
        notification.setRecipientUserId(rs.getInt("recipient_user_id"));
        notification.setRecipientName(rs.getString("recipient_name"));

        int clinicId = rs.getInt("clinic_id");
        if (!rs.wasNull()) {
            notification.setClinicId(clinicId);
        }
        notification.setClinicName(rs.getString("clinic_name"));

        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setBody(rs.getString("body"));

        int relatedAppointmentId = rs.getInt("related_appointment_id");
        if (!rs.wasNull()) {
            notification.setRelatedAppointmentId(relatedAppointmentId);
        }

        int relatedTicketId = rs.getInt("related_ticket_id");
        if (!rs.wasNull()) {
            notification.setRelatedTicketId(relatedTicketId);
        }

        int relatedIncidentId = rs.getInt("related_incident_id");
        if (!rs.wasNull()) {
            notification.setRelatedIncidentId(relatedIncidentId);
        }

        notification.setRead(rs.getInt("is_read") == 1);

        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) {
            notification.setReadAt(readAt.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }

        return notification;
    }
}