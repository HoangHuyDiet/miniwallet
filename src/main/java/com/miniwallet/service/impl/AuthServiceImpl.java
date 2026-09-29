package com.miniwallet.service.impl;

import com.miniwallet.dto.request.LoginRequest;
import com.miniwallet.dto.request.RegisterRequest;
import com.miniwallet.dto.response.LoginResponse;
import com.miniwallet.dto.response.RegisterResponse;
import com.miniwallet.entity.User;
import com.miniwallet.entity.Wallet;
import com.miniwallet.enums.UserRole;
import com.miniwallet.repository.UserRepository;
import com.miniwallet.repository.WalletRepository;
import com.miniwallet.security.CustomUserDetails;
import com.miniwallet.security.JwtService;
import com.miniwallet.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final WalletRepository walletRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  @Override
  @Transactional
  public RegisterResponse register(RegisterRequest request) {
    log.info("(register) username: {}", request.getUsername());

    if (userRepository.existsByUserName(request.getUsername())) {
      throw new IllegalArgumentException("This username was existed: " + request.getUsername());
    }

    User user = User.builder()
        .userName(request.getUsername())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(UserRole.USER)
        .build();

    User savedUser = userRepository.save(user);

    Wallet wallet = Wallet.builder()
        .user(savedUser)
        .balance(BigDecimal.ZERO)
        .build();
    walletRepository.save(wallet);
    log.info("(register) wallet created for userId: {}", savedUser.getId());

    CustomUserDetails userDetails = new CustomUserDetails(savedUser);
    String token = jwtService.generateToken(userDetails);

    return RegisterResponse.builder()
        .username(savedUser.getUserName())
        .role(savedUser.getRole().name())
        .token(token)
        .message("Registered successfully")
        .build();
  }

  @Override
  public LoginResponse login(LoginRequest request) {
    log.info("(login) username: {}", request.getUsername());

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getUsername(),
            request.getPassword()
        )
    );

    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    String token = jwtService.generateToken(userDetails);

    return LoginResponse.builder()
        .token(token)
        .build();
  }
}
