package mephi.validation.registration;

import mephi.validation.BaseValidationRule;

public class LoginRequiredValidationRule extends BaseValidationRule<RegistrationData> {

    @Override
    protected void check(RegistrationData data) throws Exception {
        if (data.login().isEmpty()) {
            throw new Exception("Логин не может быть пустым");
        }
    }
}
