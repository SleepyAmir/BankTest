package com.springbank.user.dto;

import java.util.Set;

public record RoleResponseDto(
    Long id,
    String name,
    String description,
    int priority,
    Set<String> permissions
) {}
