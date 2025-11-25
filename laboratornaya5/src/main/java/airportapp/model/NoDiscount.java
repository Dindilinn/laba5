// src/airportapp/airportapp.model/NoDiscount.java
package airportapp.model;

public class NoDiscount implements DiscountStrategy {
    public NoDiscount() {}

    @Override
    public double applyDiscount(double basePrice) {
        return basePrice;
    }
}