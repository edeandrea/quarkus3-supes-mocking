package io.quarkus.sample.superheroes.fight.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.InternalServerErrorException;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.config.FightConfig;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

/**
 * Tests for the service layer ({@link FightService}).
 */
@QuarkusTest
class FightServiceTests {
	private static final ObjectId DEFAULT_FIGHT_ID = new ObjectId();
	private static final Instant DEFAULT_FIGHT_DATE = Instant.now();

	private static final String DEFAULT_HERO_NAME = "Super Baguette";
	private static final String DEFAULT_HERO_PICTURE = "super_baguette.png";
	private static final String DEFAULT_HERO_POWERS = "eats baguette really quickly";
	private static final int DEFAULT_HERO_LEVEL = 42;
	private static final String HEROES_TEAM_NAME = "heroes";

	private static final String DEFAULT_VILLAIN_NAME = "Super Chocolatine";
	private static final String DEFAULT_VILLAIN_PICTURE = "super_chocolatine.png";
	private static final String DEFAULT_VILLAIN_POWERS = "does not eat pain au chocolat";
	private static final int DEFAULT_VILLAIN_LEVEL = 42;
	private static final String VILLAINS_TEAM_NAME = "villains";

	@InjectSpy
	FightService fightService;

	@Inject
	FightConfig fightConfig;

	@InjectMock
	HeroClient heroClient;

	@InjectMock
	VillainClient villainClient;

	@Test
	public void findAllFightsNoneFound() {
		PanacheMock.mock(Fight.class);
		when(Fight.listAll())
			.thenReturn(Uni.createFrom().item(List.of()));

		var allFights = this.fightService.findAllFights()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(allFights)
			.isNotNull()
			.isEmpty();

		PanacheMock.verify(Fight.class).listAll();
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void findAllFights() {
		PanacheMock.mock(Fight.class);
		when(Fight.listAll())
			.thenReturn(Uni.createFrom().item(List.of(createFightHeroWon())));

		var allFights = this.fightService.findAllFights()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(allFights)
			.isNotNull()
			.isNotEmpty()
			.hasSize(1)
			.extracting(
				"id",
				"fightDate",
				"winnerName",
				"winnerLevel",
				"winnerPicture",
				"loserName",
				"loserLevel",
				"loserPicture",
				"winnerTeam",
				"loserTeam"
			)
			.containsExactly(
				tuple(
					DEFAULT_FIGHT_ID,
					DEFAULT_FIGHT_DATE,
					DEFAULT_HERO_NAME,
					DEFAULT_HERO_LEVEL,
					DEFAULT_HERO_PICTURE,
					DEFAULT_VILLAIN_NAME,
					DEFAULT_VILLAIN_LEVEL,
					DEFAULT_VILLAIN_PICTURE,
					HEROES_TEAM_NAME,
					VILLAINS_TEAM_NAME
				)
			);

		PanacheMock.verify(Fight.class).listAll();
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void findFightByIdFound() {
		PanacheMock.mock(Fight.class);
		when(Fight.findById(eq(DEFAULT_FIGHT_ID)))
			.thenReturn(Uni.createFrom().item(createFightHeroWon()));

		var fight = this.fightService.findFightById(DEFAULT_FIGHT_ID.toString())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
			.extracting(
				"id",
				"fightDate",
				"winnerName",
				"winnerLevel",
				"winnerPicture",
				"loserName",
				"loserLevel",
				"loserPicture",
				"winnerTeam",
				"loserTeam"
			)
			.containsExactly(
				DEFAULT_FIGHT_ID,
				DEFAULT_FIGHT_DATE,
				DEFAULT_HERO_NAME,
				DEFAULT_HERO_LEVEL,
				DEFAULT_HERO_PICTURE,
				DEFAULT_VILLAIN_NAME,
				DEFAULT_VILLAIN_LEVEL,
				DEFAULT_VILLAIN_PICTURE,
				HEROES_TEAM_NAME,
				VILLAINS_TEAM_NAME
			);

		PanacheMock.verify(Fight.class).findById(eq(DEFAULT_FIGHT_ID));
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void findFightByIdNotFound() {
		PanacheMock.mock(Fight.class);
		when(Fight.findById(eq(DEFAULT_FIGHT_ID)))
			.thenReturn(Uni.createFrom().nullItem());

		var fight = this.fightService.findFightById(DEFAULT_FIGHT_ID.toString())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNull();

		PanacheMock.verify(Fight.class).findById(eq(DEFAULT_FIGHT_ID));
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void findRandomFightersNoneFound() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(Uni.createFrom().nullItem());

		when(this.villainClient.findRandomVillain())
			.thenReturn(Uni.createFrom().nullItem());

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(new Fighters(createFallbackHero(), createFallbackVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomFightersHeroNotFound() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(Uni.createFrom().nullItem());

		when(this.villainClient.findRandomVillain())
			.thenReturn(Uni.createFrom().item(createDefaultVillain()));

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(new Fighters(createFallbackHero(), createDefaultVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomFightersVillainNotFound() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(Uni.createFrom().item(createDefaultHero()));

		when(this.villainClient.findRandomVillain())
			.thenReturn(Uni.createFrom().nullItem());

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(new Fighters(createDefaultHero(), createFallbackVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomFightersHeroError() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

		when(this.villainClient.findRandomVillain())
			.thenReturn(Uni.createFrom().item(createDefaultVillain()));

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(new Fighters(createFallbackHero(), createDefaultVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService).fallbackRandomHero();
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomFightersVillainError() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(Uni.createFrom().item(createDefaultHero()));

		when(this.villainClient.findRandomVillain())
			.thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(new Fighters(createDefaultHero(), createFallbackVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomFightersHeroVillainError() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

		when(this.villainClient.findRandomVillain())
			.thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(new Fighters(createFallbackHero(), createFallbackVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService).fallbackRandomHero();
		verify(this.fightService).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void findRandomFighters() {
		PanacheMock.mock(Fight.class);
		when(this.heroClient.findRandomHero())
			.thenReturn(Uni.createFrom().item(createDefaultHero()));

		when(this.villainClient.findRandomVillain())
			.thenReturn(Uni.createFrom().item(createDefaultVillain()));

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(new Fighters(createDefaultHero(), createDefaultVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void performFightNullFighters() {
		PanacheMock.mock(Fight.class);

		var cve = catchThrowableOfType(
			() -> this.fightService.performFight(null),
			ConstraintViolationException.class);

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				ConstraintViolation::getInvalidValue,
				ConstraintViolation::getMessage
			)
			.containsExactly(
				null,
				"must not be null"
			);

		verify(this.fightService, never()).determineWinner(any(Fighters.class));
		verify(this.fightService, never()).shouldHeroWin(any(Fighters.class));
		verify(this.fightService, never()).shouldVillainWin(any(Fighters.class));
		verify(this.fightService, never()).getRandomWinner(any(Fighters.class));
		verify(this.fightService, never()).heroWonFight(any(Fighters.class));
		verify(this.fightService, never()).villainWonFight(any(Fighters.class));
		verify(this.fightService, never()).persistFight(any(Fight.class));
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void performFightInvalidFighters() {
		PanacheMock.mock(Fight.class);

		var cve = catchThrowableOfType(
			() -> this.fightService.performFight(new Fighters(null, createDefaultVillain())),
			ConstraintViolationException.class);

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				ConstraintViolation::getInvalidValue,
				ConstraintViolation::getMessage
			)
			.containsExactly(
				null,
				"must not be null"
			);

		verify(this.fightService, never()).determineWinner(any(Fighters.class));
		verify(this.fightService, never()).shouldHeroWin(any(Fighters.class));
		verify(this.fightService, never()).shouldVillainWin(any(Fighters.class));
		verify(this.fightService, never()).getRandomWinner(any(Fighters.class));
		verify(this.fightService, never()).heroWonFight(any(Fighters.class));
		verify(this.fightService, never()).villainWonFight(any(Fighters.class));
		verify(this.fightService, never()).persistFight(any(Fight.class));
		PanacheMock.verifyNoInteractions(Fight.class);
	}

	@Test
	public void performFightHeroShouldWin() {
    var fightOutcome = createFightHeroWon();
		var fightMatcher = fightMatcher(fightOutcome);
		var fightersMatcher = fightersMatcher(createDefaultFighters());

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(argThat(fightMatcher), any());

		doReturn(true)
      .when(this.fightService)
      .shouldHeroWin(argThat(fightersMatcher));

		doReturn(fightOutcome)
      .when(this.fightService)
      .heroWonFight(argThat(fightersMatcher));

		var fight = this.fightService.performFight(createDefaultFighters())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
      .usingRecursiveComparison()
			.isEqualTo(fightOutcome);

		verify(this.fightService).determineWinner(argThat(fightersMatcher));
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldHeroWin(argThat(fightersMatcher));
		verify(this.fightService).heroWonFight(argThat(fightersMatcher));
		verify(this.fightService, never()).shouldVillainWin(any(Fighters.class));
		verify(this.fightService, never()).villainWonFight(any(Fighters.class));
		verify(this.fightService, never()).getRandomWinner(any(Fighters.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void performFightVillainShouldWin() {
    var fightOutcome = createFightVillainWon();
    var fightMatcher = fightMatcher(fightOutcome);
		var fightersMatcher = fightersMatcher(createDefaultFighters());

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(argThat(fightMatcher), any());

		doReturn(false).when(this.fightService).shouldHeroWin(argThat(fightersMatcher));
		doReturn(true).when(this.fightService).shouldVillainWin(argThat(fightersMatcher));
		doReturn(fightOutcome).when(this.fightService).villainWonFight(argThat(fightersMatcher));

		var fight = this.fightService.performFight(createDefaultFighters())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(fightOutcome);

		verify(this.fightService).determineWinner(argThat(fightersMatcher));
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldVillainWin(argThat(fightersMatcher));
		verify(this.fightService).shouldHeroWin(argThat(fightersMatcher));
		verify(this.fightService).villainWonFight(argThat(fightersMatcher));
		verify(this.fightService, never()).heroWonFight(any(Fighters.class));
		verify(this.fightService, never()).getRandomWinner(any(Fighters.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void performFightRandomWinner() {
    var fightOutcome = createFightVillainWon();
    var fightMatcher = fightMatcher(fightOutcome);
		var fightersMatcher = fightersMatcher(createDefaultFighters());

		PanacheMock.mock(Fight.class);
		PanacheMock.doReturn(Uni.createFrom().voidItem())
			.when(Fight.class)
			.persist(argThat(fightMatcher), any());

		doReturn(false).when(this.fightService).shouldHeroWin(argThat(fightersMatcher));
		doReturn(false).when(this.fightService).shouldVillainWin(argThat(fightersMatcher));
		doReturn(fightOutcome).when(this.fightService).getRandomWinner(argThat(fightersMatcher));

		var fight = this.fightService.performFight(createDefaultFighters())
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fight)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(fightOutcome);

		verify(this.fightService).determineWinner(argThat(fightersMatcher));
		verify(this.fightService).persistFight(argThat(fightMatcher));
		verify(this.fightService).shouldVillainWin(argThat(fightersMatcher));
		verify(this.fightService).shouldHeroWin(argThat(fightersMatcher));
		verify(this.fightService).getRandomWinner(argThat(fightersMatcher));
		verify(this.fightService, never()).villainWonFight(any(Fighters.class));
		verify(this.fightService, never()).heroWonFight(any(Fighters.class));
		PanacheMock.verify(Fight.class).persist(argThat(fightMatcher), any());
		PanacheMock.verifyNoMoreInteractions(Fight.class);
	}

	@Test
	public void didHeroWinTrue() {
		var fighters = createDefaultFighters();
		fighters.getHero().setLevel(Integer.MAX_VALUE - this.fightConfig.hero().adjustBound());
		fighters.getVillain().setLevel(Integer.MIN_VALUE);

		assertThat(this.fightService.shouldHeroWin(fighters))
			.isTrue();
	}

	@Test
	public void didHeroWinFalse() {
		var fighters = createDefaultFighters();
		fighters.getHero().setLevel(Integer.MIN_VALUE);
		fighters.getVillain().setLevel(Integer.MAX_VALUE- this.fightConfig.hero().adjustBound());

		assertThat(this.fightService.shouldHeroWin(fighters))
			.isFalse();
	}

	@Test
	public void didVillainWinTrue() {
		var fighters = createDefaultFighters();
		fighters.getHero().setLevel(Integer.MIN_VALUE);
		fighters.getVillain().setLevel(Integer.MAX_VALUE);

		assertThat(this.fightService.shouldVillainWin(fighters))
			.isTrue();
	}

	@Test
	public void didVillainWinFalse() {
		var fighters = createDefaultFighters();
		fighters.getHero().setLevel(Integer.MAX_VALUE);
		fighters.getVillain().setLevel(Integer.MIN_VALUE);

		assertThat(this.fightService.shouldVillainWin(fighters))
			.isFalse();
	}

  @Test
  public void helloHeroesSuccess() {
    when(this.heroClient.helloHeroes()).thenReturn(Uni.createFrom().item("hello"));

    var message = this.fightService.helloHeroes()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("hello");

    verify(this.heroClient).helloHeroes();
    verify(this.fightService).helloHeroes();
    verifyNoMoreInteractions(this.heroClient);
    verifyNoInteractions(this.villainClient);
  }

  @Test
  public void helloHeroesFallback() {
    when(this.fightService.fallbackHelloHeroes())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.heroClient.helloHeroes())
	    .thenReturn(
				Uni.createFrom().item("hello")
					.onItem()
					.delayIt().by(Duration.ofSeconds(6))
	    );

    var message = this.fightService.helloHeroes()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

    verify(this.heroClient).helloHeroes();
    verify(this.fightService).helloHeroes();
    verify(this.fightService).fallbackHelloHeroes();
    verifyNoMoreInteractions(this.heroClient);
    verifyNoInteractions(this.villainClient);
  }

  @Test
  public void helloHeroesFailure() {
    when(this.fightService.fallbackHelloHeroes())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.heroClient.helloHeroes())
	    .thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

    var message = this.fightService.helloHeroes()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

    verify(this.heroClient).helloHeroes();
    verify(this.fightService).helloHeroes();
    verify(this.fightService).fallbackHelloHeroes();
    verifyNoMoreInteractions(this.heroClient);
    verifyNoInteractions(this.villainClient);
  }

  @Test
  public void helloVillainsSuccess() {
    when(this.villainClient.helloVillains())
	    .thenReturn(Uni.createFrom().item("hello"));

    var message = this.fightService.helloVillains()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("hello");

    verify(this.villainClient).helloVillains();
    verify(this.fightService).helloVillains();
    verifyNoMoreInteractions(this.villainClient);
    verifyNoInteractions(this.heroClient);
  }

  @Test
  public void helloVillainsFallback() {
    when(this.fightService.fallbackHelloVillains())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.villainClient.helloVillains())
	    .thenReturn(
				Uni.createFrom().item("hello")
					.onItem()
					.delayIt().by(Duration.ofSeconds(6))
	    );

    var message = this.fightService.helloVillains()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

    verify(this.villainClient).helloVillains();
    verify(this.fightService).helloVillains();
    verify(this.fightService).fallbackHelloVillains();
    verifyNoMoreInteractions(this.villainClient);
    verifyNoInteractions(this.heroClient);
  }

  @Test
  public void helloVillainsFailure() {
    when(this.fightService.fallbackHelloVillains())
	    .thenReturn(Uni.createFrom().item("fallback"));

    when(this.villainClient.helloVillains())
	    .thenReturn(Uni.createFrom().failure(InternalServerErrorException::new));

    var message = this.fightService.helloVillains()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo("fallback");

    verify(this.villainClient).helloVillains();
    verify(this.fightService).helloVillains();
    verify(this.fightService).fallbackHelloVillains();
    verifyNoMoreInteractions(this.villainClient);
    verifyNoInteractions(this.heroClient);
  }

  private Fighters createFallbackFighters() {
    return new Fighters(createFallbackHero(), createFallbackVillain());
  }

	private static Hero createDefaultHero() {
		return new Hero(
			DEFAULT_HERO_NAME,
			DEFAULT_HERO_LEVEL,
			DEFAULT_HERO_PICTURE,
			DEFAULT_HERO_POWERS
		);
	}

	private Hero createFallbackHero() {
		return new Hero(
			this.fightConfig.hero().fallback().name(),
			this.fightConfig.hero().fallback().level(),
			this.fightConfig.hero().fallback().picture(),
			this.fightConfig.hero().fallback().powers()
		);
	}

	private Villain createFallbackVillain() {
		return new Villain(
			this.fightConfig.villain().fallback().name(),
			this.fightConfig.villain().fallback().level(),
			this.fightConfig.villain().fallback().picture(),
			this.fightConfig.villain().fallback().powers()
		);
	}

	private static Villain createDefaultVillain() {
		return new Villain(
			DEFAULT_VILLAIN_NAME,
			DEFAULT_VILLAIN_LEVEL,
			DEFAULT_VILLAIN_PICTURE,
			DEFAULT_VILLAIN_POWERS
		);
	}

	private static Fighters createDefaultFighters() {
		return new Fighters(createDefaultHero(), createDefaultVillain());
	}

	private static Fight createFightHeroWon() {
		var fight = new Fight();
		fight.id = DEFAULT_FIGHT_ID;
		fight.fightDate = DEFAULT_FIGHT_DATE;
		fight.winnerName = DEFAULT_HERO_NAME;
		fight.winnerLevel = DEFAULT_HERO_LEVEL;
		fight.winnerPicture = DEFAULT_HERO_PICTURE;
		fight.loserName = DEFAULT_VILLAIN_NAME;
		fight.loserLevel = DEFAULT_VILLAIN_LEVEL;
		fight.loserPicture = DEFAULT_VILLAIN_PICTURE;
		fight.winnerTeam = HEROES_TEAM_NAME;
		fight.loserTeam = VILLAINS_TEAM_NAME;

		return fight;
	}

	private static Fight createFightVillainWon() {
		var fight = new Fight();
		fight.id = DEFAULT_FIGHT_ID;
		fight.fightDate = DEFAULT_FIGHT_DATE;
		fight.winnerName = DEFAULT_VILLAIN_NAME;
		fight.winnerLevel = DEFAULT_VILLAIN_LEVEL;
		fight.winnerPicture = DEFAULT_VILLAIN_PICTURE;
		fight.winnerTeam = VILLAINS_TEAM_NAME;
		fight.loserName = DEFAULT_HERO_NAME;
		fight.loserLevel = DEFAULT_HERO_LEVEL;
		fight.loserPicture = DEFAULT_HERO_PICTURE;
		fight.loserTeam = HEROES_TEAM_NAME;

		return fight;
	}

	private static ArgumentMatcher<Hero> heroMatcher(Hero hero) {
		return h -> (hero == h) || (
			(hero != null) &&
				(h != null) &&
				Objects.equals(hero.getName(), h.getName()) &&
				Objects.equals(hero.getLevel(), h.getLevel()) &&
				Objects.equals(hero.getPicture(), h.getPicture()) &&
				Objects.equals(hero.getPowers(), h.getPowers())
		);
	}

	private static ArgumentMatcher<Villain> villainMatcher(Villain villain) {
		return v -> (villain == v) || (
			(villain != null) &&
				(v != null) &&
				Objects.equals(villain.getName(), v.getName()) &&
				Objects.equals(villain.getLevel(), v.getLevel()) &&
				Objects.equals(villain.getPicture(), v.getPicture()) &&
				Objects.equals(villain.getPowers(), v.getPowers())
		);
	}

	private static ArgumentMatcher<Fighters> fightersMatcher(Fighters fighters) {
		return f -> (fighters == f) || (
			(fighters != null) &&
				(f != null) &&
				heroMatcher(f.getHero()).matches(fighters.getHero()) &&
				villainMatcher(f.getVillain()).matches(fighters.getVillain())
		);
	}

	private static ArgumentMatcher<Fight> fightMatcher(Fight fight) {
		return f -> (fight == f) || (
			(fight != null) &&
				(f != null) &&
				(Objects.equals(fight.fightDate, f.fightDate) || f.fightDate.isAfter(fight.fightDate)) &&
				Objects.equals(fight.id, f.id) &&
				Objects.equals(fight.loserLevel, f.loserLevel) &&
				Objects.equals(fight.loserName, f.loserName) &&
				Objects.equals(fight.loserPicture, f.loserPicture) &&
				Objects.equals(fight.loserTeam, f.loserTeam) &&
				Objects.equals(fight.winnerLevel, f.winnerLevel) &&
				Objects.equals(fight.winnerName, f.winnerName) &&
				Objects.equals(fight.winnerPicture, f.winnerPicture) &&
				Objects.equals(fight.winnerTeam, f.winnerTeam)
		);
	}
}
