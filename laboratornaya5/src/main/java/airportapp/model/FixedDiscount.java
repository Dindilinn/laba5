// src/airportapp/airportapp.model/FixedDiscount.java
package airportapp.model;

public class FixedDiscount implements DiscountStrategy {
    private final double discountAmount;

    public FixedDiscount(double discountAmount) throws InvalidTariffException {
        if (discountAmount < 0) {
            throw new InvalidTariffException("Скидка не может быть отрицательной");
        }
        if (discountAmount > 1e7) {
            throw new InvalidTariffException("Слишком большая скидка! Максимум — 10 млн.");
        }
        this.discountAmount = discountAmount;
    }

    @Override
    public double applyDiscount(double basePrice) {
        double discounted = basePrice - discountAmount;
        return Math.max(0.0, discounted);
    }
}
