package com.ticketapp.demo.catalog;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.NoSuchElementException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/events/{eventId}/sections")
public class EventSectionController {

    private final EventSectionService sectionService;

    public EventSectionController(EventSectionService sectionService) {
        this.sectionService = sectionService;
    }

    @PostMapping
    public ResponseEntity<EventSection> createSection(@PathVariable UUID eventId,
                                                       @Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sectionService.saveSectionAndGenerateSeats(eventId, request));
    }

    @GetMapping
    public List<EventSection> getSections(@PathVariable UUID eventId) {
        return sectionService.getSectionsForEvent(eventId);
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<Void> deleteSection(@PathVariable UUID eventId, @PathVariable UUID sectionId) {
        sectionService.deleteSectionAndSeats(eventId, sectionId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }
}
