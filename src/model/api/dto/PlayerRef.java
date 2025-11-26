package model.api.dto;

public final class PlayerRef {
    private final String id;
    private final String name;
    private final PlayerColor color;

    public PlayerRef(String id, String name, PlayerColor color) {
        if (id == null || !id.matches("P[1-6]"))
            throw new IllegalArgumentException("Id deve ser 'P1'..'P6'..");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nome obrigatório");
        if (color == null) throw new IllegalArgumentException("Color obrigatório");
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public String id() { return id; }
    public String name() { return name; }
    public PlayerColor color() { return color; }

    public static PlayerRef of(int playerNumber, PlayerColor color, String name) {
        return new PlayerRef("P" + playerNumber, name, color);
    }
}
