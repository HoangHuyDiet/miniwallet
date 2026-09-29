package com.miniwallet.dto.response;

import com.miniwallet.enums.TransactionDirection;
import com.miniwallet.enums.TransactionStatus;
import com.miniwallet.enums.TransactionType;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionResponse {

  private Long id;
  private TransactionType transactionType;
  private TransactionDirection transactionDirection;
  private BigDecimal amount;
  private TransactionStatus transactionStatus;
  private Timestamp createdAt;
}
