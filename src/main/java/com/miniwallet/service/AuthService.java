package com.miniwallet.service;

import com.miniwallet.dto.request.LoginRequest;
import com.miniwallet.dto.request.RegisterRequest;
import com.miniwallet.dto.response.LoginResponse;
import com.miniwallet.dto.response.RegisterResponse;

public interface AuthService {

  RegisterResponse register(RegisterRequest request);

  LoginResponse login(LoginRequest request);
}
