package helloswing;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DialogUtils {

    public static String showInputDialog(Component parent, String message, String title) {
        return JOptionPane.showInputDialog(parent, message, title, JOptionPane.PLAIN_MESSAGE);
    }

    public static void showErrorDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showErrorDialog(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfoDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static int showConfirmDialog(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION);
    }

    public static Map<String, Object> getConditionsFromUser() {
        Map<String, Object> conditions = new HashMap<>();
        JPanel panel = new JPanel(new GridLayout(0, 2));
        JTextField keyField = new JTextField();
        JTextField valueField = new JTextField();
        keyField.setToolTipText("Enter the column name for the condition");
        valueField.setToolTipText("Enter the value for the condition");
        panel.add(new JLabel("Column:"));
        panel.add(keyField);
        panel.add(new JLabel("Value:"));
        panel.add(valueField);
        int result = JOptionPane.showConfirmDialog(null, panel, "Enter Conditions", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String key = keyField.getText().trim();
            String value = valueField.getText().trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                conditions.put(key, value);
            }
        }
        return conditions;
    }

    public static String showColumnInputDialog(Component parent) {
        JPanel panel = new JPanel();
        JTextField textField = new JTextField(20);
        textField.setToolTipText("Example: id INT PRIMARY KEY, name VARCHAR(100), ...");
        panel.add(new JLabel("Enter columns:"));
        panel.add(textField);
        int result = JOptionPane.showConfirmDialog(parent, panel, "Column Input", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            return textField.getText();
        }
        return null;
    }

    public static Map<String, Object> showTableAndColumnsDialog(Component parent, String[] tables) {
        Map<String, Object> result = new HashMap<>();
        JPanel panel = new JPanel(new GridLayout(0, 2));
        JComboBox<String> tableComboBox = new JComboBox<>(tables);
        JTextField columnsField = new JTextField();
        tableComboBox.setToolTipText("Select the table");
        columnsField.setToolTipText("Enter columns separated by commas (e.g., id, name, age)");
        panel.add(new JLabel("Table:"));
        panel.add(tableComboBox);
        panel.add(new JLabel("Columns:"));
        panel.add(columnsField);
        int dialogResult = JOptionPane.showConfirmDialog(parent, panel, "Select Table and Columns", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (dialogResult == JOptionPane.OK_OPTION) {
            String selectedTable = (String) tableComboBox.getSelectedItem();
            String columns = columnsField.getText().trim();
            if (selectedTable != null && !selectedTable.isEmpty() && !columns.isEmpty()) {
                result.put("table", selectedTable);
                result.put("columns", columns);
            }
        }
        return result;
    }
}