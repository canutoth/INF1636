/* ===========================================================
 * OwnableSquare — base para propriedades com dono/preço
 * =========================================================== */

package model;

import java.util.Objects;

abstract class OwnableSquare extends Square {

    private final String id;
    private final int price;
    private Player owner; // null = sem dono

    protected OwnableSquare(final int index,
                            final String name,
                            final String id,
                            final int price) {
        super(index, name);
        this.id = Objects.requireNonNull(id, "id");
        if (price < 0) throw new IllegalArgumentException("price deve ser >= 0");
        this.price = price;
    }

    boolean hasOwner() { return owner != null; }
    Player getOwner() { return owner; }
    int getPrice() { return price; }
    String getId() { return id; }

    /** Define o proprietário. */
    void setOwner(final Player player) {
        this.owner = player;
    }
    
    // Calcula todo o valor investido pelo owner 
    abstract int getTotalInvestment();
    	
    // Devolve propriedade ao banco (remove dono). 
    abstract void removeOwner(final Player target);

    // Cálculo de aluguel específico da concreta. 
    abstract int calcRent(GameEngine engine);

    @Override
    abstract void onLand(Player player, GameEngine engine, EconomyService economy);
}

