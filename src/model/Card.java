/* ===========================================================
 * Card ; carta de Sorte/Revés (efeito + valor).
 * =========================================================== */

package model;

import java.util.Objects;

final class Card {

    private final int id;

    enum CardType {
        PAY_BANK,       // paga ao banco
        RECEIVE_BANK,   // recebe do banco
        PAY_ALL,        // paga a todos os jogadores
        RECEIVE_ALL,    // recebe de todos os jogadores
        GO_TO_JAIL,     // vai para a prisão
        GET_OUT_OF_JAIL // ganha carta de saída livre
    }


    private final CardType type;
    private final int value; // usado nos tipos PAYMENT/ALL

    Card(final int id, final CardType type, final int value) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "type");
        this.value = value;
    }

    int getId() { return id; }

    CardType type() { return type; }
    int value() { return value; }

    /** Aplica o efeito da carta. */
    void applyEffect(final Player player, final GameEngine engine, final EconomyService economy) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(engine);
        Objects.requireNonNull(economy);

        switch (type) {
            case PAY_BANK: {
                economy.applyPayment(player, value);
                break;
            }
            case RECEIVE_BANK: {
                economy.applyIncome(player, value);
                break;
            }
            case PAY_ALL: {
                for (Player other : engine.allPlayers()) {
                    if (other != player && other.isAlive()) {
                        economy.transfer(player, other, value);
                    }
                }
                break;
            }
            case RECEIVE_ALL: {
                for (Player other : engine.allPlayers()) {
                    if (other != player && other.isAlive()) {
                        economy.transfer(other, player, value);
                    }
                }
                break;
            }
            case GO_TO_JAIL: {
                engine.sendToJail(player);
                break;
            }
            case GET_OUT_OF_JAIL: {
                player.grantGetOutOfJailCard();
                break;
            }
            default: {
                break;
            }
        }
    }
}
