package com.quiz.service;

import com.quiz.dto.AuthResponse;
import com.quiz.dto.LoginRequest;
import com.quiz.dto.UserRegistrationRequest;
import com.quiz.dto.UserResponse;
import com.quiz.exception.ApiException;
import com.quiz.security.JwtProperties;
import com.quiz.security.JwtTokenService;
import com.quiz.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final UserService userService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().trim().toLowerCase(),
                            request.password()
                    ));
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String token = jwtTokenService.generateAccessToken(principal);
            UserResponse user = userService.getById(principal.getId());
            return new AuthResponse(token, "Bearer", jwtProperties.getExpirationMs(), user);
        } catch (BadCredentialsException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    @Transactional
    public AuthResponse register(UserRegistrationRequest request) {
        userService.register(request);
        LoginRequest login = new LoginRequest(request.email().trim().toLowerCase(), request.password());
        return login(login);
    }
}
