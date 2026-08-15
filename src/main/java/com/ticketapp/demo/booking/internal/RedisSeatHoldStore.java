package com.ticketapp.demo.booking.internal;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * The mechanism that makes "the same seat is never sold twice" true. Two
 * operations, both atomic at the Redis level, no application-side locking:
 *
 * <h2>Acquire — {@code SET seat:{id} {holdId} NX PX <ttl>}</h2>
 * <p>Redis executes commands single-threaded; {@code NX} ("only set if the
 * key does not already exist") is evaluated and applied as one atomic
 * operation on the server. When two requests race for the same seat at
 * literally the same instant, Redis still serializes them internally —
 * exactly one {@code SET NX} call observes an absent key and succeeds; every
 * other one observes a present key and fails immediately. There is no
 * window where both could see "not held" — that's what "atomic" buys here,
 * and it's the entire reason a database row lock (held open for the length
 * of a checkout) isn't needed. {@link #acquire} maps directly to this call
 * via {@link org.springframework.data.redis.core.ValueOperations#setIfAbsent}.
 *
 * <h2>Release — Lua compare-and-delete</h2>
 * <p>Releasing looks like it should be a simple {@code DEL}, but a plain
 * "get, check, then delete" from application code has a real race: between
 * the GET and the DEL, the TTL could expire and a *different* request could
 * acquire the now-free seat — and this stale release would then delete
 * their brand-new hold instead of the (already-gone) one it meant to
 * release. The fix is the standard safe-unlock pattern for Redis-based
 * locks: a single Lua script that checks-and-deletes atomically on the
 * server, so there's no gap between "is this still my hold?" and "delete
 * it" for anything else to land in.
 */
@Component
public class RedisSeatHoldStore {

    private static final String KEY_PREFIX = "booking:seat-hold:";

    private static final RedisScript<Long> COMPARE_AND_DELETE = new DefaultRedisScript<>(
            """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisSeatHoldStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to acquire the seat. Returns the winning holdId if this call
     * won the race, or empty if the seat was already held by someone else.
     * This is the ENTIRE decision — there is no retry, no queue, no wait;
     * the caller gets a definitive yes/no in one Redis round trip, which is
     * what lets the REST layer return a fast, clean rejection instead of a
     * timeout under flash-sale load.
     */
    public Optional<UUID> acquire(UUID seatId, Duration ttl) {
        UUID holdId = UUID.randomUUID();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key(seatId), holdId.toString(), ttl);
        return Boolean.TRUE.equals(acquired) ? Optional.of(holdId) : Optional.empty();
    }

    /**
     * Releases the seat, but ONLY if {@code holdId} is still the current
     * owner. Returns true if this call actually released it, false if there
     * was nothing to release (already expired, already released, or — most
     * importantly — now owned by a different, later hold that this stale
     * call must not be allowed to touch).
     */
    public boolean release(UUID seatId, UUID holdId) {
        Long result = redisTemplate.execute(
                COMPARE_AND_DELETE,
                Collections.singletonList(key(seatId)),
                holdId.toString()
        );
        return result != null && result == 1L;
    }

    /**
     * Whether the given holdId is (still) the current holder of the seat.
     * Used by the confirm/release flow in later steps to validate a holdId
     * before acting on it, without mutating anything.
     */
    public boolean isCurrentHolder(UUID seatId, UUID holdId) {
        String current = redisTemplate.opsForValue().get(key(seatId));
        return holdId.toString().equals(current);
    }

    private String key(UUID seatId) {
        return KEY_PREFIX + seatId;
    }
}

