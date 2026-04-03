package com.xs.bbs;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeAuditDashboardForAdmin() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"linxi",
                                  "password":"123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.roles").value(hasItem("admin")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonTestUtils.readString(loginResponse, "$.data.tokenValue");

        mockMvc.perform(get("/api/v1/audit/summary")
                        .header("xs-bbs-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.approved").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.aiDecisions").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.todayDecisions").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/audit/thoughts")
                        .header("xs-bbs-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].analysis.moderationStatus").isNotEmpty())
                .andExpect(jsonPath("$.data[0].records[0].sourceType").isNotEmpty());
    }

    @Test
    void shouldAllowAdminToReviewThought() throws Exception {
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
                                  "content":"最近总有人拉我去赌博，我其实知道这件事很危险。",
                                  "degree":"FOCUSED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysis.moderationStatus").value("REVIEW"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number thoughtIdValue = JsonPath.read(publishResponse, "$.data.thought.id");
        long thoughtId = thoughtIdValue.longValue();

        mockMvc.perform(get("/api/v1/audit/thoughts?status=REVIEW")
                        .header("xs-bbs-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].thoughtId").value(hasItem((int) thoughtId)));

        mockMvc.perform(post("/api/v1/audit/thoughts/" + thoughtId + "/decision")
                        .header("xs-bbs-token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moderationStatus":"APPROVED",
                                  "moderationReason":"人工复核后判定为求助表达，允许公开。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.moderationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.moderationReason").value("人工复核后判定为求助表达，允许公开。"));

        mockMvc.perform(get("/api/v1/audit/thoughts/" + thoughtId + "/records")
                        .header("xs-bbs-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sourceType").value("ADMIN"))
                .andExpect(jsonPath("$.data[0].currentStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data[1].sourceType").value("AI"))
                .andExpect(jsonPath("$.data[1].currentStatus").value("REVIEW"));

        mockMvc.perform(get("/api/v1/audit/thoughts?status=APPROVED")
                        .header("xs-bbs-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].thoughtId").value(hasItem((int) thoughtId)));

        mockMvc.perform(get("/api/v1/audit/summary")
                        .header("xs-bbs-token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminDecisions").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.overriddenDecisions").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldFilterAuditRecordsBySourceOperatorAndDateRange() throws Exception {
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
                                  "content":"最近总有人拉我去赌博，我知道这很危险，但还是很焦虑。",
                                  "degree":"FOCUSED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysis.moderationStatus").value("REVIEW"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number thoughtIdValue = JsonPath.read(publishResponse, "$.data.thought.id");
        long thoughtId = thoughtIdValue.longValue();

        mockMvc.perform(post("/api/v1/audit/thoughts/" + thoughtId + "/decision")
                        .header("xs-bbs-token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moderationStatus":"REJECTED",
                                  "moderationReason":"人工复核后判定为高风险引导内容，禁止公开。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.moderationStatus").value("REJECTED"));

        String today = LocalDate.now().toString();

        mockMvc.perform(get("/api/v1/audit/records")
                        .header("xs-bbs-token", token)
                        .param("sourceType", "ADMIN")
                        .param("operatorKeyword", "林")
                        .param("currentStatus", "REJECTED")
                        .param("thoughtId", String.valueOf(thoughtId))
                        .param("dateFrom", today)
                        .param("dateTo", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].thoughtId").value((int) thoughtId))
                .andExpect(jsonPath("$.data[0].sourceType").value("ADMIN"))
                .andExpect(jsonPath("$.data[0].operatorDisplayName").value("林溪"))
                .andExpect(jsonPath("$.data[0].currentStatus").value("REJECTED"));
    }

    @Test
    void shouldPageAndExportAuditRecords() throws Exception {
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
        String today = LocalDate.now().toString();

        mockMvc.perform(get("/api/v1/audit/records/page")
                        .header("xs-bbs-token", token)
                        .param("pageNo", "1")
                        .param("pageSize", "2")
                        .param("dateFrom", today)
                        .param("dateTo", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/audit/records/export")
                        .header("xs-bbs-token", token)
                        .param("dateFrom", today)
                        .param("dateTo", today))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    String disposition = result.getResponse().getHeader("Content-Disposition");
                    String body = result.getResponse().getContentAsString();
                    if (contentType == null || !contentType.startsWith("text/csv")) {
                        throw new AssertionError("expected csv content type but got: " + contentType);
                    }
                    if (disposition == null || !disposition.contains("attachment;")) {
                        throw new AssertionError("expected attachment content disposition");
                    }
                    if (!body.contains("记录ID,念头ID,审核来源")) {
                        throw new AssertionError("expected csv header row");
                    }
                });
    }
}
