package mephi.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationValidationChainTest {
    private final RegistrationValidationChain validationChain = new RegistrationValidationChain();

    @Test
    void validateAllowsCorrectRegistrationData() {
        assertDoesNotThrow(() -> validationChain.validate(
                "student_1",
                "student@gmail.com",
                "abcd123"
        ));
    }

    @Test
    void validateRejectsEmailWithUnsupportedDomain() {
        Exception exception = assertThrows(Exception.class, () -> validationChain.validate(
                "student",
                "student@example.com",
                "abcd123"
        ));

        assertEquals("Email должен быть в домене @yandex.ru, @mail.ru или @gmail.com", exception.getMessage());
    }

    @Test
    void validateRejectsPasswordWithoutRequiredDigits() {
        Exception exception = assertThrows(Exception.class, () -> validationChain.validate(
                "student",
                "student@gmail.com",
                "abcdef1"
        ));

        assertEquals("Пароль должен содержать минимум 3 цифры", exception.getMessage());
    }
}
