This repo demonstrates issue # 4 from https://github.com/quarkusio/quarkus/issues/31400

This repo contains 2 versions of the superheroes `rest-fights` service that has been stripped down to its bare minimum just to illustrate the mocking/stubbing/spying issue.

There are 2 identical versions of the app:
- [`rest-fights-quarkus2`](rest-fights-quarkus2)
    - Quarkus 2.x version of the app
- [`rest-fights-quarkus3`](rest-fights-quarkus3)
    - Quarkus 3.x version of the app
    
The only difference in the 2 versions is the Quarkus version, as well as the corresponding `javax.` <-> `jakarta.` package name changes.

# Mocking/stubbing/spying not working right

If you run `./mvnw clean verify` in `rest-fights-quarkus2`, everything works correctly. If you run `./mvnw clean verify` in `rest-fights-quarkus3`, then you'll see it does not.

For example, in `rest-fights-quarkus3` if you look at the test failure for `io.quarkus.sample.superheroes.fight.service.FightServiceTests.findRandomFightersNoneFound` it says

```
Wanted but not invoked:
fightService_Subclass.findRandomHero();
-> at io.quarkus.sample.superheroes.fight.service.FightServiceTests.findRandomFightersNoneFound(FightServiceTests.java:218)

However, there was exactly 1 interaction with this mock:
fightService_Subclass.findRandomFighters();
-> at io.quarkus.sample.superheroes.fight.service.FightServiceTests.findRandomFightersNoneFound(FightServiceTests.java:205)
```

Which isn't at all true. If you [look at the code in `rest-fights-quarkus3/io.quarkus.sample.superheroes.fight.service.FightService`](https://github.com/edeandrea/quarkus3-supes-mocking/blob/05e68ddba873c14f91f64fee17874afd772dd2b1/rest-fights-quarkus3/src/main/java/io/quarkus/sample/superheroes/fight/service/FightService.java#L53-L81):

```java
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
```

You can see that `findRandomFighters` definitely calls `findRandomHero` and `findRandomVillain`. You can even see this in the test log output:

```
09:23:35 DEBUG [io.quarkus.sample.superheroes.fight.service.FightService.findRandomFighters(FightService.java:55)] (main) Finding random fighters
09:23:35 DEBUG [io.quarkus.sample.superheroes.fight.service.FightService.findRandomHero(FightService.java:71)] (main) Finding a random hero
09:23:35 DEBUG [io.quarkus.sample.superheroes.fight.service.FightService.lambda$findRandomHero$0(FightService.java:73)] (main) Got random hero: null
09:23:35 DEBUG [io.quarkus.sample.superheroes.fight.service.FightService.findRandomVillain(FightService.java:78)] (main) Finding a random villain
09:23:35 DEBUG [io.quarkus.sample.superheroes.fight.service.FightService.lambda$findRandomVillain$1(FightService.java:80)] (main) Got random villain: null
```

In [the test in `rest-fights-quarkus3/io.quarkus.sample.superheroes.fight.service.FightServiceTests.findRandomFightersNoneFound`](https://github.com/edeandrea/quarkus3-supes-mocking/blob/05e68ddba873c14f91f64fee17874afd772dd2b1/rest-fights-quarkus3/src/test/java/io/quarkus/sample/superheroes/fight/service/FightServiceTests.java#L196-L223):

```java
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
```

you can see that it is verifying that the `findRandomHero` and `findRandomVillain` methods should have been called once, which the logs clearly show they did, but yet the mocking/spying doesn't see that they did.

This is only one example. There are other failures too that are happening because the mocking/spying isn't working the way it should.

Again, [`rest-fights-quarkus2`](rest-fights-quarkus2) and [`rest-fights-quarkus3`](rest-fights-quarkus3) are identical except for the Quarkus 2 <-> 3 changes and ALL the tests in [`rest-fights-quarkus2`](rest-fights-quarkus2) pass.
