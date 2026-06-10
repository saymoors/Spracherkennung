package mephi.validation.registration;

import mephi.validation.BaseValidationRule;

public class PasswordLetterCountValidationRule extends BaseValidationRule<RegistrationData> {
    private static final int MIN_LETTER_COUNT = 4;

    @Override
    protected void check(RegistrationData data) throws Exception {
        long letterCount = data.password().chars()
                .filter(Character::isLetter)
                .count();

        if (letterCount < MIN_LETTER_COUNT) {
            throw new Exception("Пароль должен содержать минимум 4 буквы");
        }
    }
}
