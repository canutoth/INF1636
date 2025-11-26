/* ===========================================================
 * DummySquare ; uma casa que não faz nada.
 * Todas as casas do tipo DummySquare são apenas para visitação e não tem regra de negócio explícita.
 * =========================================================== */

package model;

final class DummySquare extends Square {
    DummySquare(final int index, final String name) {
        super(index, name);
    }
    
    @Override
    void onLand(Player player, GameEngine engine, EconomyService economy) {
        // Nenhum efeito 
    }

}