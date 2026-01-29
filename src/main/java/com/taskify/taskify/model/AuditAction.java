package com.taskify.taskify.model;

public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    TOKEN_REFRESH,
    TASK_CREATE,
    TASK_UPDATE,
    TASK_DELETE,
    FOCUS_MODE_USAGE,
    STAGNANT_MODE_USAGE
}
