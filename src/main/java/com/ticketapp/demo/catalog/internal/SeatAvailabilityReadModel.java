package com.ticketapp.demo.catalog.internal;


import com.ticketapp.demo.catalog.SeatStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class SeatAvailabilityReadModel {

    private static final String KEY_PREFIX = "catalog:seat-status:";
    private static final String HELD = "HELD";
    private static final String BOOKED = "BOOKED";

    private final StringRedisTemplate redisTemplate;

    public SeatAvailabilityReadModel(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<SeatStatus> getStatus(UUID seatId) {
        String value = redisTemplate.opsForValue().get(key(seatId));
        if (value == null) {
            return Optional.empty();
        }
        return switch (value) {
            case HELD -> Optional.of(SeatStatus.HELD);
            case BOOKED -> Optional.of(SeatStatus.BOOKED);
            default -> Optional.empty();
        };
    }

    public java.util.Map<UUID, SeatStatus> getStatuses(java.util.List<UUID> seatIds) {
        if (seatIds.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.List<String> keys = seatIds.stream().map(this::key).toList();
        java.util.List<String> values = redisTemplate.opsForValue().multiGet(keys);
        java.util.Map<UUID, SeatStatus> result = new java.util.HashMap<>();
        if (values == null) {
            return result;
        }
        for (int i = 0; i < seatIds.size(); i++) {
            String value = values.get(i);
            if (HELD.equals(value)) {
                result.put(seatIds.get(i), SeatStatus.HELD);
            } else if (BOOKED.equals(value)) {
                result.put(seatIds.get(i), SeatStatus.BOOKED);
            }
        }
        return result;
    }

    public void markHeld(UUID seatId, Duration ttl) {
        Duration effectiveTtl = ttl.isNegative() || ttl.isZero() ? Duration.ofSeconds(1) : ttl;
        redisTemplate.opsForValue().set(key(seatId), HELD, effectiveTtl);
    }

    public void markBooked(UUID seatId) {
        redisTemplate.opsForValue().set(key(seatId), BOOKED);
    }

    public void clearHold(UUID seatId) {
        String current = redisTemplate.opsForValue().get(key(seatId));
        if (HELD.equals(current)) {
            redisTemplate.delete(key(seatId));
        }
    }

    private String key(UUID seatId) {
        return KEY_PREFIX + seatId;
    }
}
