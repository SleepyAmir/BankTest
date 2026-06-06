package com.springbank.user.dto;

public record PermissionResponseDto(
    Long id,
    String name,
    String description,
    String category,
    boolean systemDefault
) {}
