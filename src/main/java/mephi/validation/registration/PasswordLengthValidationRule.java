package mephi.validation.registration;

import mephi.validation.BaseValidationRule;

public class PasswordLengthValidationRule extends BaseValidationRule<RegistrationData> {
    private static final int MIN_PASSWORD_LENGTH = 7;
    private static final int MAX_PASSWORD_LENGTH = 15;

    @Override
    protected void check(RegistrationData data) throws Exception {
        int passwordLength = data.password().length();

        if (passwordLength < MIN_PASSWORD_LENGTH) {
            throw new Exception("Пароль должен содержать минимум 7 символов");
        }
        if (passwordLength > MAX_PASSWORD_LENGTH) {
            throw new Exception("Пароль должен содержать не больше 15 символов");
        }
    }
}
