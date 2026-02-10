package team2.parallax.api;

public class RateLimiter {
        // 60 seconds / 5 requests = 12,000 milliseconds
        private static final long MIN_INTERVAL_MS = 12000;
        private static long lastRequestTime = 0;

        /**
         * Call this before any API request to stay within free-tier limits.
         */
        public static synchronized void waitForNext() {
            long currentTime = System.currentTimeMillis();
            long timeSinceLast = currentTime - lastRequestTime;

            if (timeSinceLast < MIN_INTERVAL_MS) {
                try {
                    long sleepTime = MIN_INTERVAL_MS - timeSinceLast;
                    System.out.println("[RateLimiter] Throttling: Waiting " + (sleepTime / 1000) + " seconds...");
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // Update the timestamp to now
            lastRequestTime = System.currentTimeMillis();
        }
    }

