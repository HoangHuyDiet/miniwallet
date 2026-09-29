package com.miniwallet.repository;

import com.miniwallet.constant.QueryConstants;
import com.miniwallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

  Optional<Wallet> findByUserId(Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(QueryConstants.FIND_WALLET_BY_ID_WITH_LOCK)
  Optional<Wallet> findByIdWithPessimisticLock(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(QueryConstants.FIND_WALLET_BY_USER_ID_WITH_LOCK)
  Optional<Wallet> findByUserIdWithPessimisticLock(@Param("userId") Long userId);
}
