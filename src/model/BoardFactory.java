/* ===========================================================
 * BoardFactory ; cria o tabuleiro completo a partir de um CSV.
 * =========================================================== */

package model;

import java.nio.file.Path;
import java.util.*;

final class BoardFactory extends FactoryBase<Square> {

    // Índice da cadeia detectado durante o parse das linhas CSV (quando houver uma linha do tipo JAIL)
    private int jailIndex = -1;

    private static final List<String> EXPECTED_HEADER = List.of(
        "index","type","name","price","multiplier","value"
    );

    static Board fromCSV(final Path csvPath) {
        BoardFactory factory = new BoardFactory();
        List<Square> squares = factory.readCSV(csvPath, EXPECTED_HEADER);
        
        if (factory.jailIndex == -1) {
            throw new IllegalStateException("Nenhuma JailSquare encontrada no tabuleiro.");
        }

        return new Board(squares, factory.jailIndex);
    }

    @Override
    protected Square parseLine(String[] p) {
        int index = parseInt(p[0]);
        String type = p[1].trim().toUpperCase(Locale.ROOT);
        String name = p[2].trim();
        int price = parseInt(p[3]);
        int multiplier = parseInt(p[4]);
        int value = parseInt(p[5]);

        // Algumas casas (START, JAIL, PARKING) não possuem regra de negócio
        if (type.equals("START") || type.equals("JAIL") || type.equals("PARKING")) {
            // Se for JAIL, registramos o índice para o Board
            if (type.equals("JAIL")) this.jailIndex = index;
            return new DummySquare(index, name);
        }

        return switch (type) {
            case "STREET" -> new StreetOwnableSquare(
                index, name, name.toUpperCase(), price);
            case "COMPANY" -> new CompanyOwnableSquare(
                index, name, name.toUpperCase(), price, multiplier);
            case "MONEY" -> new MoneySquare(index, name, value);
            case "GOTOJAIL" -> new GoToJailSquare(index, name);
            case "CHANCE" -> new ChanceSquare(index, name);
            default -> throw new IllegalArgumentException("Valor inesperado ao criar quadrado de tabuleiro: " + type);
        };
    }
}
