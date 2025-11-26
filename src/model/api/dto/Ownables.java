package model.api.dto;

public final class Ownables {
    private Ownables() {}

    // ==== Street ====
    public static final class Street implements OwnableInfo {
        private final OwnableInfo.Core core;
        private final int propertyActualRent;
        private final int propertyHouseNumber;
        private final boolean propertyHasHotel;

        public Street(OwnableInfo.Core core, int propertyActualRent, int propertyHouseNumber, boolean propertyHasHotel) {
            if (core == null) throw new IllegalArgumentException("Core obrigatório");
            if (propertyActualRent < 0) throw new IllegalArgumentException("Rent >= 0");
            if (propertyHouseNumber < 0 || propertyHouseNumber > 4)
                throw new IllegalArgumentException("HouseNumber deve ser 0..4");
            if (propertyHasHotel && propertyHouseNumber < 1)
                throw new IllegalArgumentException("Hotel exige houseNumber==1");
            this.core = core;
            this.propertyActualRent = propertyActualRent;
            this.propertyHouseNumber = propertyHouseNumber;
            this.propertyHasHotel = propertyHasHotel;
        }

        @Override public OwnableInfo.Core core() { return core; }
        public int propertyActualRent() { return propertyActualRent; }
        public int propertyHouseNumber() { return propertyHouseNumber; }
        public boolean propertyHasHotel() { return propertyHasHotel; }
    }

    // ==== Company ====
    public static final class Company implements OwnableInfo {
        private final OwnableInfo.Core core;
        private final int propertyMultiplier;

        public Company(OwnableInfo.Core core, int propertyMultiplier) {
            if (core == null) throw new IllegalArgumentException("Core obrigatório");
            if (propertyMultiplier <= 0) throw new IllegalArgumentException("Multiplier > 0");
            this.core = core;
            this.propertyMultiplier = propertyMultiplier;
        }

        @Override public OwnableInfo.Core core() { return core; }
        public int propertyMultiplier() { return propertyMultiplier; }
    }
}
