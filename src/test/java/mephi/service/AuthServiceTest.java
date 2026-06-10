package mephi.service;

import mephi.dto.LoginRequest;
import mephi.dto.LoginResponse;
import mephi.entity.User;
import mephi.repository.UserRepository;
import mephi.validation.RegistrationValidationChain;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void loginReturnsJwtTokenForCorrectPassword() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        RegistrationValidationChain registrationValidationChain = mock(RegistrationValidationChain.class);
        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                registrationValidationChain
        );

        User user = new User();
        user.setId(1);
        user.setLogin("student");
        user.setPasswordHash("encoded-password");

        LoginRequest request = new LoginRequest();
        request.setLogin(" student ");
        request.setPassword("password123");

        when(userRepository.findByLogin("student")).thenReturn(user);
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(1, "student")).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
    }
}
