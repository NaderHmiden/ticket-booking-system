package com.ticketapp.demo.payment.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.demo.common.event.KafkaTopics;
import com.ticketapp.demo.common.event.PaymentEventPayloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Asynchronous payment stub. Knows only holdId + amount — never seats, tickets, or events.
 */
@Component
public class PaymentRequestListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentRequestListener.class);

    private final ProcessedPaymentRepository processedPaymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final double failureRate;

    public PaymentRequestListener(
            ProcessedPaymentRepository processedPaymentRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${payment.stub.failure-rate:0.1}") double failureRate) {
        this.processedPaymentRepository = processedPaymentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.failureRate = failureRate;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_REQUESTS, groupId = "payment-request-processor")
    @Transactional
    public void onPaymentRequested(String message) {
        try {
            PaymentEventPayloads.PaymentRequested request =
                    objectMapper.readValue(message, PaymentEventPayloads.PaymentRequested.class);
            process(request.holdId());
        } catch (Exception e) {
            log.warn("Skipping malformed payment.requests message: {}", message, e);
        }
    }

    private void process(String holdId) throws Exception {
        boolean firstTime = processedPaymentRepository.insertIfAbsent(holdId, ProcessedPayment.STATUS_PROCESSING);

        if (!firstTime) {
            ProcessedPayment existing = processedPaymentRepository.findById(holdId).orElse(null);
            if (existing == null) {
                return;
            }
            if (!ProcessedPayment.STATUS_PROCESSING.equals(existing.getStatus())) {
                republishExistingResult(existing);
            } else {
                log.info("Payment for hold {} still PROCESSING — ignoring duplicate request", holdId);
            }
            return;
        }

        boolean fail = ThreadLocalRandom.current().nextDouble() < failureRate;
        if (fail) {
            processedPaymentRepository.updateStatusIf(
                    holdId, ProcessedPayment.STATUS_PROCESSING, ProcessedPayment.STATUS_FAILED);
            PaymentEventPayloads.PaymentFailed failed = new PaymentEventPayloads.PaymentFailed(
                    holdId, "stub_random_failure", Instant.now());
            publish(KafkaTopics.PAYMENT_RESULTS, holdId, failed);
            log.info("Payment FAILED (stub) for hold {}", holdId);
        } else {
            processedPaymentRepository.updateStatusIf(
                    holdId, ProcessedPayment.STATUS_PROCESSING, ProcessedPayment.STATUS_SUCCEEDED);
            PaymentEventPayloads.PaymentSucceeded succeeded = new PaymentEventPayloads.PaymentSucceeded(
                    holdId, Instant.now());
            publish(KafkaTopics.PAYMENT_RESULTS, holdId, succeeded);
            log.info("Payment SUCCEEDED (stub) for hold {}", holdId);
        }
    }

    private void republishExistingResult(ProcessedPayment existing) throws Exception {
        String holdId = existing.getHoldId();
        if (ProcessedPayment.STATUS_SUCCEEDED.equals(existing.getStatus())) {
            publish(KafkaTopics.PAYMENT_RESULTS, holdId,
                    new PaymentEventPayloads.PaymentSucceeded(holdId, Instant.now()));
            log.info("Re-published PaymentSucceeded for already-processed hold {}", holdId);
        } else if (ProcessedPayment.STATUS_FAILED.equals(existing.getStatus())) {
            publish(KafkaTopics.PAYMENT_RESULTS, holdId,
                    new PaymentEventPayloads.PaymentFailed(holdId, "already_failed", Instant.now()));
            log.info("Re-published PaymentFailed for already-processed hold {}", holdId);
        }
    }

    private void publish(String topic, String key, Object event) throws Exception {
        kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(event));
    }
}
