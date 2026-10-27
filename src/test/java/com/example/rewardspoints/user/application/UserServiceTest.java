package com.example.rewardspoints.user.application;

import com.example.rewardspoints.shared.error.NotFoundException;
import com.example.rewardspoints.shared.pagination.PageResponse;
import com.example.rewardspoints.user.api.dto.CreateUserRequest;
import com.example.rewardspoints.user.api.dto.CreateUserResponse;
import com.example.rewardspoints.user.api.dto.QueryUserRequest;
import com.example.rewardspoints.user.api.dto.QueryUserResponse;
import com.example.rewardspoints.user.api.dto.UpdateUserRequest;
import com.example.rewardspoints.user.api.dto.UpdateUserResponse;
import com.example.rewardspoints.user.domain.User;
import com.example.rewardspoints.user.domain.UserStatus;
import com.example.rewardspoints.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UUID id;
    private User user;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        user = User.create("alice@example.com", "Alice");
        ReflectionTestUtils.setField(user, "id", id);
    }

    @Test
    void getUsers_returnsMappedPageResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<QueryUserResponse> response =
                userService.getUsers(new QueryUserRequest("alice", null, UserStatus.ACTIVE), pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().email()).isEqualTo("alice@example.com");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    void getUsers_withNoFilters_stillQueries() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        PageResponse<QueryUserResponse> response =
                userService.getUsers(new QueryUserRequest(null, null, null), pageable);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void getUserByUuid_returnsUser() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThat(userService.getUserByUuid(id)).isSameAs(user);
    }

    @Test
    void getUserByUuid_throwsNotFound_whenMissing() {
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByUuid(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void createUser_normalizesEmailAndSavesActiveUser() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserResponse response =
                userService.createUser(new CreateUserRequest("  Bob@Example.COM ", "Bob"));

        assertThat(response.email()).isEqualTo("bob@example.com");
        assertThat(response.displayName()).isEqualTo("Bob");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateUser_changesDisplayName_andLeavesStatusAlone() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UpdateUserResponse response = userService.updateUser(id, new UpdateUserRequest("Alice B", null));

        assertThat(response.displayName()).isEqualTo("Alice B");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateUser_suspendsAndReactivates() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UpdateUserResponse suspended = userService.updateUser(id, new UpdateUserRequest(null, UserStatus.SUSPENDED));
        assertThat(suspended.status()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(suspended.displayName()).isEqualTo("Alice");

        UpdateUserResponse active = userService.updateUser(id, new UpdateUserRequest(null, UserStatus.ACTIVE));
        assertThat(active.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateUser_withAllFieldsNull_changesNothing() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UpdateUserResponse response = userService.updateUser(id, new UpdateUserRequest(null, null));

        assertThat(response.displayName()).isEqualTo("Alice");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateUser_throwsNotFound_whenMissing() {
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(id, new UpdateUserRequest("x", null)))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteUser_deletesExistingUser() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.deleteUser(id);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_throwsNotFound_whenMissing() {
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(id)).isInstanceOf(NotFoundException.class);
        verify(userRepository, never()).delete(any(User.class));
    }
}
