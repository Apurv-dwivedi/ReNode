package com.renode.security; // Apna actual package name yahan dalna

import com.renode.security.JwtUtil;
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
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    // Yahan hum spring security ka default UserDetailsService use kar rahe hain.
    // Ise baad me hum CustomUserDetailsService se replace kar sakte hain agar zaroorat padi.
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        // Check karo ki header me "Bearer " se start hone wala token hai ya nahi
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // "Bearer " (7 characters) hata do
            username = jwtUtil.extractUsername(token);
        }

        // Agar username mil gaya aur context me pehle se authentication set nahi hai
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Token ko validate karo
            if (jwtUtil.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Sab sahi hai, ab user ko authenticated mark kar do
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Request ko aage badhne do
        filterChain.doFilter(request, response);
    }
}