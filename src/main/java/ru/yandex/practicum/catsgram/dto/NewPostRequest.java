package ru.yandex.practicum.catsgram.dto;

import lombok.Data;

@Data
public class NewPostRequest {
    private Long authorId;
    private String description;
}
