package model.api.dto;

import model.api.dto.PlayerColor;

/**
 * DTO representando uma transação de dinheiro ocorrida no sistema.
 */

public final class Transaction {

    public final String fromId;   // "BANK" ou nome do jogador (ex: "Player 1")
    public final PlayerColor fromColor; // null para BANK
    public final String toId;     // "BANK" ou nome do jogador
    public final PlayerColor toColor;   // null para BANK
    public final int amount;      // valor positivo da transferência
    public final int fromBalanceAfter; // saldo do pagador após a operação (ou caixa do banco se from==BANK)
    public final int toBalanceAfter;   // saldo do recebedor após a operação (ou caixa do banco se to==BANK)

    public Transaction(String fromId, PlayerColor fromColor, String toId, PlayerColor toColor, int amount, int fromBalanceAfter, int toBalanceAfter) {
        this.fromId = fromId;
        this.fromColor = fromColor;
        this.toId = toId;
        this.toColor = toColor;
        this.amount = amount;
        this.fromBalanceAfter = fromBalanceAfter;
        this.toBalanceAfter = toBalanceAfter;
    }

    @Override
    public String toString() {
        return String.format("Transaction{from=%s,to=%s,amt=%d}", fromId, toId, amount);
    }
}
