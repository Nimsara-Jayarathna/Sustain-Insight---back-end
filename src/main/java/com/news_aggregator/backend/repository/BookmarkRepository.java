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

    // 🔹 Check if a bookmark exists
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    // 🔹 Find a specific bookmark (for toggle or debugging)
    Optional<Bookmark> findByUserIdAndArticleId(Long userId, Long articleId);

    // 🔹 Delete bookmark by user + article
    void deleteByUserIdAndArticleId(Long userId, Long articleId);

    // 🔹 Get all bookmarks for a user (paginated)
    Page<Bookmark> findAllByUserId(Long userId, Pageable pageable);

    // 🔹 Get all bookmarks for a user (non-paginated)
    List<Bookmark> findAllByUserId(Long userId);
}
