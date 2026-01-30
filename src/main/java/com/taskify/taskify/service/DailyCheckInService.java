package com.taskify.taskify.service;

import com.taskify.taskify.dto.DailyCheckInRequest;
import com.taskify.taskify.dto.DailyCheckInResponse;

public interface DailyCheckInService {
    DailyCheckInResponse checkIn(DailyCheckInRequest request);

    DailyCheckInResponse getTodayCheckIn();
}
