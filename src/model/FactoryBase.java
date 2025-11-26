/* ===========================================================
 * FactoryBase — base genérica para leitura de CSVs e construção de entidades.
 * =========================================================== */


package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

abstract class FactoryBase<T> {

    protected List<T> readCSV(final Path csvPath, final List<String> expectedHeader) {
        Objects.requireNonNull(csvPath);
        Objects.requireNonNull(expectedHeader);

        List<T> items = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(csvPath)) {
            String header = br.readLine();
            validateHeader(header, expectedHeader);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length != expectedHeader.size())
                    throw new IllegalArgumentException("Linha inválida: " + line);
                items.add(parseLine(parts));
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler CSV: " + e.getMessage(), e);
        }

        return items;
    }

    // Método abstrato para ser implementado nas fábricas concretas
    protected abstract T parseLine(String[] parts);

    // Valida se o header está correto
    protected void validateHeader(String header, List<String> expectedHeader) {
        if (header == null) throw new IllegalArgumentException("CSV vazio.");
        String[] cols = header.split(",", -1);
        for (int i = 0; i < expectedHeader.size(); i++) {
            if (!cols[i].trim().equalsIgnoreCase(expectedHeader.get(i)))
                throw new IllegalArgumentException("Cabeçalho inválido. Esperado: " + expectedHeader);
        }
    }

    // Converte string numérica para int
    protected int parseInt(String s) {
        if (s == null || s.isBlank()) return 0;
        return Integer.parseInt(s.trim());
    }
}
