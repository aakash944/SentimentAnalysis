package com.example.demo.sentiment_analysis.user.service;

import com.example.demo.sentiment_analysis.comment.repository.CommentRepo;
import com.example.demo.sentiment_analysis.exception.UserNotFoundException;
import com.example.demo.sentiment_analysis.exception.WeakPasswordException;
import com.example.demo.sentiment_analysis.posts.repository.PostsRepo;
import com.example.demo.sentiment_analysis.reaction.repository.ReactionRepo;
import com.example.demo.sentiment_analysis.slice_response_dto.PaginatedResponse;
import com.example.demo.sentiment_analysis.user.dto.UserDto;
import com.example.demo.sentiment_analysis.user.model.Users;
import com.example.demo.sentiment_analysis.user.repository.UserRepo;
import com.example.demo.sentiment_analysis.user.user_response.UserResponse;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private PostsRepo postsRepo;

    @Mock
    private CommentRepo commentRepo;

    @Mock
    private ReactionRepo reactionRepo;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserDbReturnsPaginatedUserResponses() {
        Pageable pageable = PageRequest.of(0, 2);
        LocalDateTime firstCreatedAt = LocalDateTime.of(2026, 6, 18, 8, 30);
        LocalDateTime secondCreatedAt = LocalDateTime.of(2026, 6, 18, 9, 0);
        Users firstUser = user(new ObjectId(), "first@example.com", firstCreatedAt);
        Users secondUser = user(new ObjectId(), "second@example.com", secondCreatedAt);

        when(userRepo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(firstUser, secondUser), pageable, 2));

        PaginatedResponse<UserResponse> result = userService.getUserDb(pageable);

        assertEquals(2, result.getContent().size());
        assertEquals("first@example.com", result.getContent().get(0).getUserEmail());
        assertEquals(firstCreatedAt, result.getContent().get(0).getDateTime());
        assertEquals("second@example.com", result.getContent().get(1).getUserEmail());
        assertEquals(secondCreatedAt, result.getContent().get(1).getDateTime());
        assertEquals(0, result.getPageNumber());
        assertEquals(2, result.getPageSize());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        assertFalse(result.isHasNext());
    }

    @Test
    void newUserCreateSavesUserWithEncodedPasswordRoleAndCreatedDate() {
        UserDto userDto = new UserDto("new@example.com", "Strong1@");
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        when(userRepo.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Users result = userService.newUserCreate(userDto);

        ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
        verify(userRepo).save(userCaptor.capture());
        Users savedUser = userCaptor.getValue();
        assertSame(savedUser, result);
        assertEquals("new@example.com", savedUser.getUserEmail());
        assertEquals(List.of("USER"), savedUser.getRoles());
        assertNotNull(savedUser.getDateTime());
        assertFalse("Strong1@".equals(savedUser.getPassword()));
        assertTrue(passwordEncoder.matches("Strong1@", savedUser.getPassword()));
    }

    @Test
    void newUserCreateThrowsWeakPasswordExceptionWhenPasswordIsCommon() {
        WeakPasswordException exception = assertThrows(
                WeakPasswordException.class,
                () -> userService.newUserCreate(new UserDto("new@example.com", "password"))
        );

        assertEquals("Password is too common", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    void newUserCreateThrowsWeakPasswordExceptionWhenPasswordHasNoUppercase() {
        WeakPasswordException exception = assertThrows(
                WeakPasswordException.class,
                () -> userService.newUserCreate(new UserDto("new@example.com", "strong1@"))
        );

        assertEquals("Must contain uppercase letter", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    void newUserCreateThrowsWeakPasswordExceptionWhenPasswordHasNoLowercase() {
        WeakPasswordException exception = assertThrows(
                WeakPasswordException.class,
                () -> userService.newUserCreate(new UserDto("new@example.com", "STRONG1@"))
        );

        assertEquals("Must contain lowercase letter", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    void newUserCreateThrowsWeakPasswordExceptionWhenPasswordHasNoNumber() {
        WeakPasswordException exception = assertThrows(
                WeakPasswordException.class,
                () -> userService.newUserCreate(new UserDto("new@example.com", "Strong@Pass"))
        );

        assertEquals("Must contain number", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    void newUserCreateThrowsWeakPasswordExceptionWhenPasswordHasNoSpecialCharacter() {
        WeakPasswordException exception = assertThrows(
                WeakPasswordException.class,
                () -> userService.newUserCreate(new UserDto("new@example.com", "Strong123"))
        );

        assertEquals("Must contain special character", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    void newUserCreateThrowsWeakPasswordExceptionWhenCharacterRepeatsMoreThanFourTimes() {
        WeakPasswordException exception = assertThrows(
                WeakPasswordException.class,
                () -> userService.newUserCreate(new UserDto("new@example.com", "Strongaaaaa1@"))
        );

        assertEquals("No character can repeat more than 4 times consecutively", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    void newUserCreateThrowsWeakPasswordExceptionWhenNumberRepeatsMoreThanFourTimes() {
        WeakPasswordException exception = assertThrows(
                WeakPasswordException.class,
                () -> userService.newUserCreate(new UserDto("new@example.com", "Strong11111@"))
        );

        assertEquals("No number can repeat more than 4 times consecutively", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    void newUserCreateThrowsWeakPasswordExceptionWhenSpecialCharacterRepeatsMoreThanFourTimes() {
        WeakPasswordException exception = assertThrows(
                WeakPasswordException.class,
                () -> userService.newUserCreate(new UserDto("new@example.com", "Strong1@@@@@"))
        );

        assertEquals("No special character can repeat more than 4 times consecutively", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    void removeUserDeletesUserAndRelatedDataWhenCurrentUserOwnsAccount() {
        ObjectId userId = new ObjectId();
        Users existingUser = user(userId, "owner@example.com", LocalDateTime.now());

        when(userRepo.findById(userId)).thenReturn(Optional.of(existingUser));

        userService.removeUser(userId, "owner@example.com");

        verify(userRepo).deleteById(userId);
        verify(postsRepo).deleteByUserId(userId);
        verify(commentRepo).deleteByUserId(userId);
        verify(reactionRepo).deleteByUserId(userId);
    }

    @Test
    void removeUserThrowsUserNotFoundExceptionWhenUserDoesNotExist() {
        ObjectId userId = new ObjectId();

        when(userRepo.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.removeUser(userId, "owner@example.com")
        );

        assertEquals("User not found: " + userId, exception.getMessage());
        verify(userRepo, never()).deleteById(any());
        verifyNoInteractions(postsRepo, commentRepo, reactionRepo);
    }

    @Test
    void removeUserThrowsAccessDeniedExceptionWhenDeletingOtherAccount() {
        ObjectId userId = new ObjectId();
        Users existingUser = user(userId, "owner@example.com", LocalDateTime.now());

        when(userRepo.findById(userId)).thenReturn(Optional.of(existingUser));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> userService.removeUser(userId, "other@example.com")
        );

        assertEquals("Cannot delete other account", exception.getMessage());
        verify(userRepo, never()).deleteById(any());
        verifyNoInteractions(postsRepo, commentRepo, reactionRepo);
    }

    @Disabled
    @Test
    void newUserUpdateUpdatesEmailAndPasswordWhenCurrentUserOwnsAccount() {
        ObjectId userId = new ObjectId();
        Users existingUser = user(userId, "old@example.com", LocalDateTime.now());
        UserDto updateDto = new UserDto("updated@example.com", "Updated1@");

        when(userRepo.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepo.save(existingUser)).thenReturn(existingUser);

        Users result = userService.newUserUpdate(userId, updateDto, "old@example.com");

        assertSame(existingUser, result);
        assertEquals("updated@example.com", result.getUserEmail());
        assertEquals("Updated1@", result.getPassword());
        verify(userRepo).save(existingUser);
    }

    @Test
    void newUserUpdateLeavesEmailAndPasswordUnchangedWhenDtoFieldsAreEmpty() {
        ObjectId userId = new ObjectId();
        Users existingUser = user(userId, "old@example.com", LocalDateTime.now());
        existingUser.setPassword("Existing1@");
        UserDto updateDto = new UserDto("", "");

        when(userRepo.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepo.save(existingUser)).thenReturn(existingUser);

        Users result = userService.newUserUpdate(userId, updateDto, "old@example.com");

        assertEquals("old@example.com", result.getUserEmail());
        assertEquals("Existing1@", result.getPassword());
        verify(userRepo).save(existingUser);
    }

    @Test
    void newUserUpdateLeavesEmailAndPasswordUnchangedWhenDtoFieldsAreNull() {
        ObjectId userId = new ObjectId();
        Users existingUser = user(userId, "old@example.com", LocalDateTime.now());
        existingUser.setPassword("Existing1@");
        UserDto updateDto = new UserDto(null, null);

        when(userRepo.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepo.save(existingUser)).thenReturn(existingUser);

        Users result = userService.newUserUpdate(userId, updateDto, "old@example.com");

        assertEquals("old@example.com", result.getUserEmail());
        assertEquals("Existing1@", result.getPassword());
        verify(userRepo).save(existingUser);
    }

    @Test
    void newUserUpdateThrowsUserNotFoundExceptionWhenUserDoesNotExist() {
        ObjectId userId = new ObjectId();

        when(userRepo.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.newUserUpdate(userId, new UserDto("updated@example.com", "Updated1@"), "owner@example.com")
        );

        assertEquals("User is not found", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    void newUserUpdateThrowsAccessDeniedExceptionWhenUpdatingOtherAccount() {
        ObjectId userId = new ObjectId();
        Users existingUser = user(userId, "owner@example.com", LocalDateTime.now());

        when(userRepo.findById(userId)).thenReturn(Optional.of(existingUser));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> userService.newUserUpdate(userId, new UserDto("updated@example.com", "Updated1@"), "other@example.com")
        );

        assertEquals("Cannot update other account", exception.getMessage());
        verify(userRepo, never()).save(any());
    }

    private Users user(ObjectId id, String email, LocalDateTime dateTime) {
        return new Users(id, email, "password", dateTime, List.of("USER"));
    }
}
