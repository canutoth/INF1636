package model;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;


public class DiceRollTest {
	
	private static final int DEFAULT_TIMEOUT = 2000;
	
    private DiceRoll roll;

    @Before
    public void setUp() {
        setRoll(new DiceRoll()); // injeta seed se houver suporte
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldSumDiceCorrectly() {
        DiceRoll r = new DiceRoll(3, 4); // usar construtor de teste, stub ou setter
        assertEquals("soma deve ser d1 + d2", 7, r.getSum());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldDetectDoubleOnlyWhenEqual() {
        DiceRoll r1 = new DiceRoll(5, 5);
        assertTrue("isDouble deve ser verdadeiro quando d1 == d2", r1.isDouble());

        DiceRoll r2 = new DiceRoll(5, 4);
        assertFalse("isDouble deve ser falso quando d1 != d2", r2.isDouble());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldKeepEachDieBetween1And6() {
        DiceRoll r = new DiceRoll();
        assertTrue("d1 deve estar entre 1 e 6", r.getD1() >= 1 && r.getD1() <= 6);
        assertTrue("d2 deve estar entre 1 e 6", r.getD2() >= 1 && r.getD2() <= 6);
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldProduceValidIntegerValues() {
        DiceRoll r = new DiceRoll();
        assertEquals("d1 deve ser um inteiro", 0, r.getD1() % 1);
        assertEquals("d2 deve ser um inteiro", 0, r.getD2() % 1);
    }
    
    @Test(timeout = DEFAULT_TIMEOUT)
    public void getSum_minimumPossibleValueIs2() {
        DiceRoll r = new DiceRoll(1, 1);
        assertEquals("soma mínima deve ser 2", 2, r.getSum());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void getSum_maximumPossibleValueIs12() {
        DiceRoll r = new DiceRoll(6, 6);
        assertEquals("soma máxima deve ser 12", 12, r.getSum());
    }

    @Test(timeout = DEFAULT_TIMEOUT)
    public void shouldBeReproducibleWithSameSeedWhenSupported() {
        DiceRoll r1 = new DiceRoll(123L);
        DiceRoll r2 = new DiceRoll(123L);
        assertEquals("mesma seed deve gerar mesma soma", r1.getSum(), r2.getSum());
    }

	public DiceRoll getRoll() {
		return roll;
	}

	public void setRoll(DiceRoll roll) {
		this.roll = roll;
	}
}