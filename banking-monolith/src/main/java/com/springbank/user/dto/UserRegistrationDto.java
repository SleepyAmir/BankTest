package com.springbank.user.dto;

public record UserRegistrationDto(
    String username,
    String password,
    String email,
    String firstName,
    String lastName,
    String phoneNumber,
    String profilePictureUrl
) {}
