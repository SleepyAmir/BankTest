package com.springbank.user.mapper;

import com.springbank.user.dto.UserRegistrationDto;
import com.springbank.user.dto.UserResponseDto;
import com.springbank.user.dto.UserUpdateDto;
import com.springbank.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    UserResponseDto toResponseDto(User user);
    void updateUserFromDto(UserUpdateDto dto, @MappingTarget User user);
}
