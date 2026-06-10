package mephi.validation.registration;

import mephi.validation.BaseValidationRule;

public class PasswordWithoutWhitespaceValidationRule extends BaseValidationRule<RegistrationData> {

    @Override
    protected void check(RegistrationData data) throws Exception {
        if (data.password().chars().anyMatch(Character::isWhitespace)) {
            throw new Exception("Пароль не должен содержать пробелы");
        }
    }
}
