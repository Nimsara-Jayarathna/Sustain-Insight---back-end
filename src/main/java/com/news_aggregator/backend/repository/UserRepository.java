package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    /**
     * Loads the profile associations required by account/profile responses in one
     * repository boundary. This keeps LAZY as the default on the entity while
     * preventing detached-entity access in web/controller code.
     */
    @EntityGraph(attributePaths = {"preferredCategories", "preferredSources"})
    Optional<User> findProfileByEmail(String email);
}
