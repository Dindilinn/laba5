// src/airportapp/view/TariffEditDialog.java
package airportapp.view;

import airportapp.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class TariffEditDialog extends JDialog {
    private Tariff result = null;
    private final JTextField destField = new JTextField(20);
    private final JTextField priceField = new JTextField(10);
    private final JCheckBox discCheck = new JCheckBox("Со скидкой");
    private final JTextField discField = new JTextField(10);

    public TariffEditDialog(Frame owner, Tariff existing) {
        super(owner, existing == null ? "Добавить тариф" : "Изменить тариф", true);
        setupUI(existing);
    }

    private void setupUI(Tariff existing) {
        setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Направление:"), gbc);
        gbc.gridx = 1; form.add(destField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Базовая цена:"), gbc);
        gbc.gridx = 1; form.add(priceField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; form.add(discCheck, gbc);
        gbc.gridx = 1; form.add(discField, gbc);
        discField.setEnabled(false);
        discCheck.addActionListener(e -> discField.setEnabled(discCheck.isSelected()));

        if (existing != null) {
            destField.setText(existing.getDestination());
            priceField.setText(String.valueOf(existing.getBasePrice()));
            double disc = existing.getBasePrice() - existing.getPrice();
            if (disc > 0) {
                discCheck.setSelected(true);
                discField.setText(String.valueOf(disc));
                discField.setEnabled(true);
            }
        }

        JPanel btns = new JPanel();
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Отмена");
        ok.addActionListener(this::validateAndClose);
        cancel.addActionListener(e -> dispose());
        btns.add(ok);
        btns.add(cancel);

        add(form, BorderLayout.CENTER);
        add(btns, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void validateAndClose(ActionEvent e) {
        try {
            String dest = validateDest(destField.getText());
            double base = validateNum(priceField.getText(), "базовая стоимость");
            DiscountStrategy strat = new NoDiscount();
            if (discCheck.isSelected()) {
                double disc = validateNum(discField.getText(), "скидка");
                strat = new FixedDiscount(disc);
            }
            result = new Tariff(dest, base, strat);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Tariff showDialog() {
        setVisible(true);
        return result;
    }

    private String validateDest(String s) throws InvalidTariffException {
        s = s.trim();
        if (s.isEmpty()) throw new InvalidTariffException("Направление не может быть пустым.");
        if (s.matches("^[0-9\\p{Punct}]+$")) throw new InvalidTariffException("Должно содержать хотя бы одну букву.");
        if (s.length() < 2) throw new InvalidTariffException("Слишком короткое (минимум 2 символа).");
        if (s.length() > 50) throw new InvalidTariffException("Слишком длинное (максимум 50 символов).");
        if (!s.matches("^[\\p{L}\\s\\-'’]+$")) throw new InvalidTariffException("Только буквы, пробелы, дефисы, апострофы.");
        return s;
    }

    private double validateNum(String s, String field) throws InvalidTariffException {
        if (s == null || s.trim().isEmpty()) {
            throw new InvalidTariffException("Поле '" + field + "' не может быть пустым.");
        }
        try {
            double v = Double.parseDouble(s.trim());
            if (Double.isNaN(v) || Double.isInfinite(v)) throw new InvalidTariffException("Некорректное число.");
            if (Math.abs(v) > 1e9) throw new InvalidTariffException("Слишком большое число.");
            if (v < 0) throw new InvalidTariffException(field + " не может быть отрицательной.");
            return v;
        } catch (NumberFormatException ex) {
            throw new InvalidTariffException("Некорректный формат числа.");
        }
    }
}