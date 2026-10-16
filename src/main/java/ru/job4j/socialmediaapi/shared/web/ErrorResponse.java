package ru.job4j.socialmediaapi.shared.web;

public record ErrorResponse(
        String code,
        String message
) {
}
