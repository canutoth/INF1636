package model.api.dto;

public interface OwnableInfo {
    Core core();
    // Parte comum a qualquer ownable
    public static final class Core {
        private final PlayerRef owner;
        private final String propertyName;
        private final int boardIndex;
        private final int propertyPrice;
        private final int propertySellValue;

        public Core(PlayerRef owner, String propertyName, int boardIndex, int propertyPrice, int propertySellValue) {
            if (boardIndex < 0) throw new IllegalArgumentException("boardIndex < 0");
            if (propertyPrice < 0) throw new IllegalArgumentException("price < 0");
            if (propertySellValue < 0) throw new IllegalArgumentException("sell < 0");
            this.owner = owner;
            this.propertyName = propertyName;
            this.boardIndex = boardIndex;
            this.propertyPrice = propertyPrice;
            this.propertySellValue = propertySellValue;
        }

        public PlayerRef owner() { return owner; }
        public String propertyName() { return propertyName; }
        public int boardIndex() { return boardIndex; }
        public int propertyPrice() { return propertyPrice; }
        public int propertySellValue() { return propertySellValue; }
    }
}
