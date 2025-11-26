/* ===========================================================
 * MoneySquare ; casa de movimento financeiro direto 
 * Valor positivo: lucro 
 * Valor negativo: impostos 
 * =========================================================== */

package model;

final class MoneySquare extends Square {

    private final int amount; // positivo = ganho, negativo = perda

    MoneySquare(final int index, final String name, final int amount) {
        super(index, name);
        this.amount = amount;
    }

    @Override
    void onLand(final Player player, final GameEngine engine, final EconomyService economy) {
        if (amount == 0) {
            return; 
        }

        if (amount > 0) {
            economy.applyIncome(player, amount);
        } else {
            economy.applyPayment(player, Math.abs(amount));
        }
    }
}
