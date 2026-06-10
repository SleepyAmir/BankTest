package com.springbank.user.mapper;

import com.springbank.user.dto.UserResponseDto;
import com.springbank.user.dto.UserUpdateDto;
import com.springbank.user.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-10T12:01:24+0330",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponseDto toResponseDto(User user) {
        if ( user == null ) {
            return null;
        }

        Long id = null;
        String username = null;
        String email = null;
        String firstName = null;
        String lastName = null;
        String phoneNumber = null;
        String profilePictureUrl = null;
        boolean enabled = false;
        boolean emailVerified = false;

        id = user.getId();
        username = user.getUsername();
        email = user.getEmail();
        firstName = user.getFirstName();
        lastName = user.getLastName();
        phoneNumber = user.getPhoneNumber();
        profilePictureUrl = user.getProfilePictureUrl();
        enabled = user.isEnabled();
        emailVerified = user.isEmailVerified();

        UserResponseDto userResponseDto = new UserResponseDto( id, username, email, firstName, lastName, phoneNumber, profilePictureUrl, enabled, emailVerified );

        return userResponseDto;
    }

    @Override
    public void updateUserFromDto(UserUpdateDto dto, User user) {
        if ( dto == null ) {
            return;
        }

        if ( dto.email() != null ) {
            user.setEmail( dto.email() );
        }
        if ( dto.firstName() != null ) {
            user.setFirstName( dto.firstName() );
        }
        if ( dto.lastName() != null ) {
            user.setLastName( dto.lastName() );
        }
        if ( dto.phoneNumber() != null ) {
            user.setPhoneNumber( dto.phoneNumber() );
        }
        if ( dto.profilePictureUrl() != null ) {
            user.setProfilePictureUrl( dto.profilePictureUrl() );
        }
    }
}
