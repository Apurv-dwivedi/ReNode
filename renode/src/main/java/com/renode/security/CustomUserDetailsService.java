package com.renode.security;

import com.renode.entity.Company;
import com.renode.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Optional ko handle karne ke liye .orElseThrow() ka use kiya hai
        Company company = companyRepository.findByContactEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Company nahi mili: " + email));

        return new org.springframework.security.core.userdetails.User(
                company.getContactEmail(),
                company.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_COMPANY"))
        );
    }
}