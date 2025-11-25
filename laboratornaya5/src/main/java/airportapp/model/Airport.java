// src/airportapp/model/Airport.java
package airportapp.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Airport {
    // Абсолютный путь — данные не потеряются
    private static final String DB_PATH = System.getProperty("user.home") + "/airport.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private static final String CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS tariffs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            destination TEXT NOT NULL,
            base_price REAL NOT NULL,
            discount REAL NOT NULL
        );
        """;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            initDatabase();
        } catch (ClassNotFoundException | SQLException e) {
            throw new ExceptionInInitializerError("Не удалось инициализировать БД: " + e.getMessage());
        }
    }

    private static void initDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
        }
    }

    // === SQLite: основные операции ===
    public void addTariff(Tariff tariff) {
        String sql = "INSERT INTO tariffs(destination, base_price, discount) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tariff.getDestination());
            pstmt.setDouble(2, tariff.getBasePrice());
            pstmt.setDouble(3, tariff.getBasePrice() - tariff.getPrice());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка добавления тарифа: " + e.getMessage(), e);
        }
    }

    public boolean removeTariff(Tariff tariff) {
        String sql = "DELETE FROM tariffs WHERE destination = ? AND base_price = ? AND discount = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tariff.getDestination());
            pstmt.setDouble(2, tariff.getBasePrice());
            pstmt.setDouble(3, tariff.getBasePrice() - tariff.getPrice());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления тарифа: " + e.getMessage(), e);
        }
    }

    public List<Tariff> getTariffs() {
        List<Tariff> list = new ArrayList<>();
        String sql = "SELECT destination, base_price, discount FROM tariffs";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String dest = rs.getString("destination");
                double base = rs.getDouble("base_price");
                double discount = rs.getDouble("discount");
                DiscountStrategy strategy = (discount > 0) ? new FixedDiscount(discount) : new NoDiscount();
                list.add(new Tariff(dest, base, strategy));
            }
        } catch (SQLException | InvalidTariffException e) {
            throw new RuntimeException("Ошибка загрузки тарифов: " + e.getMessage(), e);
        }
        return list;
    }

    public Tariff findMaxPriceTariff() {
        String sql = """
            SELECT destination, base_price, discount 
            FROM tariffs 
            ORDER BY (base_price - discount) DESC 
            LIMIT 1
            """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String dest = rs.getString("destination");
                double base = rs.getDouble("base_price");
                double discount = rs.getDouble("discount");
                DiscountStrategy strategy = (discount > 0) ? new FixedDiscount(discount) : new NoDiscount();
                return new Tariff(dest, base, strategy);
            }
        } catch (SQLException | InvalidTariffException e) {
            throw new RuntimeException("Ошибка поиска макс. тарифа: " + e.getMessage(), e);
        }
        return null;
    }

    // === TXT: экспорт и импорт ===
    public void saveToFile(String filename) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
            for (Tariff t : getTariffs()) {
                double discount = t.getBasePrice() - t.getPrice();
                writer.write(String.format("%s|%.2f|%.2f", t.getDestination(), t.getBasePrice(), discount));
                writer.newLine();
            }
        }
    }

    public void loadFromFile(String filename) throws IOException, InvalidTariffException {
        // Очистка БД
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM tariffs");
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось очистить БД", e);
        }

        // Загрузка из файла и вставка в БД
        List<String> lines = Files.readAllLines(Paths.get(filename));
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\|");
            if (parts.length != 3) {
                throw new InvalidTariffException("Некорректный формат строки: " + line);
            }
            String destination = parts[0];
            double basePrice = Double.parseDouble(parts[1].replace(',', '.'));
            double discount = Double.parseDouble(parts[2].replace(',', '.'));
            DiscountStrategy strategy = (discount > 0) ? new FixedDiscount(discount) : new NoDiscount();
            addTariff(new Tariff(destination, basePrice, strategy));
        }
    }
}