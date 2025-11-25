// src/airportapp/view/MainWindow.java
package airportapp.view;

import airportapp.model.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {
    private final Airport airport = new Airport();
    private TariffTableModel tableModel;
    private JTable table; // ← ВАЖНО: поле класса

    public MainWindow() {
        setTitle("Система управления тарифами аэропорта");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        tableModel = new TariffTableModel(new ArrayList<>());
        table = new JTable(tableModel); // ← используем поле
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Загрузка данных при старте (из памяти — они сохраняются в TXT вручную)
        tableModel.setTariffs(airport.getTariffs());

        JScrollPane scrollPane = new JScrollPane(table);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Добавить");
        JButton editButton = new JButton("Изменить");
        JButton deleteButton = new JButton("Удалить");
        JButton findMaxButton = new JButton("Найти макс.");
        JButton sortButton = new JButton("Сортировка по итог. цене");
        JButton saveButton = new JButton("Сохранить");
        JButton loadButton = new JButton("Загрузить");

        addButton.addActionListener(this::handleAdd);
        editButton.addActionListener(this::handleEdit);
        deleteButton.addActionListener(this::handleDelete);
        findMaxButton.addActionListener(this::handleFindMax);
        sortButton.addActionListener(this::handleSort);
        saveButton.addActionListener(this::handleSave);
        loadButton.addActionListener(this::handleLoad);

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(findMaxButton);
        buttonPanel.add(sortButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setSize(850, 500);
        setLocationRelativeTo(null);
    }

    private void handleAdd(ActionEvent e) {
        TariffEditDialog dialog = new TariffEditDialog(this, null);
        Tariff result = dialog.showDialog();
        if (result != null) {
            airport.addTariff(result);
            tableModel.setTariffs(airport.getTariffs());
        }
    }

    private void handleEdit(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row == -1) {
            showError("Выберите тариф для изменения.");
            return;
        }
        Tariff old = tableModel.getTariffAt(row);
        TariffEditDialog dialog = new TariffEditDialog(this, old);
        Tariff updated = dialog.showDialog();
        if (updated != null) {
            airport.removeTariff(old);
            airport.addTariff(updated);
            tableModel.setTariffs(airport.getTariffs());
        }
    }

    private void handleDelete(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row == -1) {
            showError("Выберите тариф для удаления.");
            return;
        }
        Tariff toRemove = tableModel.getTariffAt(row);
        int confirm = JOptionPane.showConfirmDialog(this, "Удалить выбранный тариф?", "Подтверждение", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            airport.removeTariff(toRemove);
            tableModel.setTariffs(airport.getTariffs());
        }
    }

    private void handleFindMax(ActionEvent e) {
        Tariff max = airport.findMaxPriceTariff();
        if (max == null) {
            JOptionPane.showMessageDialog(this, "Нет добавленных тарифов.", "Информация", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, max.toString(), "Максимальный тариф", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private boolean sortAscending = true;
    private void handleSort(ActionEvent e) {
        List<Tariff> list = new ArrayList<>(airport.getTariffs());
        list.sort((t1, t2) -> {
            double p1 = t1.getPrice(), p2 = t2.getPrice();
            return sortAscending ? Double.compare(p1, p2) : Double.compare(p2, p1);
        });
        sortAscending = !sortAscending;
        tableModel.setTariffs(list);
    }

    private void handleSave(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("tariffs.txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                airport.saveToFile(chooser.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Данные сохранены в TXT!", "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showError("Ошибка: " + ex.getMessage());
            }
        }
    }

    private void handleLoad(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                airport.loadFromFile(chooser.getSelectedFile().getAbsolutePath());
                tableModel.setTariffs(airport.getTariffs());
                JOptionPane.showMessageDialog(this, "Данные загружены из TXT!", "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showError("Ошибка: " + ex.getMessage());
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    private static class TariffTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"Направление", "Базовая цена", "Итоговая цена"};
        private List<Tariff> data = new ArrayList<>();

        public TariffTableModel(List<Tariff> initial) {
            this.data = new ArrayList<>(initial);
        }

        public void setTariffs(List<Tariff> list) {
            this.data = new ArrayList<>(list);
            fireTableDataChanged();
        }

        public Tariff getTariffAt(int row) {
            return data.get(row);
        }

        @Override
        public int getRowCount() { return data.size(); }
        @Override
        public int getColumnCount() { return COLUMNS.length; }
        @Override
        public String getColumnName(int col) { return COLUMNS[col]; }
        @Override
        public Object getValueAt(int row, int col) {
            Tariff t = data.get(row);
            return switch (col) {
                case 0 -> t.getDestination();
                case 1 -> String.format("%.2f", t.getBasePrice());
                case 2 -> String.format("%.2f", t.getPrice());
                default -> "";
            };
        }
    }
}