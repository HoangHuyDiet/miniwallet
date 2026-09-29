package com.miniwallet.constant;

public class QueryConstants {

  public static final String FIND_HISTORY_WITH_FILTERS = """
      SELECT t FROM Transaction t WHERE t.selfWallet.id = :walletId \
      AND (:type IS NULL OR t.type = :type) \
      AND (:direction IS NULL OR t.direction = :direction) \
      AND t.createdAt >= :startDate AND t.createdAt <= :endDate\
      """;

  public static final String SUM_AMOUNT_BY_FILTERS = """
      SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.selfWallet.id = :walletId \
      AND (:direction IS NULL OR t.direction = :direction) \
      AND (:type IS NULL OR t.type = :type) \
      AND t.status = :status \
      AND t.createdAt >= :startDate AND t.createdAt <= :endDate\
      """;

  public static final String FIND_WALLET_BY_ID_WITH_LOCK = """
      SELECT w FROM Wallet w WHERE w.id = :id\
      """;

  public static final String FIND_WALLET_BY_USER_ID_WITH_LOCK = """
      SELECT w FROM Wallet w WHERE w.user.id = :userId\
      """;

  private QueryConstants() {
  }
}
