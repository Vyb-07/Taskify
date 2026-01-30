package com.taskify.taskify.controller.v1;

import com.taskify.taskify.dto.DailyCheckInRequest;
import com.taskify.taskify.dto.DailyCheckInResponse;
import com.taskify.taskify.service.DailyCheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/day")
@Tag(name = "Daily Check-in APIs", description = "Endpoints for capturing intent and preserving continuity")
public class DailyCheckInController {

    private final DailyCheckInService dailyCheckInService;

    public DailyCheckInController(DailyCheckInService dailyCheckInService) {
        this.dailyCheckInService = dailyCheckInService;
    }

    @Operation(summary = "Capture daily intent", description = "Submit 1-3 tasks and an optional note for today's focus")
    @ApiResponse(responseCode = "200", description = "Check-in successful")
    @ApiResponse(responseCode = "400", description = "Invalid task list size or IDs")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @ApiResponse(responseCode = "403", description = "Forbidden access to one or more tasks")
    @PostMapping("/check-in")
    public ResponseEntity<DailyCheckInResponse> checkIn(@Valid @RequestBody DailyCheckInRequest request) {
        return ResponseEntity.ok(dailyCheckInService.checkIn(request));
    }

    @Operation(summary = "Get today's view", description = "Returns today's intent, carryover tasks from yesterday, and focus suggestions")
    @ApiResponse(responseCode = "200", description = "Today's view retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized access")
    @GetMapping("/today")
    public ResponseEntity<DailyCheckInResponse> getToday() {
        return ResponseEntity.ok(dailyCheckInService.getTodayCheckIn());
    }
}
