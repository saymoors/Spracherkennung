package mephi.validation.registration;

import mephi.validation.BaseValidationRule;

public class PasswordDigitCountValidationRule extends BaseValidationRule<RegistrationData> {
    private static final int MIN_DIGIT_COUNT = 3;

    @Override
    protected void check(RegistrationData data) throws Exception {
        long digitCount = data.password().chars()
                .filter(Character::isDigit)
                .count();

        if (digitCount < MIN_DIGIT_COUNT) {
            throw new Exception("Пароль должен содержать минимум 3 цифры");
        }
    }
}
