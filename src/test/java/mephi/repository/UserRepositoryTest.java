package mephi.repository;

import mephi.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByLoginReturnsSavedUser() {
        User user = new User();
        user.setLogin("student");
        user.setEmail("student@gmail.com");
        user.setPasswordHash("hash");
        userRepository.save(user);

        User foundUser = userRepository.findByLogin("student");

        assertNotNull(foundUser);
        assertEquals("student@gmail.com", foundUser.getEmail());
    }

    @Test
    void existsByLoginAndEmailReturnsTrueForSavedUser() {
        User user = new User();
        user.setLogin("student");
        user.setEmail("student@gmail.com");
        user.setPasswordHash("hash");
        userRepository.save(user);

        assertTrue(userRepository.existsByLogin("student"));
        assertTrue(userRepository.existsByEmail("student@gmail.com"));
        assertFalse(userRepository.existsByLogin("other"));
    }
}
