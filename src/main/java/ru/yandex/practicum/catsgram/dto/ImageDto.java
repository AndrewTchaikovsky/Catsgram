package ru.yandex.practicum.catsgram.dto;

import lombok.Data;

@Data
public class ImageDto {
    private Long id;
    private long postId;
    private String originalFileName;
    private byte[] data;
}
