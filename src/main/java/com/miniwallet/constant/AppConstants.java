package com.miniwallet.constant;

public class AppConstants {

  public static final String DEFAULT_PAGE_NUMBER = "0";
  public static final String DEFAULT_PAGE_SIZE = "10";
  public static final String MSG_WALLET_NOT_FOUND = "Wallet not found for the given user";
  public static final String MSG_RECEIVER_NOT_FOUND = "Receiver wallet not found";
  public static final String MSG_INSUFFICIENT_BALANCE = "Insufficient balance";
  public static final String MSG_TRANSFER_TO_SELF = "Cannot transfer to your own wallet";
  public static final String MSG_INVALID_AMOUNT = "Amount must be greater than zero";
  public static final String MSG_DAILY_LIMIT_EXCEEDED = "Daily transfer limit exceeded";
  public static final String DESC_TOPUP = "Topup wallet";
  public static final String DESC_TRANSFER_OUT = "Transfer money to %s";
  public static final String DESC_TRANSFER_IN = "Receive money from %s";
  private AppConstants() {
  }
}
