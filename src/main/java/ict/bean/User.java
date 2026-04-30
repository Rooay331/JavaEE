package ict.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class User implements Serializable {

    private Integer userId;               // UNSIGNED INT, AUTO_INCREMENT
    private String role;                  // ENUM('PATIENT','STAFF','ADMIN')
    private Integer clinicId;             // Assigned clinic for staff users, nullable
    private String fullName;              // VARCHAR(120)
    private String email;                 // VARCHAR(255), nullable but unique
    private String phone;                 // VARCHAR(32), nullable but unique
    private LocalDate dateOfBirth;        // DATE, nullable
    private String gender;                // ENUM('MALE','FEMALE'), nullable
    private String password;              // VARCHAR(255)
    private int isActive;                 // INT, default 1 (1 = active, 0 = inactive)
    private LocalDateTime lastLoginAt;    // DATETIME, nullable
    private LocalDateTime createdAt;      // TIMESTAMP, auto set on insert
    private LocalDateTime updatedAt;      // TIMESTAMP, auto updated on change

    // Default constructor
    public User() {
    }

    // Parameterized constructor for convenience (excludes auto-managed fields)
    public User(String role, String fullName, String email, String phone,
                LocalDate dateOfBirth, String gender, String password) {
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.password = password;
        this.isActive = 1; 
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getClinicId() {
        return clinicId;
    }

    public void setClinicId(Integer clinicId) {
        this.clinicId = clinicId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}