package com.news_aggregator.backend.payload;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}
