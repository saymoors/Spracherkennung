package mephi.validation.registration;

import mephi.validation.BaseValidationRule;

public class PasswordRequiredValidationRule extends BaseValidationRule<RegistrationData> {

    @Override
    protected void check(RegistrationData data) throws Exception {
        if (data.password().isEmpty()) {
            throw new Exception("Пароль не может быть пустым");
        }
    }
}
