package com.news_aggregator.backend.dto;

import java.util.List;

public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private List<CategoryDto> preferredCategories;
    private List<SourceDto> preferredSources;

    public UserDto() {}

    public UserDto(Long id, String firstName, String lastName, String email,
                   List<CategoryDto> preferredCategories,
                   List<SourceDto> preferredSources) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.preferredCategories = preferredCategories;
        this.preferredSources = preferredSources;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<CategoryDto> getPreferredCategories() { return preferredCategories; }
    public void setPreferredCategories(List<CategoryDto> preferredCategories) { this.preferredCategories = preferredCategories; }

    public List<SourceDto> getPreferredSources() { return preferredSources; }
    public void setPreferredSources(List<SourceDto> preferredSources) { this.preferredSources = preferredSources; }
}
