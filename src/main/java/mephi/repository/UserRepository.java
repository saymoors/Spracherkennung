package mephi.repository;

import mephi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    User findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}
