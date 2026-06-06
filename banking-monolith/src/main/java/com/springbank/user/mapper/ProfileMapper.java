package com.springbank.user.mapper;

import com.springbank.user.dto.UserProfileDto;
import com.springbank.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    @Mapping(target = "roles", expression = "java(user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet()))")
    @Mapping(target = "permissions", expression = "java(user.getRoles().stream().flatMap(r -> r.getPermissions().stream()).map(p -> p.getName()).collect(java.util.stream.Collectors.toSet()))")
    UserProfileDto toProfileDto(User user);
}
