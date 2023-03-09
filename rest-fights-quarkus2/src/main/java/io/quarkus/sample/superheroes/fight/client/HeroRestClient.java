package io.quarkus.sample.superheroes.fight.client;

import javax.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;

/**
 * <a href="https://quarkus.io/guides/rest-client-reactive">Quarkus Reactive Rest Client</a> that talks to the Hero service.
 * <p>
 *   It is declared package-private so that the default client can be decorated by {@link HeroClient}. Consumers should use {@link HeroClient}.
 * </p>
 */
@ApplicationScoped
class HeroRestClient {
	/**
	 * HTTP <code>GET</code> call to {@code /api/heroes/random} on the Heroes service
	 * @return A {@link Hero}
	 * @see HeroClient#findRandomHero()
	 */
	Uni<Hero> findRandomHero() {
    return Uni.createFrom().item(new Hero("Superman", 1000, "", "Super strong"));
  }
  
	/**
	 * HTTP <code>GET</code> call to {@code /api/heroes/hello} on the Heroes service
	 * @return A "hello" from Heroes
	 */
  Uni<String> hello() {
    return Uni.createFrom().item("Hello Heroes");
  }
}
