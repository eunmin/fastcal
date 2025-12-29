package com.fastcal.domain.model;

import com.fastcal.domain.model.vo.Email;
import com.fastcal.domain.model.vo.HashedPassword;
import com.fastcal.domain.model.vo.UserDisplayName;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.Objects;

@Table("users")
public class User {

  @Id
  private Long id;

  @Column("email")
  private final Email email;

  @Column("password")
  private final HashedPassword password;

  @Column("display_name")
  private final UserDisplayName displayName;

  @Column("enabled")
  private final boolean enabled;

  @CreatedDate
  @Column("created_at")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column("updated_at")
  private LocalDateTime updatedAt;

  protected User() {
    this.email = null;
    this.password = null;
    this.displayName = null;
    this.enabled = false;
  }

  public static User of(@NonNull Email email, @NonNull HashedPassword password) {
    Objects.requireNonNull(email, "email must not be null");
    Objects.requireNonNull(password, "password must not be null");
    return new User(null, email, password, null, true, null, null);
  }

  @PersistenceCreator
  private User(Long id, Email email, HashedPassword password, UserDisplayName displayName,
      boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.id = id;
    this.email = Objects.requireNonNull(email, "email must not be null");
    this.password = Objects.requireNonNull(password, "password must not be null");
    this.displayName = displayName;
    this.enabled = enabled;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Email getEmail() {
    return email;
  }

  public HashedPassword getPassword() {
    return password;
  }

  public UserDisplayName getDisplayName() {
    return displayName;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
