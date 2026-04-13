package com.privaro.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;

@Service
public class GoogleTokenVerifier {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    @Value("${google.client-id}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            }
            logger.warn("Invalid Google ID token");
            return null;
        } catch (Exception e) {
            logger.error("Error verifying Google ID token: {}", e.getMessage());
            return null;
        }
    }

    public static class GoogleUserInfo {
        private final String googleId;
        private final String email;
        private final String name;
        private final String pictureUrl;
        private final boolean emailVerified;

        public GoogleUserInfo(GoogleIdToken.Payload payload) {
            this.googleId = payload.getSubject();
            this.email = payload.getEmail();
            this.name = (String) payload.get("name");
            this.pictureUrl = (String) payload.get("picture");
            this.emailVerified = payload.getEmailVerified();
        }

        public String getGoogleId() {
            return googleId;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public String getPictureUrl() {
            return pictureUrl;
        }

        public boolean isEmailVerified() {
            return emailVerified;
        }
    }
}
