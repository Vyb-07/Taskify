package com.taskify.taskify.exception;

public class TaskNotFoundException extends RuntimeException{
    public TaskNotFoundException(Long id){
        super("Task Not Found with id " + id);
    }
}
