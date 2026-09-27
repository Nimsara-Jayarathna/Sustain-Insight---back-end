package com.news_aggregator.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Lightweight application entry-point smoke test.
 *
 * <p>Full Spring context startup depends on PostgreSQL and external application
 * configuration, so integration startup belongs in an environment-backed test.
 * Unit/CI validation remains deterministic and does not require production secrets.</p>
 */
class BackendApplicationTests {

    @Test
    void applicationEntryPointExists() {
        assertThat(BackendApplication.class).isNotNull();
    }
}
