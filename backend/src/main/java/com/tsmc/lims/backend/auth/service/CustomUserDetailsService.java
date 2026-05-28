package com.tsmc.lims.backend.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(String employeeId) throws UsernameNotFoundException {
        // 1. Search for the user in the database using JdbcTemplate
        String sql = "SELECT employee_id, password_hash, role_enum FROM users WHERE employee_id = ?";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, employeeId);
        
        // 2. If no user is found, throw UsernameNotFoundException to let Spring Security handle it
        if (users.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + employeeId);
        }
        
        // 3. Extract user data and construct a UserDetails object
        Map<String, Object> userData = users.get(0);
        String empId = (String) userData.get("employee_id");
        String password = (String) userData.get("password_hash"); 
        String role = (String) userData.get("role_enum");
        
        // 4. Process authority string: Spring Security default role names must start with "ROLE_"
        String authority = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + role;

        // 5. Return the Spring Security built-in User object
        return new User(
                empId,
                password != null ? password : "", 
                Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
    }
}