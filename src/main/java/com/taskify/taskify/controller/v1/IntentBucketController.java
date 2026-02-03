package com.taskify.taskify.controller.v1;

import com.taskify.taskify.dto.IntentBucketRequest;
import com.taskify.taskify.dto.IntentBucketResponse;
import com.taskify.taskify.dto.IntentOverviewResponse;
import com.taskify.taskify.service.IntentBucketService;
import com.taskify.taskify.dto.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/intents")
@Tag(name = "Intent Bucket APIs", description = "Endpoints for managing purpose-driven task groupings (Intent Buckets)")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Unauthenticated - Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions or ownership violation", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Not Found - Resource does not exist or user doesn't own it", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Conflict - Uniqueness violation or business state conflict", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "429", description = "Too Many Requests - Rate limit exceeded", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class IntentBucketController {

    private final IntentBucketService intentBucketService;

    public IntentBucketController(IntentBucketService intentBucketService) {
        this.intentBucketService = intentBucketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new intent bucket", description = "Defines a new purpose or life theme for task grouping")
    public IntentBucketResponse createIntent(@Valid @RequestBody IntentBucketRequest request) {
        return intentBucketService.createIntent(request);
    }

    @GetMapping
    @Operation(summary = "List all intent buckets", description = "Returns all intent buckets defined by the current user")
    public List<IntentBucketResponse> getAllIntents() {
        return intentBucketService.getAllIntents();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an intent bucket", description = "Removes the intent bucket. Associated tasks will have their intent reference cleared.")
    public void deleteIntent(@PathVariable Long id) {
        intentBucketService.deleteIntent(id);
    }

    @GetMapping("/overview")
    @Operation(summary = "Get intent insights overview", description = "Returns aggregated insights about tasks, focus, and stagnation per intent")
    public IntentOverviewResponse getOverview() {
        return intentBucketService.getOverview();
    }
}
