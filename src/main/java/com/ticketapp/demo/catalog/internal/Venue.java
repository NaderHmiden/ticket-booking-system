package com.ticketapp.demo.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_venue")
public class Venue {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Venue() { } // JPA

    public Venue(UUID id, String name, String city, String timezone) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.timezone = timezone;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getTimezone() { return timezone; }
    public Instant getCreatedAt() { return createdAt; }
}
