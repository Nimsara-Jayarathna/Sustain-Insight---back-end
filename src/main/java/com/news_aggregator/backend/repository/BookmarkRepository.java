package com.news_aggregator.backend.repository;

import com.news_aggregator.backend.model.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // Must match field names in Bookmark.java (user, article)
    boolean existsByUser_IdAndArticle_Id(Long userId, Long articleId);

    Optional<Bookmark> findByUser_IdAndArticle_Id(Long userId, Long articleId);

    void deleteByUser_IdAndArticle_Id(Long userId, Long articleId);

    Page<Bookmark> findAllByUser_Id(Long userId, Pageable pageable);

    List<Bookmark> findAllByUser_Id(Long userId);
}

