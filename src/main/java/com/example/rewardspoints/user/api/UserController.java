package com.example.rewardspoints.user.api;

import com.example.rewardspoints.shared.pagination.PageResponse;
import com.example.rewardspoints.user.api.dto.CreateUserRequest;
import com.example.rewardspoints.user.api.dto.CreateUserResponse;
import com.example.rewardspoints.user.api.dto.QueryUserRequest;
import com.example.rewardspoints.user.api.dto.QueryUserResponse;
import com.example.rewardspoints.user.api.dto.UpdateUserRequest;
import com.example.rewardspoints.user.api.dto.UpdateUserResponse;
import com.example.rewardspoints.user.application.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public PageResponse<QueryUserResponse> findUsers(
            QueryUserRequest query,
            Pageable pageable
    ) {
        return this.userService.getUsers(query, pageable);
    }

    @GetMapping("{uuid}")
    public QueryUserResponse findUserByUuid(
            @PathVariable("uuid") UUID uuid
    ) {
        return QueryUserResponse.from(this.userService.getUserByUuid(uuid));
    }

    @PostMapping
    public CreateUserResponse createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return this.userService.createUser(request);
    }

    @PatchMapping("{uuid}")
    public UpdateUserResponse updateUser(
            @PathVariable("uuid") UUID uuid,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return this.userService.updateUser(uuid, request);
    }

    @DeleteMapping("{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @PathVariable("uuid") UUID uuid
    ) {
        this.userService.deleteUser(uuid);
    }
}
