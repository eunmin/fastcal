package com.fastcal.domain.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("users")
public class User {

  @Id
  private Long id;

  private String email;

  private String password;

  private String displayName;

  private boolean enabled;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
