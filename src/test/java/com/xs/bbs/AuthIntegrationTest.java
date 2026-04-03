package com.xs.bbs;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRegisterAndLoginThroughAuthEndpoints() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"newuser01",
                                  "password":"123456",
                                  "nickname":"新生用户",
                                  "bio":"注册后开始记录念头。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("newuser01"))
                .andExpect(jsonPath("$.data.tokenValue").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonTestUtils.readString(response, "$.data.tokenValue");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("xs-bbs-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("newuser01"));
    }

    @Test
    void shouldAllowSeedAccountToLoginAndPublishThought() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"linxi",
                                  "password":"123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.profile.nickname").value("林溪"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonTestUtils.readString(loginResponse, "$.data.tokenValue");

        mockMvc.perform(post("/api/v1/thoughts")
                        .header("xs-bbs-token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"如果方向足够清楚，慢一点也没关系。",
                                  "degree":"FOCUSED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.thought.userId").value(1))
                .andExpect(jsonPath("$.data.analysis.moderationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.analysis.embeddingReady").value(true))
                .andExpect(jsonPath("$.data.analysis.tags[0]").isNotEmpty());
    }

    @Test
    void shouldAnalyzeRejectedThoughtAndKeepItOutOfPublicFeed() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"linxi",
                                  "password":"123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonTestUtils.readString(loginResponse, "$.data.tokenValue");

        String publishResponse = mockMvc.perform(post("/api/v1/thoughts")
                        .header("xs-bbs-token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"有时会冒出自杀的念头，但其实我更想被认真接住。",
                                  "degree":"OBSESSION"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysis.moderationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.candidates").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number thoughtIdValue = JsonPath.read(publishResponse, "$.data.thought.id");
        long thoughtId = thoughtIdValue.longValue();

        mockMvc.perform(get("/api/v1/thoughts/" + thoughtId + "/analysis")
                        .header("xs-bbs-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.moderationStatus").value("REJECTED"));

        mockMvc.perform(get("/api/v1/thoughts/me/analysis")
                        .header("xs-bbs-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].thoughtId").value(hasItem((int) thoughtId)));

        mockMvc.perform(get("/api/v1/thoughts/me")
                        .header("xs-bbs-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].content").value(hasItem("有时会冒出自杀的念头，但其实我更想被认真接住。")));

        mockMvc.perform(get("/api/v1/thoughts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].content").value(not(hasItem("有时会冒出自杀的念头，但其实我更想被认真接住。"))));
    }
}
