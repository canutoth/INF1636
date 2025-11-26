/* ===========================================================
 * StreetOwnableSquare ; ruas construtíveis; aluguel calculado por fórmula
 * =========================================================== */
package model;

final class StreetOwnableSquare extends OwnableSquare {

    private int houses;             // 0–4
    private boolean hasHotel;       // true = 1 hotel (só pode existir após ter 1 casa)
    
    StreetOwnableSquare(final int index,
                        final String name,
                        final String id,
                        final int price) {
        super(index, name, id, price);
        this.houses = 0;
        this.hasHotel = false;
    }

    // Pode construir casa (até 4 casas). 
    boolean canBuildHouse() {
        return houses < 4;
    }

    // Pode construir hotel (precisa ter pelo menos 1 casa e ainda não ter hotel). 
    boolean canBuildHotel() {
        return houses >= 1 && !hasHotel;
    }

    // Custo de construção de uma casa (50% do preço). 
    int getHouseCost() { 
        return (int) Math.round(getPrice() * 0.5); 
    }

    // Custo de construção do hotel (100% do preço). 
    int getHotelCost() { 
        return getPrice(); 
    }

    // Quantas casas a rua possui (0–4). 
    int getHouses() { return houses; }

    // Tem hotel? 
    boolean hasHotel() { return hasHotel; }

    // Constrói 1 casa. 
    void buildHouse() {
        if (!canBuildHouse()) throw new IllegalStateException("Não é possível construir mais casas aqui.");
        houses++;
    }

    // Constrói o hotel. 
    void buildHotel() {
        if (!canBuildHotel()) throw new IllegalStateException("Não é possível construir hotel aqui.");
        hasHotel = true;
    }
    
    // Remove o dono (caso seja o atual) e reseta construções. 
    @Override
    void removeOwner(final Player target) {
        if (this.getOwner() != null && this.getOwner().equals(target)) {
            this.houses = 0;
            this.hasHotel = false;
            setOwner(null);
        }
    }

    // Retorna o valor total investido pelo dono atual (preço + construções). 
    @Override
    int getTotalInvestment() {
        if (this.getOwner() == null) return 0;

        int spentOnBuilds = houses * getHouseCost() + (hasHotel ? getHotelCost() : 0) + (this.getOwner() != null ? getPrice() : 0);
        return spentOnBuilds;
    }

    // Calcula o aluguel conforme fórmula: Va = Vb + Vc*n + Vh
    // Vb = valor base (10% do preço)
    // Vc = valor por casa (15% do preço)
    // Vh = valor do hotel (30% do preço)
    @Override
    int calcRent(final GameEngine engine) {
        int price = getPrice();
        int vb = (int) Math.round(price * 0.1);  // valor base
        int vc = (int) Math.round(price * 0.15); // valor por casa
        int vh = hasHotel ? (int) Math.round(price * 0.3) : 0; // valor do hotel
        return vb + (vc * houses) + vh;
    }

    // Efeito ao cair na casa. 
    @Override
    void onLand(final Player player, final GameEngine engine, final EconomyService economy) {
        if (!hasOwner() || getOwner() == player) {
            return; // Não cobrar aluguel
        }
        final int rent = calcRent(engine);
        economy.chargeRent(player, getOwner(), rent);
    }
}
