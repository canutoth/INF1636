/* ===========================================================
 * EconomyService ; aplica regras financeiras e garante liquidez/falência.
 * Delegação de execução ($) para o Bank.
 * =========================================================== */

package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class EconomyService {

    private final Bank bank;
    private static final double BANK_BUYBACK_RATE = 0.90;
    private static final int PASS_START_AMOUNT = 200;

    EconomyService(final Bank bank) {
        this.bank = Objects.requireNonNull(bank, "bank");
    }
    
    /** Retorna o banco (para salvar estado). */
    Bank getBank() {
        return bank;
    }

    /* ===========================================================
     * Aluguel: cobra do visitante um valor já calculado e o credita ao dono.
     * Recebe explicitamente o dono e o valor do aluguel para evitar que
     * o serviço precise inspecionar a propriedade inteira.
     * =========================================================== */
    void chargeRent(final Player visitor, final Player owner, final int rent) {

        if (rent <= 0) return;

        // Garante liquidez do pagador
        if (!liquidateOrBankruptIfNeeded(visitor, rent)) {
            return;
        }

        // Executa a transferência Player -> Player
        bank.transfer(visitor, owner, rent);
    }

    /* ===========================================================
     * Compra de propriedade do banco para o jogador.
     * =========================================================== */
    boolean attemptBuy(final Player player, final OwnableSquare property) {
    	
        if (property.hasOwner()) return false;
        
        final int price = property.getPrice();

        boolean canPay = player.canAfford(price);
        if (!canPay) return false;
        
        // Player -> BANK
        bank.transfer(player, null, price);

        // Transfere título
        property.setOwner(player);
        player.addProperty(property);
        return true;
    }

    /* ===========================================================
     * Construção de casa em rua do próprio jogador.
     * =========================================================== */
    boolean attemptBuildHouse(final Player player, final StreetOwnableSquare street) {
    	
        if (!street.hasOwner() || street.getOwner() != player) return false;
        if (!street.canBuildHouse()) return false;

        final int cost = street.getHouseCost();

        boolean canPay = player.canAfford(cost);
        if (!canPay) return false;
        
        bank.transfer(player, null, cost);
        street.buildHouse();
        return true;
    }

    /* ===========================================================
     * Construção de hotel em rua do próprio jogador.
     * =========================================================== */
    boolean attemptBuildHotel(final Player player, final StreetOwnableSquare street) {
    	
        if (!street.hasOwner() || street.getOwner() != player) return false;
        if (!street.canBuildHotel()) return false;

        final int cost = street.getHotelCost();

        boolean canPay = player.canAfford(cost);
        if (!canPay) return false;
        
        bank.transfer(player, null, cost);
        street.buildHotel();
        return true;
    }

    /* ===========================================================
     * Transferência direta entre jogadores (Player ↔ Player).
     * =========================================================== */
    void transfer(final Player from, final Player to, final int amount) {
        if (amount <= 0) return;
        if (from.isBankrupt()) return;

        // Garante liquidez do pagador
        if (!liquidateOrBankruptIfNeeded(from, amount)) {
        	 return; 
        }

        bank.transfer(from, to, amount);
    }
    
    /* ===========================================================
     * Aplica pagamento do jogador ao banco.
     * =========================================================== */
    void applyPayment(final Player player, final int amount) {
        if (amount <= 0) return;

        // Garante liquidez do pagador
        if (!liquidateOrBankruptIfNeeded(player, amount)) {
            return; // jogador já foi declarado falido
        }

        // Player → BANK
        bank.transfer(player, null, amount);
    }

    /* ===========================================================
     * Aplica pagamento do banco ao jogador
     * =========================================================== */
    
    void applyIncome(final Player player, final int amount) {
        if (amount <= 0) return;

        // Banco transfere dinheiro ao jogador (BANK -> Player)
        bank.transfer(null, player, amount);
    }

    /* ===========================================================
     * Se necessário liquida imóveis ou leva o jogador a falência
     * =========================================================== */
    boolean liquidateOrBankruptIfNeeded(final Player player, final int required) {
        Objects.requireNonNull(player, "player");
        
        // Verifica se o jogador tem saldo suficiente
        boolean canPay = player.canAfford(required);
       
        // Se já pode pagar, nada a fazer
        if (canPay) return true;

        // Faltando
        int missing = player.howMuchMissing(required);
        
        // Tenta vender propriedades para cobrir o valor faltante
        final List<OwnableSquare> owned = new ArrayList<>(player.getProperties());
        for (OwnableSquare prop : owned) {
            final int received = buybackPropertyToPlayer(prop, player);
            missing -= received;
            if (missing <= 0) return true;
        }

        // Se ainda falta dinheiro → falência
        declareBankruptcy(player);
        return false;
    }

    /* ===========================================================
     * Falência: remove jogador do jogo e devolve seus títulos.
     * =========================================================== */
    void declareBankruptcy(final Player player) {

        // Devolve todos os títulos ao banco (sem pagamento adicional)
        for (OwnableSquare prop : new ArrayList<>(player.getProperties())) {
            player.removeProperty(prop);
            prop.removeOwner(player); 
        }
        player.setBankrupt();
    }
    
    /* ===========================================================
     * Venda de propriedade do jogador para o banco.
     * =========================================================== */
    int buybackPropertyToPlayer(final OwnableSquare prop, final Player player) {
        final int gross = prop.getTotalInvestment();
        final int received = (int) Math.floor(gross * BANK_BUYBACK_RATE);

        // Banco paga ao jogador (BANK -> Player)
        bank.transfer(null, player, received);

        // Remove propriedade do jogador e limpa a posse
        player.removeProperty(prop);
        prop.removeOwner(player);

        return received;
    }

    /* ===========================================================
     * Avalia o valor de recompra de uma propriedade pelo banco.
     * =========================================================== */
    int evaluateSellValue(final OwnableSquare prop) {
        final int gross = prop.getTotalInvestment(); 
        return (int) Math.floor(gross * BANK_BUYBACK_RATE);
    }

    /* ===========================================================
     * Bônus por cruzar a linha de partida: credita um valor fixo ao jogador.
     * Método package-private para ser chamado pelo GameEngine.
     * =========================================================== */
    void creditPassStart(final Player player) {
        Objects.requireNonNull(player, "player");
        applyIncome(player, PASS_START_AMOUNT);
    }

    /* ===========================================================
     * Venda voluntária do jogador para o banco.
     * =========================================================== */
    boolean attemptSell(final Player player, final OwnableSquare prop) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(prop, "prop");
        if (!prop.hasOwner() || prop.getOwner() != player) return false;

        buybackPropertyToPlayer(prop, player);
        return true;
    }

    /* ===========================================================
     * Drena (retorna e limpa) o log de transações do banco.
     * =========================================================== */
    java.util.List<model.api.dto.Transaction> drainTransactionLog() {
        return bank.drainTransactions();
    }
}

