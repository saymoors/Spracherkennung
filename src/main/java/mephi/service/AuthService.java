package mephi.service;

import mephi.dto.LoginRequest;
import mephi.dto.LoginResponse;
import mephi.dto.RegisterRequest;
import mephi.entity.User;
import mephi.repository.UserRepository;
import mephi.validation.RegistrationValidationChain;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RegistrationValidationChain registrationValidationChain;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RegistrationValidationChain registrationValidationChain) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.registrationValidationChain = registrationValidationChain;
    }

    public void register(RegisterRequest request) throws Exception {
        String login = request != null && request.getLogin() != null ? request.getLogin().trim() : "";
        String email = request != null && request.getEmail() != null ? request.getEmail().trim() : "";
        String password = request != null && request.getPassword() != null ? request.getPassword() : "";

        validateRegistration(login, email, password);

        if (userRepository.existsByEmail(email)) {
            throw new Exception("Email уже занят");
        }
        if (userRepository.existsByLogin(login)) {
            throw new Exception("Логин уже занят");
        }

        User user = new User();
        user.setLogin(login);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) throws Exception {
        String login = request.getLogin() != null ? request.getLogin().trim() : "";
        String password = request.getPassword() != null ? request.getPassword() : "";

        if (login.isEmpty()) {
            throw new Exception("Логин не может быть пустым");
        }
        if (password.isEmpty()) {
            throw new Exception("Пароль не может быть пустым");
        }

        User user = userRepository.findByLogin(login);

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new Exception("Неверный логин или пароль");
        }

        String token = jwtService.generateToken(user.getId(), user.getLogin());
        LoginResponse response = new LoginResponse();
        response.setToken(token);

        return response;
    }

    private void validateRegistration(String login, String email, String password) throws Exception {
        registrationValidationChain.validate(login, email, password);
    }
}
