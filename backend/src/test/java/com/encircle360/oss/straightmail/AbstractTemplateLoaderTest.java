package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.loader.FileTemplateLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = TestApplication.class)
public class AbstractTemplateLoaderTest {

    @Autowired
    FileTemplateLoader fileTemplateLoader;

    @Test
    void load_template_with_html_and_subject_returns_null_plain_when_plain_file_is_absent() {
        // test_json_node.ftl + test_json_node_subject.ftl exist; test_json_node_plain.ftl does not.
        Template template = fileTemplateLoader.loadTemplate("test_json_node");

        assertNotNull(template);
        assertNotNull(template.getHtml());
        assertNotNull(template.getSubject());
        assertNull(template.getPlain(), "Plain template is optional — absence must be silently null");
    }

    @Test
    void load_template_with_missing_id_yields_null_fields() {
        Template template = fileTemplateLoader.loadTemplate("__definitely_missing__");

        assertNotNull(template);
        assertNull(template.getHtml());
        assertNull(template.getSubject());
        assertNull(template.getPlain());
    }
}
