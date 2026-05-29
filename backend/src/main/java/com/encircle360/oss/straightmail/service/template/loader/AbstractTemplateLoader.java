package com.encircle360.oss.straightmail.service.template.loader;

import com.encircle360.oss.straightmail.model.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstract base class for {@link TemplateLoader} implementations that load FreeMarker templates
 * from the filesystem.
 *
 * <p>Provides shared helper methods for reading template file content from either the classpath
 * (under {@code classpath:templates/}) or the filesystem (under {@code /resources/templates/}),
 * and for constructing a {@link com.encircle360.oss.straightmail.model.Template} from the
 * three conventional template files ({@code .ftl}, {@code _subject.ftl}, {@code _plain.ftl}).
 */
@Slf4j
public abstract class AbstractTemplateLoader implements TemplateLoader {

    /**
     * Reads the content of a template file, first from the classpath, then from the filesystem.
     *
     * <p>Classpath lookup: {@code classpath:templates/<path>}.<br>
     * Filesystem fallback: {@code /resources/templates/<path>}.
     *
     * @param path the relative path within the templates directory (e.g. {@code "welcome.ftl"})
     * @return the file content as a string, or {@code null} if the file is not found in either location
     */
    protected String getFileContent(String path) {
        String fullClassPath = "templates/" + path;
        Resource resource = new ClassPathResource(fullClassPath);
        if (resource.exists()) {
            try {
                return new String(resource.getInputStream().readAllBytes());
            } catch (IOException e) {
                log.error("Error while getting file content from classpath {}.", fullClassPath, e);
            }
        }

        String fullFsPath = "/resources/templates/" + path;
        Path filePath = Paths.get(fullFsPath);
        if (Files.exists(filePath)) {
            try {
                return Files.readString(filePath);
            } catch (Exception e) {
                log.error("Error while getting file content {} from filesystem.", fullFsPath, e);
            }
        }

        log.error("Couldn't find template {} in classpath or filesystem.", path);
        return null;
    }

    /**
     * Builds a {@link com.encircle360.oss.straightmail.model.Template} by reading the three
     * conventional files for the given template ID:
     * <ul>
     *   <li>{@code <templateId>.ftl} — HTML body</li>
     *   <li>{@code <templateId>_subject.ftl} — subject line</li>
     *   <li>{@code <templateId>_plain.ftl} — plain-text body</li>
     * </ul>
     *
     * @param templateId the template identifier (without suffix)
     * @return a {@link com.encircle360.oss.straightmail.model.Template} with content read from files;
     * fields are {@code null} if the corresponding file does not exist
     */
    protected Template loadFromFiles(String templateId) {
        String baseTemplatePath = templateId + ".ftl";
        String subjectTemplatePath = templateId + "_subject.ftl";
        String plainTemplatePath = templateId + "_plain.ftl";

        return Template.builder()
                .name(templateId)
                .subject(this.getFileContent(subjectTemplatePath))
                .plain(this.getFileContent(plainTemplatePath))
                .html(this.getFileContent(baseTemplatePath))
                .build();
    }
}
