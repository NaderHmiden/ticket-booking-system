package com.ticketapp.demo.payment;

import com.ticketapp.demo.payment.internal.ProcessedPayment;
import com.ticketapp.demo.payment.internal.ProcessedPaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final ProcessedPaymentRepository processedPaymentRepository;

    public PaymentController(ProcessedPaymentRepository processedPaymentRepository) {
        this.processedPaymentRepository = processedPaymentRepository;
    }

    /**
     * Returns stub payment processing status for a holdId, if any.
     */
    @GetMapping("/{holdId}")
    public ResponseEntity<?> getStatus(@PathVariable String holdId) {
        return processedPaymentRepository.findById(holdId)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(toResponse(p)))
                .orElseGet(() -> ResponseEntity.notFound()
                        .build());
    }

    private static PaymentStatusResponse toResponse(ProcessedPayment payment) {
        return new PaymentStatusResponse(
                payment.getHoldId(),
                payment.getStatus(),
                payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null
        );
    }
}
