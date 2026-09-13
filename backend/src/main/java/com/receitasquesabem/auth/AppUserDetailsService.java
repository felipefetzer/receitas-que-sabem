package com.receitasquesabem.auth;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.receitasquesabem.user.UserRepository;

/**
 * Diz ao Spring Security como ir buscar um utilizador à nossa tabela.
 * O Security só precisa do username, da password cifrada e das permissões.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsername(username)
                .map(u -> org.springframework.security.core.userdetails.User
                        .withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .authorities(List.of())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado: " + username));
    }
}
