package com.privaro.backend.dto;

public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserDto user;

    public AuthResponse() {}

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AuthResponse response = new AuthResponse();
        public Builder accessToken(String accessToken) { response.accessToken = accessToken; return this; }
        public Builder refreshToken(String refreshToken) { response.refreshToken = refreshToken; return this; }
        public Builder tokenType(String tokenType) { response.tokenType = tokenType; return this; }
        public Builder expiresIn(long expiresIn) { response.expiresIn = expiresIn; return this; }
        public Builder user(UserDto user) { response.user = user; return this; }
        public AuthResponse build() { return response; }
    }

    public static class UserDto {
        private Long id;
        private String email;
        private String displayName;
        private String authProvider;

        public UserDto() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getAuthProvider() { return authProvider; }
        public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private final UserDto dto = new UserDto();
            public Builder id(Long id) { dto.id = id; return this; }
            public Builder email(String email) { dto.email = email; return this; }
            public Builder displayName(String displayName) { dto.displayName = displayName; return this; }
            public Builder authProvider(String authProvider) { dto.authProvider = authProvider; return this; }
            public UserDto build() { return dto; }
        }
    }
}
