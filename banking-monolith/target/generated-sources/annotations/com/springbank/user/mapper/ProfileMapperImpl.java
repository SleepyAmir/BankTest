package com.springbank.user.mapper;

import com.springbank.user.dto.UserProfileDto;
import com.springbank.user.entity.User;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-07T19:59:29+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class ProfileMapperImpl implements ProfileMapper {

    @Override
    public UserProfileDto toProfileDto(User user) {
        if ( user == null ) {
            return null;
        }

        Long id = null;
        String username = null;
        String email = null;
        String firstName = null;
        String lastName = null;
        String phoneNumber = null;

        id = user.getId();
        username = user.getUsername();
        email = user.getEmail();
        firstName = user.getFirstName();
        lastName = user.getLastName();
        phoneNumber = user.getPhoneNumber();

        Set<String> roles = user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet());
        Set<String> permissions = user.getRoles().stream().flatMap(r -> r.getPermissions().stream()).map(p -> p.getName()).collect(java.util.stream.Collectors.toSet());

        UserProfileDto userProfileDto = new UserProfileDto( id, username, email, firstName, lastName, phoneNumber, roles, permissions );

        return userProfileDto;
    }
}
