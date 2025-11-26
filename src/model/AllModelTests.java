package model;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    DiceRollTest.class,
    BankTest.class,
    PlayerTest.class,
    StreetOwnableSquareTest.class,
    EconomyServiceTest.class,
    GameEngineTest.class
})
public class AllModelTests { }
