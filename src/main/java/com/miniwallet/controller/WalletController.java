package com.miniwallet.controller;

import com.miniwallet.dto.request.TopUpRequest;
import com.miniwallet.dto.response.BalanceResponse;
import com.miniwallet.dto.response.TransactionResponse;
import com.miniwallet.security.CustomUserDetails;
import com.miniwallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/v1/wallet")
public class WalletController {
  private final WalletService walletService;

  public WalletController(WalletService walletService) {
    this.walletService = walletService;
  }

  @GetMapping("/balance")
  public ResponseEntity<BalanceResponse> getBalance(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
    BalanceResponse balanceResponse = walletService.getBalance(customUserDetails.getId());
    return ResponseEntity.ok(balanceResponse);
  }

  @PostMapping("/topup")
  public ResponseEntity<TransactionResponse> topup(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestBody @Valid TopUpRequest request) {
    TransactionResponse response = walletService.topUp(userDetails.getId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
