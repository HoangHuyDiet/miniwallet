package com.miniwallet.service.impl;

import com.miniwallet.constant.AppConstants;
import com.miniwallet.dto.request.TopUpRequest;
import com.miniwallet.dto.response.BalanceResponse;
import com.miniwallet.dto.response.TransactionResponse;
import com.miniwallet.entity.Transaction;
import com.miniwallet.entity.Wallet;
import com.miniwallet.enums.TransactionDirection;
import com.miniwallet.enums.TransactionStatus;
import com.miniwallet.enums.TransactionType;
import com.miniwallet.exception.ResourceNotFoundException;
import com.miniwallet.repository.TransactionRepository;
import com.miniwallet.repository.WalletRepository;
import com.miniwallet.service.WalletService;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class WalletServiceImpl implements WalletService {

  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;

  public WalletServiceImpl(WalletRepository walletRepository,
      TransactionRepository transactionRepository) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public BalanceResponse getBalance(Long userId) {
    log.info("(getBalance) userId: {}", userId);
    Wallet wallet = walletRepository.findByUserId(userId).orElseThrow(
        () -> {
          log.warn("Wallet not found for user ID: {}", userId);
          return new ResourceNotFoundException(AppConstants.MSG_WALLET_NOT_FOUND);
        });
    return BalanceResponse.builder()
        .id(wallet.getId())
        .balance(wallet.getBalance())
        .build();
  }

  @Override
  @Transactional
  public TransactionResponse topUp(Long userId, TopUpRequest topUpRequest) {
    log.info("(topUp) userId: {}, topUpRequest: {}", userId, topUpRequest);
    Wallet wallet = walletRepository.findByUserIdWithPessimisticLock(userId)
        .orElseThrow(() -> {
          log.warn("Wallet not found for user ID: {}", userId);
          return new ResourceNotFoundException(AppConstants.MSG_WALLET_NOT_FOUND);
        });

    BigDecimal amount = topUpRequest.getAmount();
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("Invalid topup amount: {} for user ID: {}", amount, userId);
      throw new IllegalArgumentException(AppConstants.MSG_INVALID_AMOUNT);
    }

    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.add(amount);
    wallet.setBalance(balanceAfter);
    walletRepository.save(wallet);

    Transaction transaction = Transaction.builder()
        .selfWalletId(wallet)
        .type(TransactionType.TOPUP)
        .direction(TransactionDirection.IN)
        .status(TransactionStatus.COMPLETED)
        .amount(amount)
        .totalAmount(amount)
        .balance_before(balanceBefore)
        .balance_after(balanceAfter)
        .description(AppConstants.DESC_TOPUP)
        .build();

    Transaction saved = transactionRepository.save(transaction);
    log.info("Topup successful for user ID: {}, new balance: {}, referenceId: {}",
        userId, balanceAfter, saved.getReferenceId()) ;
    return maptoResponse(saved);
  }

  private TransactionResponse maptoResponse(Transaction request) {
    return TransactionResponse.builder()
        .id(request.getId())
        .transactionType(request.getType())
        .transactionDirection(request.getDirection())
        .amount(request.getAmount())
        .transactionStatus(request.getStatus())
        .createdAt(request.getCreatedAt())
        .build();
  }
}
