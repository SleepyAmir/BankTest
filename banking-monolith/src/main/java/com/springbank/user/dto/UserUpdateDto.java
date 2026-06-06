package com.springbank.user.dto;

public record UserUpdateDto(
    String email,
    String firstName,
    String lastName,
    String phoneNumber,
    String profilePictureUrl
) {}
