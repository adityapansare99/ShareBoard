package com.cb.patterns.proxy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenBucketTest {

    @Test
    void testInitialTokensAvailable() {
        TokenBucket bucket = new TokenBucket(5, 2, 1000);
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertEquals(0, bucket.getAvailableTokens());
    }

    @Test
    void testRateLimitedWhenEmpty() {
        TokenBucket bucket = new TokenBucket(2, 1, 1000);
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertFalse(bucket.tryConsume());
        assertFalse(bucket.tryConsume());
    }

    @Test
    void testRefillOverTime() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(1, 1, 100);
        assertTrue(bucket.tryConsume());
        assertEquals(0, bucket.getAvailableTokens());
        Thread.sleep(150);
        assertEquals(1, bucket.getAvailableTokens());
        assertTrue(bucket.tryConsume());
    }

    @Test
    void testMaxCapacityNotExceeded() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(2, 10, 100);
        assertEquals(2, bucket.getAvailableTokens());
        Thread.sleep(150);
        assertEquals(2, bucket.getAvailableTokens()); // Should not exceed capacity
    }
}