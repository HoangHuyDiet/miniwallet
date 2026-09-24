package com.miniwallet.entity;

import com.miniwallet.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(unique = true, nullable = false)
  private String userName;

  @Column(nullable = false)
  private String password;

  @Builder.Default
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private UserRole role = UserRole.USER;

  @Column(nullable = false)
  @CreationTimestamp
  private Timestamp createdAt;

  @Column(nullable = false)
  @UpdateTimestamp
  private Timestamp updatedAt;
}
