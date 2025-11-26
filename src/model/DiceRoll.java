/* ===========================================================
 * DiceRoll — representa uma jogada de dois dados (1..6).
 * =========================================================== */

package model;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


final class DiceRoll {

    private final int d1;
    private final int d2;
    private final int sum;
    private final boolean isDouble;

    // Construtor padrão: usa fonte de aleatoriedade do sistema
    public DiceRoll() {
        int rd1 = ThreadLocalRandom.current().nextInt(1, 7);
        int rd2 = ThreadLocalRandom.current().nextInt(1, 7);
        this.d1 = rd1;
        this.d2 = rd2;
        this.sum = d1 + d2;
        this.isDouble = (d1 == d2);
    }

    // Construtor determinístico para testes: usa uma seed específica
    public DiceRoll(long seed) {
        Random r = new Random(seed);
        int rd1 = r.nextInt(6) + 1; // 1..6
        int rd2 = r.nextInt(6) + 1; // 1..6
        this.d1 = rd1;
        this.d2 = rd2;
        this.sum = d1 + d2;
        this.isDouble = (d1 == d2);
    }

    // Construtor para injetar valores específicos (ex.: testes)
    public DiceRoll(int d1, int d2) {
        // validação de intervalo 1..6
        if (d1 < 1 || d1 > 6 || d2 < 1 || d2 > 6) {
            throw new IllegalArgumentException("Dice values must be between 1 and 6");
        }
        this.d1 = d1;
        this.d2 = d2;
        this.sum = d1 + d2;
        this.isDouble = (d1 == d2);
    }

    // Getters compatíveis com o teste
    public int getD1() { return d1; }
    public int getD2() { return d2; }
    public int getSum() { return sum; }
    public boolean isDouble() { return isDouble; }

    @Override
    public String toString() {
        return "DiceRoll{d1=" + d1 + ", d2=" + d2 + ", sum=" + sum + ", double=" + isDouble + "}";
    }
}