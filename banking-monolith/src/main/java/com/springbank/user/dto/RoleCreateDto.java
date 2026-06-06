package com.springbank.user.dto;

public record RoleCreateDto(
    String name,
    String description,
    int priority
) {}
