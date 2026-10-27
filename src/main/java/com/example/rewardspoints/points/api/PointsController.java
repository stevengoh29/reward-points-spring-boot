package com.example.rewardspoints.points.api;

import com.example.rewardspoints.points.api.dto.BalanceResponse;
import com.example.rewardspoints.points.api.dto.EarnRequest;
import com.example.rewardspoints.points.api.dto.EarnResponse;
import com.example.rewardspoints.points.api.dto.LedgerEntryResponse;
import com.example.rewardspoints.points.api.dto.RedeemRequest;
import com.example.rewardspoints.points.api.dto.RedeemResponse;
import com.example.rewardspoints.points.application.PointsService;
import com.example.rewardspoints.shared.pagination.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * userId travels in the body / query until authentication (M2) replaces it with the logged-in user.
 */
@RestController
@RequestMapping("points")
@RequiredArgsConstructor
public class PointsController {
    private final PointsService pointsService;

    @PostMapping("earn")
    public EarnResponse earn(@Valid @RequestBody EarnRequest request) {
        return pointsService.earn(request);
    }

    @PostMapping("redeem")
    public RedeemResponse redeem(@Valid @RequestBody RedeemRequest request) {
        return pointsService.redeem(request);
    }

    @GetMapping("balance")
    public BalanceResponse balance(@RequestParam UUID userId) {
        return pointsService.getBalance(userId);
    }

    /** Newest first; the UUIDv7 id breaks ties between entries created in the same instant. */
    @GetMapping("history")
    public PageResponse<LedgerEntryResponse> history(
            @RequestParam UUID userId,
            @PageableDefault(sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return pointsService.getHistory(userId, pageable);
    }
}
