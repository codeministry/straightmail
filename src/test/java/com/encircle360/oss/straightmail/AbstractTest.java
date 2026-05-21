package com.encircle360.oss.straightmail;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

@AutoConfigureMockMvc
public abstract class AbstractTest {

    // Embedded SMTP on the same port the test config points at (1025).
    @RegisterExtension
    protected static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    protected MockMvc mock;

    protected MvcResult get(String url, ResultMatcher resultMatcher) throws Exception {
        return mock.perform(MockMvcRequestBuilders.get(url)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(resultMatcher)
            .andReturn();
    }

    protected MvcResult delete(String url, ResultMatcher resultMatcher) throws Exception {
        return mock.perform(MockMvcRequestBuilders.delete(url)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(resultMatcher)
            .andReturn();
    }

    protected <T> MvcResult post(String url, T body, ResultMatcher resultMatcher) throws Exception {
        return mock.perform(MockMvcRequestBuilders.post(url)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(body)))
            .andExpect(resultMatcher)
            .andReturn();
    }

    protected <T> MvcResult emptyPost(String url, ResultMatcher resultMatcher) throws Exception {
        return mock.perform(MockMvcRequestBuilders.post(url)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(resultMatcher)
            .andReturn();
    }

    protected <T> MvcResult put(String url, T body, ResultMatcher resultMatcher) throws Exception {
        return mock.perform(MockMvcRequestBuilders.put(url)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(body)))
            .andExpect(resultMatcher)
            .andReturn();
    }

    protected <T> T resultToObject(MvcResult result, Class<T> tClass) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getContentAsString());
        return mapper.readValue(response.getContentAsString(), tClass);
    }
}
