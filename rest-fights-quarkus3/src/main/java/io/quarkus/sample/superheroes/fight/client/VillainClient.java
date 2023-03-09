package io.quarkus.sample.superheroes.fight.client;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;

/**
 * Bean to be used for interacting with the Villain service.
 * <p>
 *   Uses the <a href="https://docs.oracle.com/javaee/7/tutorial/jaxrs-client001.htm">JAX-RS Rest Client</a> with the <a href="https://quarkus.io/guides/resteasy-reactive#resteasy-reactive-client">RESTEasy Reactive client</a>.
 * </p>
 */
@ApplicationScoped
public class VillainClient {
  /**
   * Finds a random {@link Villain}. The retry logic is applied to the result of the {@link CircuitBreaker}, meaning that retries that return failures could trigger the breaker to open.
   * @return A random {@link Villain}
   */
  @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
  @CircuitBreakerName("findRandomVillain")
  @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
  public Uni<Villain> findRandomVillain() {
    // Want the 404 handling to be part of the circuit breaker
    // This means that the 404 responses aren't considered errors by the circuit breaker
    return Uni.createFrom().item(new Villain("Darth Vader", 100, "", "Light sabre"))
      .onFailure(Is404Exception.IS_404).recoverWithNull();
  }

  /**
   * Calls hello on the Villains service.
   * @return A "hello" from Villains
   */
  public Uni<String> helloVillains() {
    return Uni.createFrom().item("Hello villains");
  }
}
