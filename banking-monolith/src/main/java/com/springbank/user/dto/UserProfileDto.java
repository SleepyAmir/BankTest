package com.springbank.user.dto;

import java.util.Set;

public record UserProfileDto(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName,
    String phoneNumber,
    Set<String> roles,
    Set<String> permissions
) {}
