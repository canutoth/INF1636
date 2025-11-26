/* ===========================================================
 * CompanyOwnableSquare — companhias (não constroem); aluguel = soma dos dados × multiplicador
 * =========================================================== */

package model;

final class CompanyOwnableSquare extends OwnableSquare {

    private final int multiplier;

    CompanyOwnableSquare(final int index,
                         final String name,
                         final String id,
                         final int price,
                         final int multiplier) {
        super(index, name, id, price);
        if (multiplier <= 0)
            throw new IllegalArgumentException("multiplicador deve ser positivo");
        this.multiplier = multiplier;
    }

    // Mostra o multiplicador usado para calcular o aluguel.
    int getMultiplier() { return multiplier; }
    
    // Retorna o valor total investido pelo jogador (apenas preço da companhia). 
    int getTotalInvestment() {
        return getPrice();
    }
    
    // Remove o proprietário, se for o atual. 
    void removeOwner(final Player player) {
        if (this.getOwner() != null && this.getOwner().equals(player)) {
            setOwner(null);
        }
    }
    
    @Override
    int calcRent(final GameEngine engine) {
        final int lastSum = (engine != null && engine.lastRoll() != null)
                ? engine.lastRoll().getSum()
                : 0;
        return multiplier * lastSum;
    }

    @Override
    void onLand(final Player player, final GameEngine engine, final EconomyService economy) {
        if (!hasOwner() || getOwner() == player) {
            return; // Não cobrar aluguel
        }

        final int rent = calcRent(engine);
        economy.chargeRent(player, getOwner(), rent);
    }
}
