/* ===========================================================
 * ChanceSquare ; sorte/revés: saca e aplica uma carta.
 * =========================================================== */

package model;

final class ChanceSquare extends Square {

    ChanceSquare(final int index, final String name) {
        super(index, name);
    }

    @Override
    void onLand(final Player player, final GameEngine engine, final EconomyService economy) {
    	 engine.drawAndUseCard(player);
    }
}
