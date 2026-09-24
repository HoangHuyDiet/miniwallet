package com.miniwallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RegisterResponse {

  private long id;
  private String username;
  private String role;
  private String token;
  private String message;
}
