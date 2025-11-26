package model;

import static org.junit.Assert.*;
import static model.api.dto.PlayerColor.*;

import org.junit.Test;

public class BankTest {
	
	private static final int DEFAULT_TIMEOUT = 2000;

	@Test(timeout = DEFAULT_TIMEOUT)
    public void transfer_PlayerToBank_debitsPlayer() {
        Bank bank = new Bank(1_000);
        Player payer = new Player("p1", "Alice", RED, 200);

        bank.transfer(payer, null, 150);

        assertEquals("saldo do pagador após transferir ao banco",
                50, payer.getMoney());
    }

	@Test(timeout = DEFAULT_TIMEOUT)
    public void transfer_BankToPlayer_creditsPlayer() {
        Bank bank = new Bank(1_000);
        Player payee = new Player("p1", "Alice", RED, 200);

        bank.transfer(null, payee, 150);

        assertEquals("saldo do recebedor após crédito do banco",
                350, payee.getMoney());
    }

	@Test(timeout = DEFAULT_TIMEOUT)
    public void transfer_PlayerToPlayer_movesMoneyBetweenPlayers() {
        Bank bank = new Bank(1_000);
        Player payer = new Player("a", "Alice", RED, 500);
        Player payee = new Player("b", "Bob", BLUE, 100);

        bank.transfer(payer, payee, 200);
        
        assertEquals("saldo do pagador após transferência a outro jogador",
                300, payer.getMoney());
        assertEquals("saldo do recebedor após transferência de outro jogador",
                300, payee.getMoney());
    }

    @Test(timeout = DEFAULT_TIMEOUT, expected = IllegalArgumentException.class)
    public void transfer_InvalidBothNull_throws() {
        Bank bank = new Bank(1_000);
        bank.transfer(null, null, 10);
    }

    @Test(timeout = DEFAULT_TIMEOUT, expected = IllegalArgumentException.class)
    public void transfer_NegativeAmount_throws() {
        Bank bank = new Bank(1_000);
        Player p = new Player("p1", "Alice", RED, 100);
        bank.transfer(p, null, -1);
    }
}