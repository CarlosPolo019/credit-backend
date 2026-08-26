package com.fya.credits.config;

import com.google.cloud.firestore.Firestore;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports Firestore reachability under /actuator/health as the "firestore"
 * component. A plain JVM-alive check isn't enough to know the API can
 * actually serve requests, since every endpoint depends on Firestore.
 */
@Component
public class FirestoreHealthIndicator implements HealthIndicator {
  private final Firestore firestore;

  public FirestoreHealthIndicator(Firestore firestore) {
    this.firestore = firestore;
  }

  @Override
  public Health health() {
    try {
      firestore.collection("users").limit(1).get().get(3, TimeUnit.SECONDS);
      return Health.up().build();
    } catch (Exception ex) {
      return Health.down(ex).build();
    }
  }
}
