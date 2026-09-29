package com.miniwallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUpRequest {
  @NotNull(message = "Amount is required")
  @DecimalMin(value = "10000", message = "Minimum topup amount is 10,000")
  private BigDecimal amount;
}
