package com.xs.bbs.audit;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.xs.bbs.ai.ThoughtAiProfileView;
import com.xs.bbs.ai.ThoughtModerationStatus;
import com.xs.bbs.common.ApiResponse;
import com.xs.bbs.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.nio.charset.StandardCharsets;

@SaCheckRole("admin")
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/thoughts")
    public ApiResponse<List<AuditThoughtView>> thoughts(
            @RequestParam(required = false) ThoughtModerationStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(auditService.listThoughts(status, keyword));
    }

    @GetMapping("/summary")
    public ApiResponse<AuditSummaryView> summary() {
        return ApiResponse.ok(auditService.summary());
    }

    @GetMapping("/thoughts/{thoughtId}/records")
    public ApiResponse<List<AuditRecordView>> records(@PathVariable Long thoughtId) {
        return ApiResponse.ok(auditService.records(thoughtId));
    }

    @GetMapping("/records")
    public ApiResponse<List<AuditRecordView>> records(
            @RequestParam(required = false) AuditSourceType sourceType,
            @RequestParam(required = false) String operatorKeyword,
            @RequestParam(required = false) ThoughtModerationStatus currentStatus,
            @RequestParam(required = false) Long thoughtId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ApiResponse.ok(auditService.records(sourceType, operatorKeyword, currentStatus, thoughtId, keyword, dateFrom, dateTo));
    }

    @GetMapping("/records/page")
    public ApiResponse<PageResponse<AuditRecordView>> recordPage(
            @RequestParam(required = false) AuditSourceType sourceType,
            @RequestParam(required = false) String operatorKeyword,
            @RequestParam(required = false) ThoughtModerationStatus currentStatus,
            @RequestParam(required = false) Long thoughtId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(auditService.recordPage(
                sourceType,
                operatorKeyword,
                currentStatus,
                thoughtId,
                keyword,
                dateFrom,
                dateTo,
                pageNo,
                pageSize
        ));
    }

    @GetMapping(value = "/records/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportRecords(
            @RequestParam(required = false) AuditSourceType sourceType,
            @RequestParam(required = false) String operatorKeyword,
            @RequestParam(required = false) ThoughtModerationStatus currentStatus,
            @RequestParam(required = false) Long thoughtId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        String csv = auditService.exportRecordsCsv(sourceType, operatorKeyword, currentStatus, thoughtId, keyword, dateFrom, dateTo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + auditService.buildExportFilename() + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/thoughts/{thoughtId}/decision")
    public ApiResponse<ThoughtAiProfileView> decide(
            @PathVariable Long thoughtId,
            @Valid @RequestBody AuditDecisionRequest request
    ) {
        return ApiResponse.ok(auditService.decide(thoughtId, StpUtil.getLoginIdAsLong(), request));
    }
}
