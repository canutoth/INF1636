/* ===========================================================
 * Bank ; executa débitos/créditos e operações com o banco como contraparte.
 * Mantém apenas o caixa; regras/validações vivem na EconomyService.
 * =========================================================== */

package model;

final class Bank {

    // --- Caixa do banco ---
    private int cash;
    // Registro de transações desde a última drenagem
    private final java.util.List<model.api.dto.Transaction> transactions = new java.util.ArrayList<>();

    Bank(final int initialCash) {
        if (initialCash < 0) throw new IllegalArgumentException("Caixa inicial inválido.");
        this.cash = initialCash;
    }
    
    /** Retorna o dinheiro atual do banco. */
    int getCash() {
        return cash;
    }

    /* ===========================================================
     * Transferência genérica de dinheiro.
     * Convenção: passar null indica a ponta "BANK".
     *  - from == null  => BANK paga
     *  - to   == null  => BANK recebe
     * ===========================================================
     */
    void transfer(final Player from, final Player to, final int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount deve ser >= 0");

        if (from == null && to == null) {
            throw new IllegalArgumentException("Pelo menos uma ponta deve ser Player.");
        }

        if (from == null) { // BANK -> Player
            ensureBankHas(amount);
            to.credit(amount);
            cash -= amount;
            // registra transação (BANK -> Player)
            transactions.add(new model.api.dto.Transaction(
                    "BANK", null,
                    to.getName(), to.getColor(),
                    amount,
                    cash, // caixa do banco após pagamento
                    to.getMoney() // saldo do jogador após crédito
            ));
            return;
        }

        if (to == null) { // Player -> BANK
            from.debit(amount); 
            cash += amount;
            // registra transação (Player -> BANK)
            transactions.add(new model.api.dto.Transaction(
                    from.getName(), from.getColor(),
                    "BANK", null,
                    amount,
                    from.getMoney(), // saldo do jogador após débito
                    cash // caixa do banco após recebimento
            ));
            return;
        }

        // Player -> Player
        from.debit(amount);
        to.credit(amount);
        // registra transação (Player -> Player)
        transactions.add(new model.api.dto.Transaction(
                from.getName(), from.getColor(),
                to.getName(), to.getColor(),
                amount,
                from.getMoney(),
                to.getMoney()
        ));
        // caixa do banco não muda
    }

    /* ===========================================================
     * Utilidades
     * ===========================================================
     */
    
    private void ensureBankHas(final long amount) {
        if (amount > Integer.MAX_VALUE) throw new IllegalArgumentException("Valor excessivo.");
        if (cash < amount) {
            throw new IllegalStateException("Banco sem caixa suficiente para a operação.");
        }
    }

    /**
     * Retorna e limpa o registro de transações acumuladas desde a última chamada.
     */
    java.util.List<model.api.dto.Transaction> drainTransactions() {
        java.util.List<model.api.dto.Transaction> out = new java.util.ArrayList<>(transactions);
        transactions.clear();
        return out;
    }
}
