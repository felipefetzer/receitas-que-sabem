package com.receitasquesabem.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.receitasquesabem.common.ApiExceptions.ForbiddenException;
import com.receitasquesabem.user.User;
import com.receitasquesabem.user.UserRepository;

/**
 * Atalho para obter o utilizador autenticado.
 * R4: o autor de uma receita vem sempre daqui, nunca do pedido.
 */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Sem sessão ativa.");
        }
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ForbiddenException("Sessão inválida."));
    }
}
