package com.xs.bbs.thought;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("thought_post")
public class ThoughtPostEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String content;

    @TableField("degree_code")
    private String degreeCode;

    private Boolean allowRecommendation;
    private Boolean publicVisible;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDegreeCode() {
        return degreeCode;
    }

    public void setDegreeCode(String degreeCode) {
        this.degreeCode = degreeCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getAllowRecommendation() {
        return allowRecommendation;
    }

    public void setAllowRecommendation(Boolean allowRecommendation) {
        this.allowRecommendation = allowRecommendation;
    }

    public Boolean getPublicVisible() {
        return publicVisible;
    }

    public void setPublicVisible(Boolean publicVisible) {
        this.publicVisible = publicVisible;
    }
}
