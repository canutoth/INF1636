/* ===========================================================
 * GameStateLoader ; carrega o estado completo do jogo de CSV.
 * Lê jogadores, propriedades, deck e configurações.
 * =========================================================== */

package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import model.api.dto.PlayerColor;
import model.api.dto.PlayerRef;

/**
 * Classe responsável por deserializar o estado do jogo de um arquivo CSV.
 * Contém dados necessários para restaurar uma partida salva.
 */
final class GameStateLoader {
    
    /**
     * Dados carregados de uma partida salva.
     */
    static class SavedGameData {
        final int currentPlayerIndex;
        final List<PlayerData> players;
        final List<StreetPropertyData> streetProperties;
        final List<CompanyPropertyData> companyProperties;
        final List<CardData> deckCards;
        final int getOutOfJailCardsOut;
        final int bankCash;
        
        SavedGameData(int currentPlayerIndex,
                     List<PlayerData> players,
                     List<StreetPropertyData> streetProperties,
                     List<CompanyPropertyData> companyProperties,
                     List<CardData> deckCards,
                     int getOutOfJailCardsOut,
                     int bankCash) {
            this.currentPlayerIndex = currentPlayerIndex;
            this.players = players;
            this.streetProperties = streetProperties;
            this.companyProperties = companyProperties;
            this.deckCards = deckCards;
            this.getOutOfJailCardsOut = getOutOfJailCardsOut;
            this.bankCash = bankCash;
        }
    }
    
    /**
     * Dados de um jogador carregado.
     */
    static class PlayerData {
        final String id;
        final String name;
        final PlayerColor color;
        final int money;
        final int position;
        final boolean inJail;
        final int getOutOfJailCards;
        final boolean alive;
        
        PlayerData(String id, String name, PlayerColor color, int money, 
                  int position, boolean inJail, int getOutOfJailCards, boolean alive) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.money = money;
            this.position = position;
            this.inJail = inJail;
            this.getOutOfJailCards = getOutOfJailCards;
            this.alive = alive;
        }
        
        PlayerRef toPlayerRef() {
            return new PlayerRef(id, name, color);
        }
    }
    
    /**
     * Dados de uma propriedade de rua (street) carregada.
     */
    static class StreetPropertyData {
        final int squareIndex;
        final String ownerId;
        final int houses;
        final boolean hasHotel;
        
        StreetPropertyData(int squareIndex, String ownerId, int houses, boolean hasHotel) {
            this.squareIndex = squareIndex;
            this.ownerId = ownerId;
            this.houses = houses;
            this.hasHotel = hasHotel;
        }
    }
    
    /**
     * Dados de uma propriedade de companhia carregada.
     */
    static class CompanyPropertyData {
        final int squareIndex;
        final String ownerId;
        
        CompanyPropertyData(int squareIndex, String ownerId) {
            this.squareIndex = squareIndex;
            this.ownerId = ownerId;
        }
    }
    
    /**
     * Dados de uma carta carregada.
     */
    static class CardData {
        final int cardId;
        final String cardType;
        final int cardValue;
        
        CardData(int cardId, String cardType, int cardValue) {
            this.cardId = cardId;
            this.cardType = cardType;
            this.cardValue = cardValue;
        }
    }
    
    private GameStateLoader() {
        // Utility class
    }
    
    /**
     * Carrega o estado do jogo de um arquivo CSV.
     * 
     * @param loadPath caminho do arquivo salvo
     * @return dados do jogo salvo
     * @throws IOException se houver erro ao ler o arquivo
     */
    static SavedGameData loadGame(Path loadPath) throws IOException {
        int currentPlayerIndex = 0;
        List<PlayerData> players = new ArrayList<>();
        List<StreetPropertyData> streetProperties = new ArrayList<>();
        List<CompanyPropertyData> companyProperties = new ArrayList<>();
        List<CardData> deckCards = new ArrayList<>();
        int getOutOfJailCardsOut = 0;
        int bankCash = 200000; // valor padrão
        
        try (BufferedReader reader = Files.newBufferedReader(loadPath, StandardCharsets.US_ASCII)) {
            String line;
            String currentSection = "";
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Ignora linhas vazias
                if (line.isEmpty()) {
                    continue;
                }
                
                // Identifica seções
                if (line.startsWith("# GAME_INFO")) {
                    currentSection = "GAME_INFO";
                    continue;
                } else if (line.startsWith("# PLAYERS")) {
                    currentSection = "PLAYERS";
                    continue;
                } else if (line.startsWith("# PROPERTIES_STREETS")) {
                    currentSection = "PROPERTIES_STREETS";
                    continue;
                } else if (line.startsWith("# PROPERTIES_COMPANIES")) {
                    currentSection = "PROPERTIES_COMPANIES";
                    continue;
                } else if (line.startsWith("# DECK_STATE")) {
                    currentSection = "DECK_STATE";
                    continue;
                } else if (line.startsWith("# JAIL_CARDS_OUT")) {
                    currentSection = "JAIL_CARDS_OUT";
                    continue;
                } else if (line.startsWith("#")) {
                    // Ignora comentários
                    continue;
                }
                
                // Processa dados de acordo com a seção
                String[] parts = line.split(",");
                
                switch (currentSection) {
                    case "GAME_INFO":
                        if (parts[0].equals("currentPlayerIndex")) {
                            currentPlayerIndex = Integer.parseInt(parts[1]);
                        } else if (parts[0].equals("bankCash")) {
                            bankCash = Integer.parseInt(parts[1]);
                        }
                        break;
                        
                    case "PLAYERS":
                        if (parts.length == 8) {
                            players.add(new PlayerData(
                                parts[0], // id
                                parts[1], // name
                                PlayerColor.valueOf(parts[2]), // color
                                Integer.parseInt(parts[3]), // money
                                Integer.parseInt(parts[4]), // position
                                Boolean.parseBoolean(parts[5]), // inJail
                                Integer.parseInt(parts[6]), // getOutOfJailCards
                                Boolean.parseBoolean(parts[7]) // alive
                            ));
                        }
                        break;
                        
                    case "PROPERTIES_STREETS":
                        if (parts.length == 4) {
                            streetProperties.add(new StreetPropertyData(
                                Integer.parseInt(parts[0]), // squareIndex
                                parts[1], // ownerId
                                Integer.parseInt(parts[2]), // houses
                                Boolean.parseBoolean(parts[3]) // hasHotel
                            ));
                        }
                        break;
                        
                    case "PROPERTIES_COMPANIES":
                        if (parts.length == 2) {
                            companyProperties.add(new CompanyPropertyData(
                                Integer.parseInt(parts[0]), // squareIndex
                                parts[1] // ownerId
                            ));
                        }
                        break;
                        
                    case "DECK_STATE":
                        if (parts.length == 3) {
                            deckCards.add(new CardData(
                                Integer.parseInt(parts[0]), // cardId
                                parts[1], // cardType
                                Integer.parseInt(parts[2]) // cardValue
                            ));
                        }
                        break;
                        
                    case "JAIL_CARDS_OUT":
                        if (parts[0].equals("getOutOfJailCardsOut")) {
                            getOutOfJailCardsOut = Integer.parseInt(parts[1]);
                        }
                        break;
                }
            }
        }
        
        return new SavedGameData(currentPlayerIndex, players, streetProperties, 
                                companyProperties, deckCards, getOutOfJailCardsOut, bankCash);
    }
}
