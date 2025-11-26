/* ===========================================================
 * Square ; base abstrata das casas do tabuleiro.
 * O que acontece ao cair aqui?
 * =========================================================== */

package model;

import java.util.Objects;

abstract class Square {

    private final int index;
    private final String name;

    protected Square(final int index, final String name) {
        if (index < 0) throw new IllegalArgumentException("index deve ser >= 0");
        this.index = index;
        this.name = Objects.requireNonNull(name, "name");
    }

    /* Efeito ao cair nesta casa. */
    abstract void onLand(Player player, GameEngine engine, EconomyService economy);

    int index() { return index; }
    String name() { return name; }
    
    /**
     * Retorna um identificador simples do tipo desta Square (classe concreta).
     * Ex.: "ChanceSquare", "GoToJailSquare", "JailSquare", "MoneySquare", "StreetOwnableSquare", "StartSquare"
     */
    String type() { return this.getClass().getSimpleName(); }
}

