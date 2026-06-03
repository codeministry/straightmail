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
     * Reads a template file that must exist. Logs ERROR when the file cannot be
     * resolved because the missing file will break the surrounding render/send call.
     *
     * @param path template path relative to the {@code templates/} root (e.g. {@code "welcome.ftl"})
     * @return file content, or {@code null} if the file is missing
     */
    protected String getRequiredFileContent(String path) {
        String content = readTemplateFile(path);
        if (content == null) {
            log.error("Couldn't find required template {} in classpath or filesystem.", path);
        }
        return content;
    }

    /**
     * Reads an optional template file. Missing files are not logged because the
     * caller is expected to have a fallback (e.g. deriving plain text from HTML).
     * I/O errors while reading an existing file are still logged at ERROR.
     *
     * @param path template path relative to the {@code templates/} root
     * @return file content, or {@code null} if the file does not exist
     */
    protected String getOptionalFileContent(String path) {
        return readTemplateFile(path);
    }

    private String readTemplateFile(String path) {
        String fullClassPath = "templates/" + path;
        Resource resource = new ClassPathResource(fullClassPath);
        if (resource.exists()) {
            try {
                return new String(resource.getInputStream().readAllBytes());
            } catch (IOException e) {
                log.error("Error while reading template {} from classpath.", fullClassPath, e);
                return null;
            }
        }

        String fullFsPath = "/resources/templates/" + path;
        Path filePath = Paths.get(fullFsPath);
        if (Files.exists(filePath)) {
            try {
                return Files.readString(filePath);
            } catch (Exception e) {
                log.error("Error while reading template {} from filesystem.", fullFsPath, e);
                return null;
            }
        }

        return null;
    }

    /**
     * Builds a {@link com.encircle360.oss.straightmail.model.Template} by reading the three
     * conventional files for the given template ID:
     * <ul>
     *   <li>{@code <templateId>.ftl} — HTML body (required)</li>
     *   <li>{@code <templateId>_subject.ftl} — subject line (required)</li>
     *   <li>{@code <templateId>_plain.ftl} — plain-text body (optional)</li>
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

        // The HTML body and the subject line are required to build a valid mail;
        // the plain-text body is optional because EmailService derives one from
        // the rendered HTML when no dedicated template is present.
        return Template.builder()
                .name(templateId)
                .subject(getRequiredFileContent(subjectTemplatePath))
                .plain(getOptionalFileContent(plainTemplatePath))
                .html(getRequiredFileContent(baseTemplatePath))
                .build();
    }
}
