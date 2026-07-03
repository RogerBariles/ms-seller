package com.pasteleria.pos.repository;

import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company ORDER BY u.name")
    List<User> findAllWithCompany();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.role IN :roles ORDER BY u.name")
    List<User> findByRoleInWithCompany(@Param("roles") List<UserRole> roles);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.id = :id")
    Optional<User> findByIdWithCompany(@Param("id") UUID id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.username = :username")
    Optional<User> findByUsernameWithCompany(@Param("username") String username);

    @Query(value = """
            SELECT * FROM users u
            WHERE u.active = TRUE
              AND u.birth_date IS NOT NULL
              AND EXTRACT(MONTH FROM u.birth_date) = :month
              AND EXTRACT(DAY FROM u.birth_date) = :day
            """, nativeQuery = true)
    List<User> findActiveWithBirthdayOn(@Param("month") int month, @Param("day") int day);
}
