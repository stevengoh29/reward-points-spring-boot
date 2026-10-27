package com.example.rewardspoints.user.application;

import com.example.rewardspoints.shared.error.NotFoundException;
import com.example.rewardspoints.shared.pagination.PageResponse;
import com.example.rewardspoints.shared.persistence.specification.SpecificationBuilder;
import com.example.rewardspoints.user.api.dto.CreateUserRequest;
import com.example.rewardspoints.user.api.dto.CreateUserResponse;
import com.example.rewardspoints.user.api.dto.QueryUserRequest;
import com.example.rewardspoints.user.api.dto.QueryUserResponse;
import com.example.rewardspoints.user.api.dto.UpdateUserRequest;
import com.example.rewardspoints.user.api.dto.UpdateUserResponse;
import com.example.rewardspoints.user.exceptions.EmailAlreadyExistsException;
import com.example.rewardspoints.user.domain.User;
import com.example.rewardspoints.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public PageResponse<QueryUserResponse> getUsers(QueryUserRequest query, Pageable pageable) {
        Specification<User> spec = SpecificationBuilder.<User>create()
                .contains("email", query.email())
                .contains("displayName", query.displayName())
                .equal("status", query.status())
                .build();
        return PageResponse.from(userRepository.findAll(spec, pageable), QueryUserResponse::from);
    }

    public User getUserByUuid(UUID uuid) {
        return this.userRepository.findById(uuid)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public CreateUserResponse createUser(CreateUserRequest request) {
        User user = User.create(request.email(), request.displayName());
        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // ux_users_email_active is the only unique constraint a valid request can trip
            throw new EmailAlreadyExistsException(user.getEmail());
        }
        return CreateUserResponse.from(savedUser);
    }

    @Transactional
    public UpdateUserResponse updateUser(UUID uuid, UpdateUserRequest request) {
        User user = getUserByUuid(uuid);
        if (request.displayName() != null) {
            user.changeDisplayName(request.displayName());
        }
        if (request.status() != null) {
            switch (request.status()) {
                case ACTIVE -> user.activate();
                case SUSPENDED -> user.suspend();
            }
        }
        // flush so auditing has populated updatedAt before we build the response
        return UpdateUserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void deleteUser(UUID uuid) {
        userRepository.delete(getUserByUuid(uuid));
    }
}
