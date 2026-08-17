package com.ticketapp.demo;

import com.ticketapp.demo.booking.HoldResult;
import com.ticketapp.demo.booking.internal.QuantityHoldStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency test for GENERAL_ADMISSION ticket holds.
 * 500 threads compete to reserve tickets from a stock of 200.
 * Validates that the total acquired never exceeds stock and matches Redis state.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class GATicketConcurrentHoldIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @Autowired
    private QuantityHoldStrategy quantityHoldStrategy;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private UUID ticketTypeId;
    private UUID eventId;
    private static final int INITIAL_STOCK = 200;
    private static final int NUM_THREADS = 500;

    @BeforeEach
    public void setUp() {
        ticketTypeId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        
        // Initialize ticket availability in Redis
        String ticketAvailableKey = "booking:ticket-type-available:" + ticketTypeId;
        redisTemplate.delete(ticketAvailableKey);
        redisTemplate.opsForValue().set(ticketAvailableKey, String.valueOf(INITIAL_STOCK));
    }

    @Test
    public void testConcurrentQuantityHolds() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);
        ConcurrentHashMap<UUID, Integer> acquiredByHold = new ConcurrentHashMap<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    int quantity = (int) (Math.random() * 3) + 1; // Random quantity 1-3
                    UUID userId = UUID.randomUUID();
                    
                    HoldResult result = quantityHoldStrategy.acquireQuantity(
                            ticketTypeId, quantity, eventId, userId);
                    
                    acquiredByHold.put(result.holdId(), quantity);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    // Expected when stock is exhausted
                }
            });
        }

        executor.shutdown();
        boolean completed = executor.awaitTermination(30, TimeUnit.SECONDS);
        assertTrue(completed, "Executor did not complete within timeout");

        // Calculate total acquired
        int totalAcquired = acquiredByHold.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        // Verify totals never exceeded stock
        assertTrue(totalAcquired <= INITIAL_STOCK,
                "Total acquired (" + totalAcquired + ") exceeds initial stock (" + INITIAL_STOCK + ")");

        // Verify Redis state matches: final available = initial stock - total acquired
        String ticketAvailableKey = "booking:ticket-type-available:" + ticketTypeId;
        String redisValue = redisTemplate.opsForValue().get(ticketAvailableKey);
        assertNotNull(redisValue, "Ticket availability key not found in Redis");
        
        int finalAvailable = Integer.parseInt(redisValue);
        int expectedAvailable = INITIAL_STOCK - totalAcquired;
        
        assertEquals(expectedAvailable, finalAvailable,
                "Redis final available (" + finalAvailable + ") does not match expected (" + expectedAvailable + ")");

        // Verify we acquired some tickets (stock should be exhausted or nearly exhausted)
        assertTrue(successCount.get() > 0, "No holds were successfully acquired");
        System.out.println("Successfully acquired " + successCount.get() + " holds, total quantity: " + totalAcquired);
    }
}
