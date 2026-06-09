package mephi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableScheduling
public class SpracherkennungApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv
                .configure()
                .directory(".")
                .ignoreIfMissing()
                .load();

        String[] environmentKeys = {
                "SERVER_PORT",
                "POSTGRES_HOST",
                "POSTGRES_PORT",
                "POSTGRES_DB",
                "POSTGRES_USER",
                "POSTGRES_PASSWORD",
                "UPLOAD_DIR",
                "JWT_SECRET",
                "JWT_EXPIRATION_MS",
                "SALUTESPEECH_CLIENT_ID",
                "SALUTESPEECH_CLIENT_SECRET",
                "SALUTESPEECH_AUTH_URL",
                "SALUTESPEECH_API_URL",
                "SALUTESPEECH_SCOPE"
        };

        for (String environmentKey : environmentKeys) {
            setProperty(dotenv, environmentKey);
        }

        SpringApplication.run(SpracherkennungApplication.class, args);
    }

    private static void setProperty(Dotenv dotenv, String key) {
        String value = dotenv.get(key);
        if (value != null) {
            System.setProperty(key, value);
        }
    }
}
