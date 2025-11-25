// src/airportapp/airportapp.model/Tariff.java
package airportapp.model;

public class Tariff {
    private final String destination;
    private final double basePrice;
    private final DiscountStrategy discountStrategy;

    public Tariff(String destination, double basePrice, DiscountStrategy discountStrategy) throws InvalidTariffException {
        if (destination == null || destination.trim().isEmpty()) {
            throw new InvalidTariffException("Направление не может быть пустым");
        }
        if (basePrice < 0) {
            throw new InvalidTariffException("Цена не может быть отрицательной");
        }
        if (basePrice > 1e7) {
            throw new InvalidTariffException("Слишком высокая цена! Максимум — 10 млн.");
        }
        this.destination = destination.trim();
        this.basePrice = basePrice;
        this.discountStrategy = (discountStrategy != null) ? discountStrategy : new NoDiscount();
    }

    public String getDestination() {
        return destination;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getPrice() {
        return discountStrategy.applyDiscount(basePrice);
    }

    @Override
    public String toString() {
        return String.format("Направление: %s, Базовая цена: %.2f, Итоговая цена: %.2f",
                destination, basePrice, getPrice());
    }
}