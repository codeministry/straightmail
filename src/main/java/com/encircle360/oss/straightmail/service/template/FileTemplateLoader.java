package com.encircle360.oss.straightmail.service.template;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.encircle360.oss.straightmail.model.Template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileTemplateLoader extends AbstractTemplateLoader {

    @Override
    public Template loadTemplate(String templateId) {
        return loadFromFiles(templateId);
    }

    @PostConstruct
    public void logTemplates() throws IOException {
        log.info("FileTemplateLoader initialisation started...");
        Resource folder = new PathResource("/resources/templates/");
        String fileName = folder.getFile().getPath();
        Path path = Path.of(fileName);

        if (!path.toFile().exists()) {
            log.info("/resources/templates/ does not exist. Service will not load any templates from filesystem.");
            return;
        }

        Files.walk(path).toList()
                .stream()
                .filter(file -> file.getFileName().toString().contains("."))
                .forEach(file -> log.info("Found filesystem template file {}", file.toAbsolutePath()));
    }
}
