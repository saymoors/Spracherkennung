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

        setProperty(dotenv, "JWT_SECRET");
        setProperty(dotenv, "JWT_EXPIRATION_MS");
        setProperty(dotenv, "SALUTESPEECH_CLIENT_ID");
        setProperty(dotenv, "SALUTESPEECH_CLIENT_SECRET");

        SpringApplication.run(SpracherkennungApplication.class, args);
    }

    private static void setProperty(Dotenv dotenv, String key) {
        String value = dotenv.get(key);
        if (value != null) {
            System.setProperty(key, value);
        }
    }
}
