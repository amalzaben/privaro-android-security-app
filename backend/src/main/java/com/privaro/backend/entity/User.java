package com.privaro.backend.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    public enum AuthProvider {
        EMAIL, GOOGLE
    }

    public User() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public String getGoogleId() { return googleId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getRefreshToken() { return refreshToken; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final User user = new User();
        public Builder email(String email) { user.email = email; return this; }
        public Builder passwordHash(String passwordHash) { user.passwordHash = passwordHash; return this; }
        public Builder displayName(String displayName) { user.displayName = displayName; return this; }
        public Builder authProvider(AuthProvider authProvider) { user.authProvider = authProvider; return this; }
        public Builder googleId(String googleId) { user.googleId = googleId; return this; }
        public User build() { return user; }
    }

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
