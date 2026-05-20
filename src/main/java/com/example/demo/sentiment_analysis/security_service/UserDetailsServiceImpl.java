package com.example.demo.sentiment_analysis.security_service;

import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepo userRepo;
    public UserDetailsServiceImpl(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String userEmail) {
        Users byUserEmail = userRepo.findByUserEmail(userEmail);
        if (byUserEmail!=null){
            return User.builder().username(byUserEmail.getUserEmail())
                    .password(byUserEmail.getPassword())
                    .roles(byUserEmail.getRoles().toArray(new String[0])).build();
        }
        throw new UsernameNotFoundException("User Not found exception");
    }
}
