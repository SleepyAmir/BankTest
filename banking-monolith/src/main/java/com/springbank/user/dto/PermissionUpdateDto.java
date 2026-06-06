package com.springbank.user.dto;

public record PermissionUpdateDto(
    String name,
    String description,
    String category
) {}
