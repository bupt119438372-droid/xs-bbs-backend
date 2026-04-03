package com.xs.bbs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SocialIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeFollowingFollowersAndUserHome() throws Exception {
        String leftRegister = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"relation_left",
                                  "password":"123456",
                                  "nickname":"关系左侧",
                                  "bio":"关系左侧 bio"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String leftToken = JsonTestUtils.readString(leftRegister, "$.data.tokenValue");
        Integer leftUserId = JsonTestUtils.readInteger(leftRegister, "$.data.user.userId");

        String rightRegister = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"relation_right",
                                  "password":"123456",
                                  "nickname":"关系右侧",
                                  "bio":"关系右侧 bio"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String rightToken = JsonTestUtils.readString(rightRegister, "$.data.tokenValue");
        Integer rightUserId = JsonTestUtils.readInteger(rightRegister, "$.data.user.userId");

        String sameThought = """
                {
                  "content":"我想把长期主义写进今天的选择里，也想在焦虑里守住自己的节奏。",
                  "degree":"OBSESSION"
                }
                """;

        mockMvc.perform(post("/api/v1/thoughts")
                        .header("xs-bbs-token", leftToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameThought))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/thoughts")
                        .header("xs-bbs-token", rightToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameThought))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/social/follow")
                        .header("xs-bbs-token", leftToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetId": %s
                                }
                                """.formatted(rightUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followed").value(true));

        mockMvc.perform(post("/api/v1/social/follow")
                        .header("xs-bbs-token", rightToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetId": %s
                                }
                                """.formatted(leftUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mutualFollow").value(true));

        mockMvc.perform(get("/api/v1/social/following/me")
                        .header("xs-bbs-token", leftToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].userId").value(hasItem(rightUserId)))
                .andExpect(jsonPath("$.data[0].canViewHome").value(true));

        mockMvc.perform(get("/api/v1/social/followers/me")
                        .header("xs-bbs-token", leftToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].userId").value(hasItem(rightUserId)));

        mockMvc.perform(get("/api/v1/users/%s/home".formatted(rightUserId))
                        .header("xs-bbs-token", leftToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.nickname").value("关系右侧"))
                .andExpect(jsonPath("$.data.followStatus.followed").value(true))
                .andExpect(jsonPath("$.data.followsYou").value(true))
                .andExpect(jsonPath("$.data.fullThoughtAccess").value(true))
                .andExpect(jsonPath("$.data.thoughts[0].content").isNotEmpty());

        mockMvc.perform(get("/api/v1/thoughts/following/me")
                        .header("xs-bbs-token", leftToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(rightUserId));
    }

    @Test
    void shouldRejectFollowBeforeRealNameUnlock() throws Exception {
        String alphaRegister = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"alpha_user",
                                  "password":"123456",
                                  "nickname":"Alpha",
                                  "bio":"alpha bio"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String betaRegister = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"beta_user",
                                  "password":"123456",
                                  "nickname":"Beta",
                                  "bio":"beta bio"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String alphaToken = JsonTestUtils.readString(alphaRegister, "$.data.tokenValue");
        Integer betaUserId = JsonTestUtils.readInteger(betaRegister, "$.data.user.userId");

        mockMvc.perform(post("/api/v1/social/follow")
                        .header("xs-bbs-token", alphaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetId": %s
                                }
                                """.formatted(betaUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("follow is allowed only after real-name unlock"));
    }
}
