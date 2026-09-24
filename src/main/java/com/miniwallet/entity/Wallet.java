package com.miniwallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "wallet")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @OneToOne
  @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
  private User userId;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(nullable = false)
  private long version;

  @Column(nullable = false)
  @CreationTimestamp
  private Timestamp createdAt;

  @Column(nullable = false)
  @UpdateTimestamp
  private Timestamp updatedAt;
}
