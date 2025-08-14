package com.quiz.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class SimpleRateLimiter {

    private final int maxTokens; // 桶的最大容量
    private final AtomicInteger tokens; // 当前桶中的令牌数量
    private long lastRefillTimestamp; // 上次填充令牌的时间戳
    private final long refillRateInMillis; // 令牌填充速率，单位毫秒

    public SimpleRateLimiter(int maxTokens, long refillRateInMillis) {
        this.maxTokens = maxTokens;
        this.tokens = new AtomicInteger(maxTokens); // 初始时填满令牌
        this.lastRefillTimestamp = System.currentTimeMillis();
        this.refillRateInMillis = refillRateInMillis;
    }

    public synchronized boolean tryAcquire() {
        refillTokens();

        if (tokens.get() > 0) {
            tokens.decrementAndGet();
            return true;
        }
        return false;
    }

    private void refillTokens() {
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastRefillTimestamp;

        // 根据经过的时间和填充速率计算需要添加多少令牌
        long newTokens = elapsedTime / refillRateInMillis;
        if (newTokens > 0) {
            int currentTokens = tokens.get();
            int updatedTokens = Math.min(currentTokens + (int)newTokens, maxTokens);
            tokens.set(updatedTokens);
            lastRefillTimestamp = now - (elapsedTime % refillRateInMillis);
        }
    }
}
