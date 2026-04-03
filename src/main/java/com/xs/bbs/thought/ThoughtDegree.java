package com.xs.bbs.thought;

public enum ThoughtDegree {
    CASUAL("杂念", 1.0D),
    FOCUSED("心念", 1.5D),
    OBSESSION("执念", 2.0D);

    private final String label;
    private final double weight;

    ThoughtDegree(String label, double weight) {
        this.label = label;
        this.weight = weight;
    }

    public String getLabel() {
        return label;
    }

    public double getWeight() {
        return weight;
    }
}
