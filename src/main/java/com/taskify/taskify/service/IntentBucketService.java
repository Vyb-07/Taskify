package com.taskify.taskify.service;

import com.taskify.taskify.dto.IntentBucketRequest;
import com.taskify.taskify.dto.IntentBucketResponse;
import com.taskify.taskify.dto.IntentOverviewResponse;

import java.util.List;

public interface IntentBucketService {
    IntentBucketResponse createIntent(IntentBucketRequest request);

    List<IntentBucketResponse> getAllIntents();

    void deleteIntent(Long id);

    IntentOverviewResponse getOverview();
}
