package com.ticketapp.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.demo.booking.HoldStrategy;
import com.ticketapp.demo.booking.internal.Booking;
import com.ticketapp.demo.booking.internal.BookingRepository;
import com.ticketapp.demo.booking.internal.HoldStrategyFactory;
import com.ticketapp.demo.catalog.internal.EventListing;
import com.ticketapp.demo.catalog.internal.EventRepository;
import com.ticketapp.demo.catalog.internal.EventType;
import com.ticketapp.demo.catalog.internal.Venue;
import com.ticketapp.demo.catalog.internal.VenueRepository;
import com.ticketapp.demo.common.event.KafkaTopics;
import com.ticketapp.demo.common.event.PaymentEventPayloads;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies booking-side idempotence on payment results and expired-hold anomaly publishing.
 * Uses Embedded Kafka + in-memory H2 (no Docker required).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "payment.stub.failure-rate=0",
                "spring.datasource.url=jdbc:h2:mem:payment_flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        }
)
@EmbeddedKafka(
        partitions = 1,
        topics = {
                KafkaTopics.PAYMENT_REQUESTS,
                KafkaTopics.PAYMENT_RESULTS,
                KafkaTopics.PAYMENT_ANOMALIES
        }
)
class PaymentFlowIdempotencyIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockBean
    private HoldStrategyFactory holdStrategyFactory;

    /** Redis-backed strategies are not needed; exclude their construction via mocks if required. */
    @MockBean
    private com.ticketapp.demo.booking.internal.RedisSeatHoldStore redisSeatHoldStore;

    @MockBean
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    private HoldStrategy holdStrategy;
    private UUID eventId;
    private UUID resourceId;

    @BeforeEach
    void setUp() {
        holdStrategy = Mockito.mock(HoldStrategy.class);
        when(holdStrategyFactory.resolve(any(EventType.class))).thenReturn(holdStrategy);
        when(holdStrategy.confirm(any(), any())).thenReturn(true);
        when(holdStrategy.release(any(), any())).thenReturn(true);

        Venue venue = venueRepository.save(new Venue(UUID.randomUUID(), "Test Arena", "Paris", "Europe/Paris"));
        EventListing event = new EventListing(
                UUID.randomUUID(), venue.getId(), "Test Show", "CONCERT", Instant.now().plusSeconds(3600));
        eventId = eventRepository.save(event).getId();
        resourceId = UUID.randomUUID();
    }

    @Test
    void paymentSucceededPublishedThrice_confirmsBookingAndCallsStrategyOnce() throws Exception {
        String holdId = UUID.randomUUID().toString();
        Booking booking = new Booking(UUID.randomUUID(), holdId, eventId, resourceId, UUID.randomUUID(), new BigDecimal("42.00"));
        booking.setStatus("PAYMENT_PENDING");
        booking.setPaymentRequested(true);
        bookingRepository.save(booking);

        PaymentEventPayloads.PaymentRequested requested = new PaymentEventPayloads.PaymentRequested(
                holdId, booking.getAmount(), Instant.now());
        kafkaTemplate.send(KafkaTopics.PAYMENT_REQUESTS, holdId, objectMapper.writeValueAsString(requested)).get();

        PaymentEventPayloads.PaymentSucceeded succeeded =
                new PaymentEventPayloads.PaymentSucceeded(holdId, Instant.now());
        String successJson = objectMapper.writeValueAsString(succeeded);
        for (int i = 0; i < 3; i++) {
            kafkaTemplate.send(KafkaTopics.PAYMENT_RESULTS, holdId, successJson).get();
        }

        assertEquals("CONFIRMED", awaitStatus(holdId, "CONFIRMED").getStatus());
        verify(holdStrategy, times(1)).confirm(resourceId, UUID.fromString(holdId));
    }

    @Test
    void latePaymentSucceededOnReleasedHold_publishesExpiredHoldAnomaly() throws Exception {
        String holdId = UUID.randomUUID().toString();
        Booking booking = new Booking(UUID.randomUUID(), holdId, eventId, resourceId, UUID.randomUUID(), new BigDecimal("19.99"));
        booking.setStatus("RELEASED");
        booking.setPaymentRequested(true);
        bookingRepository.save(booking);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("anomaly-test", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (Consumer<String, String> consumer =
                     new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, KafkaTopics.PAYMENT_ANOMALIES);

            PaymentEventPayloads.PaymentSucceeded succeeded =
                    new PaymentEventPayloads.PaymentSucceeded(holdId, Instant.now());
            kafkaTemplate.send(KafkaTopics.PAYMENT_RESULTS, holdId, objectMapper.writeValueAsString(succeeded)).get();

            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.PAYMENT_ANOMALIES, Duration.ofSeconds(15));

            PaymentEventPayloads.PaymentReceivedForExpiredHold anomaly =
                    objectMapper.readValue(record.value(), PaymentEventPayloads.PaymentReceivedForExpiredHold.class);
            assertEquals(holdId, anomaly.holdId());
            assertEquals(0, new BigDecimal("19.99").compareTo(anomaly.amount()));
        }

        Booking reloaded = bookingRepository.findByHoldId(holdId).orElseThrow();
        assertEquals("RELEASED", reloaded.getStatus());
        verify(holdStrategy, Mockito.never()).confirm(any(), any());
        verify(holdStrategy, Mockito.never()).release(any(), any());
    }

    private Booking awaitStatus(String holdId, String expected) throws InterruptedException {
        AssertionError last = null;
        for (int i = 0; i < 50; i++) {
            try {
                Booking booking = bookingRepository.findByHoldId(holdId).orElseThrow();
                assertEquals(expected, booking.getStatus());
                return booking;
            } catch (AssertionError | java.util.NoSuchElementException e) {
                last = e instanceof AssertionError ae ? ae : new AssertionError(e);
                Thread.sleep(200);
            }
        }
        throw last != null ? last : new AssertionError("timeout waiting for status " + expected);
    }
}
