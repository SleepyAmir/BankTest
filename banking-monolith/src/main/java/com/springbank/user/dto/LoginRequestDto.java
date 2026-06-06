package com.springbank.user.dto;

public record LoginRequestDto(
    String username,
    String password
) {}
