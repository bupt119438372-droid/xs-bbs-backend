package com.xs.bbs;

import com.jayway.jsonpath.JsonPath;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateNotificationForMatchedUserAndAllowRead() throws Exception {
        String receiverLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"shenqing",
                                  "password":"123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String receiverToken = JsonPath.read(receiverLogin, "$.data.tokenValue");
        String baselineNotifications = mockMvc.perform(get("/api/v1/notifications/me")
                        .header("xs-bbs-token", receiverToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<?> baselineMessages = JsonPath.read(baselineNotifications, "$.data");
        int baselineSize = baselineMessages.size();
        Set<Integer> baselineIds = JsonPath.<List<Integer>>read(baselineNotifications, "$.data[*].id").stream()
                .collect(Collectors.toSet());

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"xuxing",
                                  "password":"123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String publisherToken = JsonPath.read(loginResponse, "$.data.tokenValue");

        mockMvc.perform(post("/api/v1/thoughts")
                        .header("xs-bbs-token", publisherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"真正难的不是努力，而是在漫长里依然相信自己想去的方向。",
                                  "degree":"OBSESSION"
                                }
                                """))
                .andExpect(status().isOk());

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    String currentNotifications = mockMvc.perform(get("/api/v1/notifications/me")
                                    .header("xs-bbs-token", receiverToken))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
                    List<?> currentMessages = JsonPath.read(currentNotifications, "$.data");
                    assertThat(currentMessages.size()).isGreaterThanOrEqualTo(baselineSize + 1);
                });

        String notifications = mockMvc.perform(get("/api/v1/notifications/me")
                        .header("xs-bbs-token", receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data[0].type").value("RELATIONSHIP"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Integer> currentIds = JsonPath.read(notifications, "$.data[*].id");
        Integer notificationId = currentIds.stream()
                .filter(id -> !baselineIds.contains(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a newly created notification for the receiver"));

        mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/read")
                        .header("xs-bbs-token", receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READ"));

        String afterReadNotifications = mockMvc.perform(get("/api/v1/notifications/me")
                        .header("xs-bbs-token", receiverToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Integer> idsAfterRead = JsonPath.read(afterReadNotifications, "$.data[*].id");
        List<String> statusesAfterRead = JsonPath.read(afterReadNotifications, "$.data[*].status");
        int readIndex = idsAfterRead.indexOf(notificationId);

        assertThat(readIndex).isGreaterThanOrEqualTo(0);
        assertThat(statusesAfterRead.get(readIndex)).isEqualTo("READ");
    }
}
