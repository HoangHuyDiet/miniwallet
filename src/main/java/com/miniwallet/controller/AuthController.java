package com.miniwallet.controller;

import com.miniwallet.dto.request.LoginRequest;
import com.miniwallet.dto.request.RegisterRequest;
import com.miniwallet.dto.response.LoginResponse;
import com.miniwallet.dto.response.RegisterResponse;
import com.miniwallet.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    log.info("(register) username: {}", request.getUsername());
    RegisterResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    log.info("(login) username: {}", request.getUsername());
    LoginResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }
}
