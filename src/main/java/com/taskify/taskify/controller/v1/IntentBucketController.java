package com.taskify.taskify.controller.v1;

import com.taskify.taskify.dto.IntentBucketRequest;
import com.taskify.taskify.dto.IntentBucketResponse;
import com.taskify.taskify.dto.IntentOverviewResponse;
import com.taskify.taskify.service.IntentBucketService;
import io.swagger.v3.oas.annotations.Operation;
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
