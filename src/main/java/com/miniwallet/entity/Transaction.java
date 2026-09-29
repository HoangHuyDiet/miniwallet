package com.miniwallet.entity;

import com.miniwallet.enums.TransactionDirection;
import com.miniwallet.enums.TransactionStatus;
import com.miniwallet.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "transactions")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Builder.Default
  @Column(nullable = false, unique = true, length = 36)
  private String referenceId = UUID.randomUUID().toString();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "self_wallet_id", nullable = false)
  private Wallet selfWalletId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id")
  private Wallet wallet;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionDirection direction;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionStatus status = TransactionStatus.PENDING;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @Builder.Default
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal fee = BigDecimal.ZERO;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAmount;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal balance_before;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal balance_after;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "original_transaction_id")
  private Transaction originalTransaction;

  @Column(length = 255)
  private String description;

  @Column(name = "failure_reason", length = 255)
  private String failureReason;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Timestamp createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Timestamp updatedAt;

}
