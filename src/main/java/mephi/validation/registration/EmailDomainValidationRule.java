package mephi.validation.registration;

import mephi.validation.BaseValidationRule;

public class EmailDomainValidationRule extends BaseValidationRule<RegistrationData> {
    private static final String[] ALLOWED_EMAIL_DOMAINS = {
            "@yandex.ru",
            "@mail.ru",
            "@gmail.com"
    };

    @Override
    protected void check(RegistrationData data) throws Exception {
        String email = data.email().toLowerCase();

        for (String allowedDomain : ALLOWED_EMAIL_DOMAINS) {
            if (email.endsWith(allowedDomain)) {
                return;
            }
        }

        throw new Exception("Email должен быть в домене @yandex.ru, @mail.ru или @gmail.com");
    }
}
