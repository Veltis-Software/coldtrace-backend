package com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="asset_integration_outbox")
public class AssetIntegrationOutboxEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
  @Column(name="event_id",nullable=false,unique=true,length=36) String eventId;
  @Column(name="payload",nullable=false,columnDefinition="JSON") String payload;
  @Column(name="published_at") LocalDateTime publishedAt;
  @Column(name="attempts",nullable=false) int attempts;
}
