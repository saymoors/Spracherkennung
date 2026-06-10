package mephi.validation.registration;

import mephi.validation.BaseValidationRule;

public class LoginAllowedCharactersValidationRule extends BaseValidationRule<RegistrationData> {

    @Override
    protected void check(RegistrationData data) throws Exception {
        String login = data.login();

        for (int i = 0; i < login.length(); i++) {
            char character = login.charAt(i);

            if (!isAllowedLoginCharacter(character)) {
                throw new Exception("Логин может содержать только буквы, цифры, точку, дефис и нижнее подчеркивание");
            }
        }
    }

    private boolean isAllowedLoginCharacter(char character) {
        return Character.isLetterOrDigit(character)
                || character == '.'
                || character == '-'
                || character == '_';
    }
}
