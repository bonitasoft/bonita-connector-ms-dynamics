package com.bonitasoft.connectors.dynamics;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void shouldReturnResultOnFirstAttempt() throws DynamicsException {
        var policy = new RetryPolicy(3);
        String result = policy.execute(() -> "success");
        assertThat(result).isEqualTo("success");
    }

    @Test
    void shouldRetryOnRetryableException() throws DynamicsException {
        var policy = new TestableRetryPolicy(3);
        var counter = new int[]{0};
        String result = policy.execute(() -> {
            counter[0]++;
            if (counter[0] < 3) {
                throw new DynamicsException("Rate limited", 429, true);
            }
            return "success";
        });
        assertThat(result).isEqualTo("success");
        assertThat(counter[0]).isEqualTo(3);
    }

    @Test
    void shouldNotRetryOnNonRetryableException() {
        var policy = new TestableRetryPolicy(3);
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new DynamicsException("Forbidden", 403, false);
        })).isInstanceOf(DynamicsException.class).hasMessageContaining("Forbidden");
    }

    @Test
    void shouldExhaustRetriesAndThrow() {
        var policy = new TestableRetryPolicy(2);
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new DynamicsException("Server error", 500, true);
        })).isInstanceOf(DynamicsException.class).hasMessageContaining("Server error");
    }

    @Test
    void shouldIdentifyRetryableStatusCodes() {
        assertThat(RetryPolicy.isRetryableStatusCode(429)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(500)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(502)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(503)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(400)).isFalse();
        assertThat(RetryPolicy.isRetryableStatusCode(401)).isFalse();
        assertThat(RetryPolicy.isRetryableStatusCode(403)).isFalse();
        assertThat(RetryPolicy.isRetryableStatusCode(404)).isFalse();
    }

    @Test
    void shouldCalculateExponentialWait() {
        var policy = new RetryPolicy(5);
        long wait0 = policy.calculateWait(0);
        long wait1 = policy.calculateWait(1);
        long wait2 = policy.calculateWait(2);
        // Exponential backoff with jitter: base wait doubles each attempt
        assertThat(wait0).isGreaterThanOrEqualTo(1000L);
        assertThat(wait1).isGreaterThanOrEqualTo(2000L);
        assertThat(wait2).isGreaterThanOrEqualTo(4000L);
    }

    @Test
    void shouldWrapUnexpectedExceptionInDynamicsException() {
        var policy = new TestableRetryPolicy(3);
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new IllegalStateException("unexpected");
        })).isInstanceOf(DynamicsException.class)
                .hasMessageContaining("Unexpected error");
    }

    /**
     * Testable RetryPolicy that skips actual Thread.sleep.
     */
    static class TestableRetryPolicy extends RetryPolicy {
        TestableRetryPolicy(int maxRetries) {
            super(maxRetries);
        }

        @Override
        void sleep(long millis) {
            // No-op for testing
        }
    }
}
