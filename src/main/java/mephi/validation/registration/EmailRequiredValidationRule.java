package mephi.validation.registration;

import mephi.validation.BaseValidationRule;

public class EmailRequiredValidationRule extends BaseValidationRule<RegistrationData> {

    @Override
    protected void check(RegistrationData data) throws Exception {
        if (data.email().isEmpty()) {
            throw new Exception("Email не может быть пустым");
        }
    }
}
