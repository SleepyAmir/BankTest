package com.springbank.user.dto;

public record PermissionCreateDto(
    String name,
    String description,
    String category
) {}
