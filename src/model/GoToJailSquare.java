/* ===========================================================
 * GoToJailSquare ; envia o jogador imediatamente à prisão.
 * =========================================================== */

package model;

final class GoToJailSquare extends Square {

    GoToJailSquare(final int index, final String name) {
        super(index, name);
    }

    @Override
    void onLand(final Player player, final GameEngine engine, final EconomyService economy) {
        engine.sendToJail(player);
    }
}

