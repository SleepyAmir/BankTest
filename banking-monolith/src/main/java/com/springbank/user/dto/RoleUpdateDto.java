package com.springbank.user.dto;

public record RoleUpdateDto(
    String name,
    String description,
    int priority
) {}
