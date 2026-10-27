package com.example.rewardspoints.user.domain;

import com.example.rewardspoints.shared.persistence.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.Locale;
import java.util.Objects;

@Getter
@Entity
@Table(name = "users") // "user" is a reserved word in PostgreSQL
@SQLDelete(sql = "UPDATE users SET deleted_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA needs it; other code must use create()
public class User extends SoftDeletableEntity {

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    public static User create(String email, String displayName) {
        User user = new User();
        user.email = normalizeEmail(email);
        user.displayName = displayName;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    private static String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email").trim().toLowerCase(Locale.ROOT);
    }
}