package ict.bean;

import java.io.Serializable;

public class BatchImportResult implements Serializable {

    private int rowNumber;
    private String entityType;
    private String identifier;
    private String action;
    private boolean success;
    private String message;

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatusLabel() {
        return success ? "SUCCESS" : "ERROR";
    }

    public String getStatusChipClass() {
        return success ? "status-confirmed" : "status-cancelled";
    }

    public String getRowLabel() {
        return rowNumber <= 0 ? "-" : "Row " + rowNumber;
    }
}