package com.tsmc.lims.backend.fabuser.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "employee_id", length = 20)
    private String employeeId;

    @Column(name = "role_enum", length = 50)
    private String roleEnum;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    protected UserEntity() {
    }

    public UserEntity(String employeeId, String roleEnum, String firstName, String lastName, String department, String email) {
        this.employeeId = employeeId;
        this.roleEnum = roleEnum;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.email = email;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getRoleEnum() {
        return roleEnum;
    }

    public boolean isActive() {
        return active;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDepartment() {
        return department;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return (firstName + " " + lastName).trim();
    }
}
