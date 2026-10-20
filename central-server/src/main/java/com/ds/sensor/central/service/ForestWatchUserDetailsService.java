package com.ds.sensor.central.service;

import com.ds.sensor.central.model.UserEntity;
import com.ds.sensor.central.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class ForestWatchUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public ForestWatchUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username cannot be blank");
        }
        String cleanUsername = username.trim();
        UserEntity entity = userRepository.findByUsername(cleanUsername)
                .or(() -> userRepository.findByUsernameIgnoreCase(cleanUsername))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + cleanUsername));

        return new User(
                entity.getUsername(),
                entity.getPassword(),
                entity.isEnabled(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(entity.getRole().getAuthority()))
        );
    }
}
