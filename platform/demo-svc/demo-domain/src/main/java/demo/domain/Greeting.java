package demo.domain;

/** Pure domain value object — no framework imports (ADR-007). */
public record Greeting(String message) {}
