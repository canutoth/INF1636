package model;

import static org.junit.Assert.*;
import static model.api.dto.PlayerColor.*;

import org.junit.Test;

public class PlayerTest {
	
	private static final int DEFAULT_TIMEOUT = 2000;

	@Test(timeout = DEFAULT_TIMEOUT)
	public void testCreditAndDebit() {
		Player p = new Player("p1", "Alice", RED, 100);
		p.credit(50);
		assertEquals("saldo após crédito", 150, p.getMoney());
		p.debit(40);
		assertEquals("saldo após débito", 110, p.getMoney());
	}

	@Test(timeout = DEFAULT_TIMEOUT)
	public void testJailEnterLeave() {
		Player p = new Player("p1", "Alice", RED, 100);
		assertFalse(p.isInJail());
		p.setInJail(true);
		assertTrue(p.isInJail());
		p.setInJail(false);
		assertFalse(p.isInJail());
	}

	@Test(timeout = DEFAULT_TIMEOUT)
	public void testPropertyManagement() {
		Player p = new Player("p1", "Alice", RED, 100);
		StreetOwnableSquare s = new StreetOwnableSquare(0, "Rua 0", "R0", 200);
		s.setOwner(p);
		p.addProperty(s);
		assertTrue("propriedade deve constar na lista do jogador", p.getProperties().contains(s));
		p.removeProperty(s);
		assertFalse("propriedade não deve permanecer na lista do jogador", p.getProperties().contains(s));
	}

	@Test(timeout = DEFAULT_TIMEOUT)
	public void testBankruptcyFlag() {
		Player p = new Player("p1", "Alice", RED, 0);
		assertTrue("jogador nasce vivo", p.isAlive());
		p.setBankrupt();
		assertTrue("flag de falência deve ser verdadeira", p.isBankrupt());
        assertFalse("jogador falido não está vivo", p.isAlive());

	}
}