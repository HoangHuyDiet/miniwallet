package com.miniwallet.service;

import com.miniwallet.dto.request.TopUpRequest;
import com.miniwallet.dto.response.BalanceResponse;
import com.miniwallet.dto.response.TransactionResponse;
import com.miniwallet.security.CustomUserDetails;

public interface WalletService {
  BalanceResponse getBalance(Long userId);
  TransactionResponse topUp(Long userId, TopUpRequest topUpRequest);

}
