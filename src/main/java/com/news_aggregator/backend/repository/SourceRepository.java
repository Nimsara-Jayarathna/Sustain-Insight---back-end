package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SourceRepository extends JpaRepository<Source, Long> {
    Optional<Source> findByName(String name);
}
