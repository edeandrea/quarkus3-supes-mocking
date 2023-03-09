package io.quarkus.sample.superheroes.fight.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.config.FightConfig;

import io.smallrye.mutiny.Uni;

/**
 * Business logic for the Fight service
 */
@ApplicationScoped
public class FightService {
	private final HeroClient heroClient;
	private final VillainClient villainClient;
	private final FightConfig fightConfig;
	private final Random random = new Random();

	public FightService(HeroClient heroClient, VillainClient villainClient, FightConfig fightConfig) {
		this.heroClient = heroClient;
		this.villainClient = villainClient;
		this.fightConfig = fightConfig;
  }

	public Uni<List<Fight>> findAllFights() {
    Log.debug("Getting all fights");
		return Fight.listAll();
	}

	public Uni<Fight> findFightById(String id) {
    Log.debugf("Finding fight by id = %s", id);
		return Fight.findById(new ObjectId(id));
	}

  @Fallback(fallbackMethod = "fallbackRandomFighters")
	public Uni<Fighters> findRandomFighters() {
    Log.debug("Finding random fighters");

    var villain = findRandomVillain()
      .onItem().ifNull().continueWith(this::createFallbackVillain);

		var hero = findRandomHero()
      .onItem().ifNull().continueWith(this::createFallbackHero);

		return Uni.combine()
			.all()
			.unis(hero, villain)
			.combinedWith(Fighters::new);
	}

	@Fallback(fallbackMethod = "fallbackRandomHero")
	Uni<Hero> findRandomHero() {
    Log.debug("Finding a random hero");
		return this.heroClient.findRandomHero()
			.invoke(hero -> Log.debugf("Got random hero: %s", hero));
	}

	@Fallback(fallbackMethod = "fallbackRandomVillain")
	Uni<Villain> findRandomVillain() {
    Log.debug("Finding a random villain");
		return this.villainClient.findRandomVillain()
			.invoke(villain -> Log.debugf("Got random villain: %s", villain));
	}

  @Timeout(value = 5, unit = ChronoUnit.SECONDS)
  @Fallback(fallbackMethod = "fallbackHelloHeroes")
  public Uni<String> helloHeroes() {
    Log.debug("Pinging heroes service");
    return this.heroClient.helloHeroes()
      .invoke(hello -> Log.debugf("Got %s from the Heroes microservice", hello));
  }

  Uni<Fighters> fallbackRandomFighters() {
    return Uni.createFrom().item(new Fighters(createFallbackHero(), createFallbackVillain()))
      .invoke(() -> Log.warn("Falling back on finding random fighters"));
  }

  Uni<String> fallbackHelloHeroes() {
    return Uni.createFrom().item("Could not invoke the Heroes microservice")
      .invoke(message -> Log.warn(message));
  }

  @Timeout(value = 5, unit = ChronoUnit.SECONDS)
  @Fallback(fallbackMethod = "fallbackHelloVillains")
  public Uni<String> helloVillains() {
    Log.debug("Pinging villains service");
    return this.villainClient.helloVillains()
      .invoke(hello -> Log.debugf("Got %s from the Villains microservice", hello));
  }

  Uni<String> fallbackHelloVillains() {
    return Uni.createFrom().item("Could not invoke the Villains microservice")
      .invoke(message -> Log.warn(message));
  }

	Uni<Hero> fallbackRandomHero() {
		return Uni.createFrom().item(this::createFallbackHero)
			.invoke(h -> Log.warn("Falling back on Hero"));
	}

	private Hero createFallbackHero() {
		return new Hero(
			this.fightConfig.hero().fallback().name(),
			this.fightConfig.hero().fallback().level(),
			this.fightConfig.hero().fallback().picture(),
			this.fightConfig.hero().fallback().powers()
		);
	}

	Uni<Villain> fallbackRandomVillain() {
		return Uni.createFrom().item(this::createFallbackVillain)
			.invoke(v -> Log.warn("Falling back on Villain"));
	}

	private Villain createFallbackVillain() {
		return new Villain(
			this.fightConfig.villain().fallback().name(),
			this.fightConfig.villain().fallback().level(),
			this.fightConfig.villain().fallback().picture(),
			this.fightConfig.villain().fallback().powers()
		);
	}

	public Uni<Fight> performFight(@NotNull @Valid Fighters fighters) {
    Log.debugf("Performing a fight with fighters: %s", fighters);
		return determineWinner(fighters)
			.chain(this::persistFight);
	}

	Uni<Fight> persistFight(Fight fight) {
    Log.debugf("Persisting a fight: %s", fight);
		return Fight.persist(fight)
      .replaceWith(fight);
	}

	Uni<Fight> determineWinner(Fighters fighters) {
    Log.debugf("Determining winner between fighters: %s", fighters);

		// Amazingly fancy logic to determine the winner...
		return Uni.createFrom().item(() -> {
				Fight fight;

				if (shouldHeroWin(fighters)) {
					fight = heroWonFight(fighters);
				}
				else if (shouldVillainWin(fighters)) {
					fight = villainWonFight(fighters);
				}
				else {
					fight = getRandomWinner(fighters);
				}

				fight.fightDate = Instant.now();

				return fight;
			}
		);
	}

	boolean shouldHeroWin(Fighters fighters) {
		int heroAdjust = this.random.nextInt(this.fightConfig.hero().adjustBound());
		int villainAdjust = this.random.nextInt(this.fightConfig.villain().adjustBound());

		return (fighters.getHero().getLevel() + heroAdjust) > (fighters.getVillain().getLevel() + villainAdjust);
	}

	boolean shouldVillainWin(Fighters fighters) {
		return fighters.getHero().getLevel() < fighters.getVillain().getLevel();
	}

	Fight getRandomWinner(Fighters fighters) {
		return this.random.nextBoolean() ?
		       heroWonFight(fighters) :
		       villainWonFight(fighters);
	}

	Fight heroWonFight(Fighters fighters) {
		Log.infof("Yes, Hero %s won over %s :o)", fighters.getHero().getName(), fighters.getVillain().getName());

		Fight fight = new Fight();
		fight.winnerName = fighters.getHero().getName();
		fight.winnerPicture = fighters.getHero().getPicture();
		fight.winnerLevel = fighters.getHero().getLevel();
		fight.loserName = fighters.getVillain().getName();
		fight.loserPicture = fighters.getVillain().getPicture();
		fight.loserLevel = fighters.getVillain().getLevel();
		fight.winnerTeam = this.fightConfig.hero().teamName();
		fight.loserTeam = this.fightConfig.villain().teamName();

		return fight;
	}

	Fight villainWonFight(Fighters fighters) {
		Log.infof("Gee, Villain %s won over %s :o(", fighters.getVillain().getName(), fighters.getHero().getName());

		Fight fight = new Fight();
		fight.winnerName = fighters.getVillain().getName();
		fight.winnerPicture = fighters.getVillain().getPicture();
		fight.winnerLevel = fighters.getVillain().getLevel();
		fight.loserName = fighters.getHero().getName();
		fight.loserPicture = fighters.getHero().getPicture();
		fight.loserLevel = fighters.getHero().getLevel();
		fight.winnerTeam = this.fightConfig.villain().teamName();
		fight.loserTeam = this.fightConfig.hero().teamName();
		return fight;
	}
}
