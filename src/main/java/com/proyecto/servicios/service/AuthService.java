package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.cliente.LoginRequest;
import com.proyecto.servicios.dto.cliente.LoginResponseDto;

public interface AuthService {

    LoginResponseDto login(LoginRequest request);
}

