package com.receitasquesabem.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.receitasquesabem.common.ApiExceptions.ConflictException;
import com.receitasquesabem.user.User;
import com.receitasquesabem.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserRepository users,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    public record Credentials(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 6, max = 100) String password) {
    }

    public record UserResponse(Long id, String username) {
    }

    public record CsrfResponse(String token, String headerName) {
    }

    /**
     * O frontend chama isto antes da primeira operação de escrita.
     * Devolve o token no corpo porque, estando noutro domínio,
     * não consegue ler o cookie.
     */
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody Credentials credentials) {
        if (users.existsByUsername(credentials.username())) {
            throw new ConflictException("Já existe um utilizador com esse nome.");
        }
        User user = users.save(new User(
                credentials.username(),
                passwordEncoder.encode(credentials.password())));
        return new UserResponse(user.getId(), user.getUsername());
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody Credentials credentials,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        // Lança BadCredentialsException se falhar — tratada no ApiExceptionHandler.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.username(), credentials.password()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Grava o contexto na sessão; é isto que faz o cookie ser emitido.
        securityContextRepository.saveContext(context, request, response);

        User user = users.findByUsername(credentials.username()).orElseThrow();
        return new UserResponse(user.getId(), user.getUsername());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        User user = users.findByUsername(authentication.getName()).orElseThrow();
        return new UserResponse(user.getId(), user.getUsername());
    }
}
