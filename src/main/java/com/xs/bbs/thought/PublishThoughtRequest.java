package com.xs.bbs.thought;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublishThoughtRequest(
        @NotBlank @Size(max = 200) String content,
        @NotNull ThoughtDegree degree,
        Boolean allowRecommendation,
        Boolean publicVisible
) {
    public PublishThoughtRequest {
        allowRecommendation = allowRecommendation == null || allowRecommendation;
        publicVisible = publicVisible == null || publicVisible;
    }

    public PublishThoughtRequest(String content, ThoughtDegree degree) {
        this(content, degree, true, true);
    }
}
