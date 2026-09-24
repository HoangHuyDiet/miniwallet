package com.miniwallet.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

  @NotNull(message = "Tên đăng nhập ko đc null")
  private String username;
  @Size(min = 8, message = "Mật khẩu ko được ngắn hơn 8 ký tự")
  private String password;

}
