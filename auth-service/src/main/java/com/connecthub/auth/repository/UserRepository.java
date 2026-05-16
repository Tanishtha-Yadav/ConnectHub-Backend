package com.connecthub.auth.repository;

import com.connecthub.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUserId(UUID userId);

    Boolean existsByEmail(String email);

    Boolean existsByUsername(String username);

        @Query("""
            SELECT u
            FROM User u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
        List<User> searchByQuery(@Param("query") String query);

    void deleteByUserId(UUID userId);

    Long countByIsActive(Boolean isActive);

    @Query("SELECT u FROM User u WHERE u.isActive = true")
    List<User> findAllActiveUsers();
}
