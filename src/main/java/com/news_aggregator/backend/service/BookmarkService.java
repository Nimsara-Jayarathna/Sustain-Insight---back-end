package com.news_aggregator.backend.service;

import com.news_aggregator.backend.dto.ArticleDto;
import com.news_aggregator.backend.dto.PagedResponse;
import com.news_aggregator.backend.model.Article;
import com.news_aggregator.backend.model.Bookmark;
import com.news_aggregator.backend.model.User;
import com.news_aggregator.backend.repository.ArticleRepository;
import com.news_aggregator.backend.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ArticleRepository articleRepository;

    @Value("${pagination.defaultSize:10}")
    private int defaultPageSize;

    @Value("${pagination.maxSize:50}")
    private int maxPageSize;

    /**
     * Add a bookmark for this user and article
     */
    public void addBookmark(User user, Long articleId) {
        if (bookmarkRepository.existsByUserIdAndArticleId(user.getId(), articleId)) {
            return; // Already bookmarked
        }

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        Bookmark bookmark = Bookmark.builder()
            .user(user)
            .article(article)
            .createdAt(OffsetDateTime.now()) // ✅ set manually
            .build();

        bookmarkRepository.save(bookmark);

    }

    /**
     * Remove bookmark
     */
    @Transactional
    public void removeBookmark(User user, Long articleId) {
        bookmarkRepository.deleteByUserIdAndArticleId(user.getId(), articleId);
    }

    /**
     * Get all bookmarks for a user (paginated)
     */
    public PagedResponse<ArticleDto> getBookmarks(User user, int page, int size) {
    int pageIndex = page > 0 ? page - 1 : 0; // ✅ convert FE 1-based → Spring 0-based
    Pageable pageable = PageRequest.of(pageIndex, size, Sort.by("createdAt").descending());

    Page<Bookmark> bookmarks = bookmarkRepository.findAllByUserId(user.getId(), pageable);

    List<ArticleDto> articles = bookmarks.getContent().stream()
            .map(b -> new ArticleDto(
                    b.getArticle().getId(),
                    b.getArticle().getTitle(),
                    b.getArticle().getSummary(),
                    b.getArticle().getImageUrl(),
                    b.getArticle().getPublishedAt(),
                    b.getArticle().getSources().stream().map(s -> s.getName()).toList(),
                    b.getArticle().getCategories().stream().map(c -> c.getName()).toList(),
                    true
            ))
            .toList();

    return new PagedResponse<>(
            articles,
            bookmarks.getNumber() + 1,   // ✅ convert back to 1-based for FE
            bookmarks.getSize(),
            bookmarks.getTotalElements(),
            bookmarks.getTotalPages(),
            bookmarks.isLast()
    );
}

}
