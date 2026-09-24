package com.miniwallet.entity;

import jakarta.persistence.Id;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@RedisHash("idempotency_keys")
public class IdempotencyKey {
  @Id
  private String idempotencyKey;

  private Long userId;

  private Long transactionId;

  private String responseBody;

  private Integer responseStatus;

  private Timestamp createdAt;

  @TimeToLive
  @Builder.Default
  private Long ttl = 600L;
}
