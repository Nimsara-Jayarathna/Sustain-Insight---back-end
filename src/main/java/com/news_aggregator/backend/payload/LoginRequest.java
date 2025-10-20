
package com.news_aggregator.backend.payload;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    private String deviceInfo;
    private String location;
    private String userAgent;
}
