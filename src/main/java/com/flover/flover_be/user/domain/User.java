package com.flover.flover_be.user.domain;

import com.flover.flover_be.user.exception.UserErrorCode;
import com.flover.flover_be.user.exception.UserException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private static final long EXP_GRANT_INTERVAL_SECONDS = 5L;
    private static final long EXP_PER_INTERVAL = 1L;
    private static final long EXPERIENCE_PER_LEVEL = 720L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    private String email;
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(nullable = false, columnDefinition = "int default 1")
    private int level = 1;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long experience = 0L;

    @Column(name = "total_plogging_seconds", nullable = false, columnDefinition = "bigint default 0")
    private long totalPloggingSeconds = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "title", nullable = false)
    private UserTitle title = UserTitle.쓰봉이;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static User create(OAuthProvider provider, String providerId, String email, String nickname, String profileImageUrl) {
        User user = new User();
        user.provider = provider;
        user.providerId = providerId;
        user.email = email;
        user.nickname = nickname;
        user.profileImageUrl = profileImageUrl;
        return user;
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void addPloggingTimeSeconds(long seconds) {
        if (seconds <= 0) {
            throw new UserException(UserErrorCode.INVALID_PLOGGING_TIME);
        }

        this.totalPloggingSeconds += seconds;
        this.experience = (this.totalPloggingSeconds / EXP_GRANT_INTERVAL_SECONDS) * EXP_PER_INTERVAL;
        this.level = (int) (this.experience / EXPERIENCE_PER_LEVEL) + 1;
        this.title = UserTitle.of(this.level);
    }
}