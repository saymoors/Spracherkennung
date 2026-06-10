package mephi.validation;

import mephi.validation.registration.EmailDomainValidationRule;
import mephi.validation.registration.EmailRequiredValidationRule;
import mephi.validation.registration.LoginAllowedCharactersValidationRule;
import mephi.validation.registration.LoginRequiredValidationRule;
import mephi.validation.registration.PasswordDigitCountValidationRule;
import mephi.validation.registration.PasswordLetterCountValidationRule;
import mephi.validation.registration.PasswordLengthValidationRule;
import mephi.validation.registration.PasswordRequiredValidationRule;
import mephi.validation.registration.PasswordWithoutWhitespaceValidationRule;
import mephi.validation.registration.RegistrationData;
import org.springframework.stereotype.Component;

@Component
public class RegistrationValidationChain {
    private final ValidationRule<RegistrationData> rules;

    public RegistrationValidationChain() {
        rules = new LoginRequiredValidationRule();
        rules
                .setNext(new LoginAllowedCharactersValidationRule())
                .setNext(new EmailRequiredValidationRule())
                .setNext(new EmailDomainValidationRule())
                .setNext(new PasswordRequiredValidationRule())
                .setNext(new PasswordWithoutWhitespaceValidationRule())
                .setNext(new PasswordLetterCountValidationRule())
                .setNext(new PasswordDigitCountValidationRule())
                .setNext(new PasswordLengthValidationRule());
    }

    public void validate(String login, String email, String password) throws Exception {
        rules.validate(new RegistrationData(login, email, password));
    }
}
