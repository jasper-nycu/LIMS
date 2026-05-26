package com.tsmc.lims.backend.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    // Ensure you have implemented this to fetch the user from the database
    @Autowired
    private UserDetailsService userDetailsService; 

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String empId;

        // 1. Check if the Bearer Token is present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the Token and parse the empId (Subject)
        jwt = authHeader.substring(7);
        try {
            empId = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Token is expired or invalid. Proceed with the filter chain 
            // and let Spring Security handle the 403 Forbidden response.
            filterChain.doFilter(request, response);
            return;
        }

        // 3. If parsing is successful and the SecurityContext has no authentication, proceed with validation
        if (empId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(empId);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Issue Spring Security internal authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Store the user identity in the global SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // 4. Pass control to the next filter in the chain (or the Controller)
        filterChain.doFilter(request, response);
    }
}