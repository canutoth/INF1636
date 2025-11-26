/* ===========================================================
 * Player ; estado de cada jogador (Model).
 * Mantém saldo, posição, prisão, propriedades e status de vida.
 * =========================================================== */

package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import model.api.dto.PlayerColor;

final class Player {

    // --- Identidade/visual ---
    private final String id;
    private final String name;
    private final PlayerColor color;

    // --- Estado econômico/posicional ---
    private int money;
    private int position;

    // --- Prisão/cartas ---
    private boolean inJail;
    private int getOutOfJailCards;

    // --- Patrimônio e status ---
    private final List<OwnableSquare> properties;
    private boolean alive;

    Player(final String id, final String name, final PlayerColor color, final int initialMoney) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.color = Objects.requireNonNull(color, "color");
        if (initialMoney < 0) throw new IllegalArgumentException("Dinheiro inicial inválido.");
        this.money = initialMoney;
        this.position = 0;
        this.inJail = false;
        this.getOutOfJailCards = 0;
        this.properties = new ArrayList<>();
        this.alive = true;
    }

    // ===== Operações financeiras =====

    /** Credita valor ao jogador. */
    void credit(final int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount deve ser >= 0");
        this.money += amount;
    }

    /** Debita valor do jogador (saldo nunca fica negativo - EconomyService garante liquidez). */
    void debit(final int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount deve ser >= 0");
        if (amount > this.money) {
            throw new IllegalStateException("Saldo insuficiente para débito: " + amount + " > " + money);
        }
        this.money -= amount;
    }

    // ===== Movimento/posição =====

    /** Move peão para índice do tabuleiro. */
    void moveTo(final int index) {
        if (index < 0) throw new IllegalArgumentException("index deve ser >= 0");
        this.position = index;
    }

    // ===== Prisão/cartas =====

    /** Define estado de prisão. */
    void setInJail(final boolean flag) { this.inJail = flag; }

    /** Está preso? */
    boolean isInJail() { return inJail; }

    /** Consome 1 cartão "saída livre", se houver. */
    boolean consumeGetOutOfJailCard() {
        if (getOutOfJailCards > 0) {
            getOutOfJailCards--;
            return true;
        }
        return false;
    }

    /** Concede 1 cartão "saída livre". */
    void grantGetOutOfJailCard() { this.getOutOfJailCards++; }
    
    int getGetOutOfJailCards() { return getOutOfJailCards; }

    // ===== Propriedades =====

    /** Adiciona propriedade ao patrimônio. */
    void addProperty(final OwnableSquare p) {
        if (!properties.contains(p)) properties.add(p);
    }

    /** Remove propriedade do patrimônio. */
    void removeProperty(final OwnableSquare p) {
        properties.remove(p);
    }

    // ===== Status de vida/bankruptcy =====

    /** Está falido? (equivale a não estar vivo no jogo) */
    boolean isBankrupt() { return !alive; }

    /** Marca jogador como falido (fora do jogo). */
    void setBankrupt() { this.alive = false; this.money = 0;}

    /** Está ativo no jogo? */
    boolean isAlive() { return alive; }

    /**
     * Verifica se o jogador possui dinheiro suficiente para pagar um valor.
     */
    boolean canAfford(final int amount) {
        if (amount < 0)
            throw new IllegalArgumentException("amount deve ser >= 0");
        return this.money >= amount;
    }

    /**
     * Retorna quanto falta para o jogador conseguir pagar o valor solicitado.
     */
    int howMuchMissing(final int amount) {
        if (amount < 0)
            throw new IllegalArgumentException("amount deve ser >= 0");
        return (this.money >= amount) ? 0 : (amount - this.money);
    }
    
    /** Dinheiro atual. */
    int getMoney() { return money; }

    /** Posição atual no tabuleiro. */
    int getPosition() { return position; }

    /** Id do jogador. */
    String getId() { return id; }

    /** Nome do jogador. */
    String getName() { return name; }

    /** Cor do jogador. */
    PlayerColor getColor() { return color; }

    /** Lista imutável das propriedades. */
    List<OwnableSquare> getProperties() { return Collections.unmodifiableList(properties); }
    
    /* Retorna os índices das propriedades pertencentes ao jogador. */
    int[] getPropertiesIndex() {
    	 final int size = properties.size();
    	    final int[] indices = new int[size];
    	    for (int i = 0; i < size; i++) {
    	        indices[i] = properties.get(i).index();
    	    }
    	    return indices;
    }


    @Override public String toString() {
        return "Player{id='%s', name='%s', money=%d, pos=%d, alive=%s}".formatted(id, name, money, position, alive);
    }
}
