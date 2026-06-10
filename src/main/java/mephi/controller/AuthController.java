package mephi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mephi.dto.LoginRequest;
import mephi.dto.LoginResponse;
import mephi.dto.RegisterRequest;
import mephi.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Регистрация и вход")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "ОК"),
            @ApiResponse(responseCode = "409", description = "Логин или email уже занят")
    })
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("OK");
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Вход")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ОК"),
            @ApiResponse(responseCode = "401", description = "Неверный логин или пароль")
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(exception.getMessage());
        }
    }
}
