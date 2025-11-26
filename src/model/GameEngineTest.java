package model;

import static org.junit.Assert.*;
import static model.api.dto.PlayerColor.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class GameEngineTest {

    private static final int BOARD_SIZE = 10;
    private static final int DEFAULT_TIMEOUT = 2000;

    // Square sem efeito para testes
    static class NoopSquare extends Square {
        NoopSquare(int index) { super(index, "S" + index); }
        @Override void onLand(Player player, GameEngine engine, EconomyService economy) { /* no-op */ }
    }

    private Board makeBoard(int size, int jailIndex) {
        List<Square> squares = new ArrayList<>();
        for (int i = 0; i < size; i++) squares.add(new NoopSquare(i));
        return new Board(squares, jailIndex);
    }

    // Novo helper: tabuleiro contendo uma casa Vá para a Prisão
    private Board makeBoardWithGoToJail(int size, int goToJailIndex, int jailIndex) {
        List<Square> squares = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (i == goToJailIndex) squares.add(new GoToJailSquare(i, "Go To Jail"));
            else squares.add(new NoopSquare(i));
        }
        return new Board(squares, jailIndex);
    }

    // Helper: tabuleiro com propriedade própria em 0
    private Board makeBoardWithPropertyAt0(StreetOwnableSquare prop, int size, int jailIndex) {
        List<Square> squares = new ArrayList<>();
        squares.add(prop);
        for (int i = 1; i < size; i++) squares.add(new NoopSquare(i));
        return new Board(squares, jailIndex);
    }

    private List<Player> makePlayers() {
        Player p1 = new Player("p1", "Alice", RED, 1500);
        Player p2 = new Player("p2", "Bob", BLUE, 1500);
        return Arrays.asList(p1, p2);
    }

    private Deck makeDeck() {
        // Deck mínimo não-vazio
        return new Deck(Arrays.asList(new Card(0, Card.CardType.RECEIVE_BANK, 0)));
    }

    private EconomyService makeEconomy() {
        return new EconomyService(new Bank(1_000_000));
    }

    private GameEngine newEngineAt(int startIndex, List<Player> players) {
        return new GameEngine(
            makeBoard(BOARD_SIZE, 3),
            players,
            makeDeck(),
            makeEconomy(),
            startIndex
        );
    }

    private List<Player> players;
    private GameEngine engine;

    @Before
    public void setUp() {
        this.players = makePlayers();
        this.engine = newEngineAt(0, players);
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void moveBy_mustMoveOnlyCurrentPlayer() {
        Player p1 = players.get(0);
        Player p2 = players.get(1);
        assertEquals(0, p1.getPosition());
        assertEquals(0, p2.getPosition());

        engine.moveBy(4);

        assertEquals(4, p1.getPosition());
        assertEquals(0, p2.getPosition());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void moveBy_wrapsAroundBoard() {
        Player p1 = players.get(0);
        p1.moveTo(8); // posição quase no fim

        engine.moveBy(5); // 8 + 5 = 13 -> 13 % 10 = 3

        assertEquals(3, p1.getPosition());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void afterEndTurn_moveByAffectsNextPlayer() {
        Player p1 = players.get(0);
        Player p2 = players.get(1);

        engine.endTurn(); // passa a vez para p2
        engine.moveBy(2);

        assertEquals(0, p1.getPosition());
        assertEquals(2, p2.getPosition());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void rollAndResolve_movesByDiceSumForCurrentPlayer() {
        Player p1 = players.get(0);
        int start = p1.getPosition();

        engine.beginTurn();
        engine.rollAndResolve();

        DiceRoll last = engine.lastRoll();
        int expected = (start + last.getSum()) % BOARD_SIZE;
        assertEquals(expected, p1.getPosition());

        // Garante que o outro jogador não foi movido
        assertEquals(0, players.get(1).getPosition());
    }

    // ==============================
    // Prisão: ida/saída/permanece
    // ==============================

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldGoToJailWhenLandingOnGoToJail() {
        int jailIndex = 3;
        int goToJailIndex = 5;
        List<Player> ps = makePlayers();
        Board board = makeBoardWithGoToJail(BOARD_SIZE, goToJailIndex, jailIndex);
        GameEngine localEngine = new GameEngine(board, ps, makeDeck(), makeEconomy(), 0);

        Player p1 = ps.get(0);
        p1.moveTo(goToJailIndex);
        localEngine.onLand();

        assertTrue(p1.isInJail());
        assertEquals(jailIndex, p1.getPosition());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldLeaveJailWithDouble() {
        Player p1 = players.get(0);
        engine.sendToJail(p1);
        assertTrue(p1.isInJail());
        engine.applyJailRules(new DiceRoll(3, 3));
        assertFalse(p1.isInJail());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldLeaveJailWithCard() {
        Player p1 = players.get(0);
        engine.sendToJail(p1);
        assertTrue(p1.isInJail());
        p1.grantGetOutOfJailCard();
        engine.applyJailRules(new DiceRoll(1, 2));
        assertFalse(p1.isInJail());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldRemainInJailWithoutDoubleOrCard() {
        Player p1 = players.get(0);
        engine.sendToJail(p1);
        assertTrue(p1.isInJail());
        engine.applyJailRules(new DiceRoll(1, 2));
        assertTrue(p1.isInJail());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldSkipBankruptPlayerInTurnOrder() {
        StreetOwnableSquare rentProp = new StreetOwnableSquare(0, "Rua 0", "R0", 200);

        Player poor = new Player("pPoor", "Carol", GRAY, 0);
        Player owner = new Player("pOwner", "Dave", YELLOW, 500);
        Player third = new Player("pThird", "Eve", PURPLE, 500);

        rentProp.setOwner(owner);
        owner.addProperty(rentProp);
        // construir 4 casas e depois hotel
        for (int i = 0; i < 4; i++) rentProp.buildHouse();
        rentProp.buildHotel();

        Board board = makeBoardWithPropertyAt0(rentProp, BOARD_SIZE, 3);
        List<Player> ps = Arrays.asList(poor, owner, third);
        GameEngine localEngine = new GameEngine(board, ps, new Deck(Arrays.asList(new Card(0, Card.CardType.RECEIVE_BANK, 0))), new EconomyService(new Bank(1_000_000)), 0);

        localEngine.onLand();
        assertTrue(poor.isBankrupt());
        assertEquals(500, owner.getMoney());

        int idx1 = localEngine.endTurn();
        assertSame(owner, localEngine.currentPlayer());

        int idx2 = localEngine.endTurn();
        assertSame(third, localEngine.currentPlayer());

        int idx3 = localEngine.endTurn();
        assertSame(owner, localEngine.currentPlayer());

        assertTrue(idx1 >= 0 && idx2 >= 0 && idx3 >= 0);
    }
}