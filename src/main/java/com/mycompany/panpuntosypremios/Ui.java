package com.mycompany.panpuntosypremios;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;

final class Ui {
    private Ui() {}
    static JPanel fila(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (Component c : comps) p.add(c);
        return p;
    }
    static JScrollPane scroll(JTable t) { return new JScrollPane(t); }
    static void error(Component c, Exception e) { JOptionPane.showMessageDialog(c, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
    static void info(Component c, String s) { JOptionPane.showMessageDialog(c, s, "Información", JOptionPane.INFORMATION_MESSAGE); }
    static void tablaDialog(Component c, String titulo, TableModel model) {
        JTable t = new JTable(model);
        JScrollPane sp = new JScrollPane(t);
        sp.setPreferredSize(new Dimension(700,350));
        JOptionPane.showMessageDialog(c, sp, titulo, JOptionPane.INFORMATION_MESSAGE);
    }
}
