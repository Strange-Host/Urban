package com.devsam.urbantrend.Security;

import com.devsam.urbantrend.Service.Auth.AuthCache;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("customUserDetailsService")
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthCache authCache;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return authCache.find(email)
                .or(() -> authCache.load(email))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
