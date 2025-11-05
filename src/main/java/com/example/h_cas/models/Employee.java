package com.example.h_cas.models;

/**
 * Employee model class representing healthcare staff members in the H-CAS system.
 * Contains all necessary information for employee management.
 */
public class Employee {
    
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String role;
    private String username;
    private String password;
    private String createdDate;
    private boolean isActive;
    
    // Professional fields
    private String licenseNumber;
    private String specialization;
    private String experience;
    private String department;
    
    // Profile picture
    private String profilePictureUrl;

    // Default constructor
    public Employee() {
        this.isActive = true;
    }

    // Constructor with basic information
    public Employee(String employeeId, String firstName, String lastName, String email, 
                   String phone, String role, String username, String password) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.username = username;
        this.password = password;
        this.isActive = true;
    }

    // Getters and Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    // Utility methods
    public String getFullName() {
        // Return exactly as entered by admin - preserve spacing and formatting
        if (firstName == null) firstName = "";
        if (lastName == null) lastName = "";
        
        // Combine first and last name exactly as admin entered them
        String fullName = firstName.trim();
        if (!lastName.trim().isEmpty()) {
            fullName += " " + lastName.trim();
        }
        
        return fullName.trim();
    }

    public String getRoleDisplayName() {
        switch (role) {
            case "Nurse":
                return "Nurse";
            case "Doctor":
                return "Doctor";
            case "Pharmacist":
                return "Pharmacist";
            case "Lab Technician":
                return "Lab Technician";
            case "Receptionist":
                return "Receptionist";
            case "Administrator":
                return "Administrator";
            default:
                return role;
        }
    }

    public boolean isAdmin() {
        return "Administrator".equals(role);
    }

    public boolean isHealthcareStaff() {
        return "Nurse".equals(role) || "Doctor".equals(role) || "Pharmacist".equals(role) || 
               "Lab Technician".equals(role) || "Receptionist".equals(role);
    }

    @Override
    public String toString() {
        return "Employee{" +
                "employeeId='" + employeeId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", username='" + username + '\'' +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Employee employee = (Employee) obj;
        return employeeId != null ? employeeId.equals(employee.employeeId) : employee.employeeId == null;
    }

    @Override
    public int hashCode() {
        return employeeId != null ? employeeId.hashCode() : 0;
    }
}



