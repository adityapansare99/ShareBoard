package com.cb.patterns.proxy;

public class TokenBucket {

    private final int capacity;
    private final int refillRate;
    private final long refillIntervalMs;

    private int tokens;
    private long lastRefillTime;

    public TokenBucket(int capacity, int refillRate, long refillIntervalMs) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.refillIntervalMs = refillIntervalMs;
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    /**
     * Try to consume one token. Returns true if allowed, false if rate-limited.
     */
    public synchronized boolean tryConsume() {
        refill();
        if (tokens > 0) {
            tokens--;
            return true;
        }
        return false;
    }

    /**
     * Refill tokens based on elapsed time since last refill.
     */
    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTime;

        if (elapsed >= refillIntervalMs) {
            int intervals = (int) (elapsed / refillIntervalMs);
            tokens = Math.min(capacity, tokens + intervals * refillRate);
            lastRefillTime = now - (elapsed % refillIntervalMs); // Keep remainder
        }
    }

    public synchronized int getAvailableTokens() {
        refill();
        return tokens;
    }

    public int getCapacity() { return capacity; }
}
