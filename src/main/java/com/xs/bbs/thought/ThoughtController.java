package com.xs.bbs.thought;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xs.bbs.ai.ThoughtAiProfileView;
import com.xs.bbs.ai.ThoughtAiService;
import com.xs.bbs.common.ApiResponse;
import com.xs.bbs.common.SearchSupport;
import com.xs.bbs.match.MatchCandidateView;
import com.xs.bbs.match.MatchService;
import com.xs.bbs.social.RelationshipService;
import com.xs.bbs.user.UserService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/thoughts")
public class ThoughtController {

    private final ThoughtRepository thoughtRepository;
    private final ThoughtApplicationService thoughtApplicationService;
    private final MatchService matchService;
    private final ThoughtAiService thoughtAiService;
    private final RelationshipService relationshipService;
    private final UserService userService;

    public ThoughtController(
            ThoughtRepository thoughtRepository,
            ThoughtApplicationService thoughtApplicationService,
            MatchService matchService,
            ThoughtAiService thoughtAiService,
            RelationshipService relationshipService,
            UserService userService
    ) {
        this.thoughtRepository = thoughtRepository;
        this.thoughtApplicationService = thoughtApplicationService;
        this.matchService = matchService;
        this.thoughtAiService = thoughtAiService;
        this.relationshipService = relationshipService;
        this.userService = userService;
    }

    @SaCheckLogin
    @PostMapping
    public ApiResponse<PublishedThoughtResponse> publish(@Valid @RequestBody PublishThoughtRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        ThoughtPost thought = thoughtApplicationService.publish(userId, request);
        ThoughtAiProfileView analysis = thoughtAiService.getView(thought.id());
        List<MatchCandidateView> candidates = matchService.findCandidatesForThought(thought);
        return ApiResponse.ok(new PublishedThoughtResponse(thought, analysis, candidates));
    }

    @GetMapping
    public ApiResponse<List<ThoughtPost>> listAll(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(thoughtRepository.findAll().stream()
                .filter(thoughtAiService::allowsPublicDisplay)
                .filter(thought -> {
                    var profile = userService.getRequiredUser(thought.userId());
                    return SearchSupport.matches(keyword, thought.content(), profile.nickname(), profile.bio());
                })
                .toList());
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<ThoughtPost>> listByUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String keyword
    ) {
        var profile = userService.getRequiredUser(userId);
        return ApiResponse.ok(thoughtRepository.findByUserId(userId).stream()
                .filter(thoughtAiService::allowsPublicDisplay)
                .filter(thought -> SearchSupport.matches(keyword, thought.content(), profile.nickname(), profile.bio()))
                .toList());
    }

    @SaCheckLogin
    @GetMapping("/me")
    public ApiResponse<List<ThoughtPost>> listMine(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(thoughtRepository.findByUserId(StpUtil.getLoginIdAsLong()).stream()
                .filter(thought -> SearchSupport.matches(keyword, thought.content()))
                .toList());
    }

    @SaCheckLogin
    @GetMapping("/following/me")
    public ApiResponse<List<ThoughtPost>> listFollowingFeed(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(relationshipService.listFollowingPublicThoughts(StpUtil.getLoginIdAsLong(), keyword));
    }

    @SaCheckLogin
    @GetMapping("/me/analysis")
    public ApiResponse<List<ThoughtAiProfileView>> myAnalyses() {
        return ApiResponse.ok(thoughtAiService.getViewsByUserId(StpUtil.getLoginIdAsLong()));
    }

    @SaCheckLogin
    @GetMapping("/{thoughtId}/analysis")
    public ApiResponse<ThoughtAiProfileView> analysis(@PathVariable Long thoughtId) {
        ThoughtPost thought = thoughtRepository.findById(thoughtId)
                .orElseThrow(() -> new IllegalArgumentException("thought does not exist"));
        if (!thought.userId().equals(StpUtil.getLoginIdAsLong())) {
            throw new IllegalArgumentException("thought does not belong to current user");
        }
        return ApiResponse.ok(thoughtAiService.getView(thoughtId));
    }
}
