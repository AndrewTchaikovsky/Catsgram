package ru.yandex.practicum.catsgram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.catsgram.dal.ImageRepository;
import ru.yandex.practicum.catsgram.dal.PostRepository;
import ru.yandex.practicum.catsgram.dto.ImageDto;
import ru.yandex.practicum.catsgram.dto.ImageUploadResponse;
import ru.yandex.practicum.catsgram.exception.ConditionsNotMetException;
import ru.yandex.practicum.catsgram.exception.ImageFileException;
import ru.yandex.practicum.catsgram.exception.NotFoundException;
import ru.yandex.practicum.catsgram.model.Image;
import ru.yandex.practicum.catsgram.model.ImageData;
import ru.yandex.practicum.catsgram.model.Post;
import ru.yandex.practicum.catsgram.mapper.ImageMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final PostRepository postRepository;

    // директория для хранения изображений
    @Value("${catsgram.image-directory}")
    private String imageDirectory;

    // сохранение списка изображений, связанных с указанным постом
    public List<ImageUploadResponse> saveImages(long postId, List<MultipartFile> files) {
        return files
                .stream()
                .map(file -> saveImage(postId, file))
                .collect(Collectors.toList());
    }

    // сохранение отдельного изображения, связанного с указанным постом
    private ImageUploadResponse saveImage(long postId, MultipartFile file) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ConditionsNotMetException("Указанный пост не найден"));

        // сохраняем изображение на диск и возвращаем путь к файлу
        Path filePath = saveFile(file, post);

        Image image = imageRepository.save(ImageMapper.maptoImage(postId, filePath, file.getOriginalFilename()));

        return ImageMapper.mapToImageUploadResponse(image);
    }

    public List<ImageDto> getImages(long postId) {
        return imageRepository.findByPostId(postId)
                .stream()
                .map(image -> {
                    byte[] bytes = loadFile(image);
                    return ImageMapper.mapToImageDto(image, bytes);
                })
                .collect(Collectors.toList());
    }

    // загружаем данные указанного изображения с диска
    public ImageData getImageData(long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Изображение с id = " + imageId + " не найдено"));

        // загрузка файла с диска
        byte[] data = loadFile(image);

        return new ImageData(data, image.getOriginalFileName());
    }

    // сохранение файла изображения
    private Path saveFile(MultipartFile file, Post post) {
        try {
            // формирование уникального названия файла на основе текущего времени и расширения оригинального файла
            String uniqueFileName = String.format("%d.%s", Instant.now().toEpochMilli(),
                    StringUtils.getFilenameExtension(file.getOriginalFilename()));

            // формирование пути для сохранения файла с учётом идентификаторов автора и поста
            Path uploadPath = Paths.get(
                    imageDirectory,
                    String.valueOf(post.getAuthor().getId()),
                    post.getId().toString()
            );

            Path filePath = uploadPath.resolve(uniqueFileName);

            // создаём директории, если они ещё не созданы
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // сохраняем файл по сформированному пути
            file.transferTo(filePath);
            return filePath;
        } catch (IOException e) {
            throw new ImageFileException("Ошибка сохранения файла");
        }
    }

    private byte[] loadFile(Image image) {
        Path path = Paths.get(image.getFilePath());
        if (Files.exists(path)) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                throw new ImageFileException("Ошибка чтения файла.  Id: " + image.getId()
                        + ", name: " + image.getOriginalFileName());
            }
        } else {
            throw new ImageFileException("Файл не найден. Id: " + image.getId()
                    + ", name: " + image.getOriginalFileName());
        }
    }

}