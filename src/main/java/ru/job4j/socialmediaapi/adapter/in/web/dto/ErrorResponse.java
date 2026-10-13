package ru.job4j.socialmediaapi.adapter.in.web.dto;

public record ErrorResponse(
        String code,
        String message
) {
}
