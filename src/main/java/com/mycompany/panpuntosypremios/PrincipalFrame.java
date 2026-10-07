package com.mycompany.panpuntosypremios;


import javax.swing.*;
import java.awt.*;

public class PrincipalFrame extends JFrame {
    private final ServicioRestaurante servicio = new ServicioRestaurante();

    public PrincipalFrame() {
        setTitle("Pan, Puntos y Premios");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180,760);
        setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Clientes", new ClientesPanel(servicio));
        tabs.addTab("Productos y menús", new ProductosPanel(servicio));
        tabs.addTab("Inventario", new InventarioPanel(servicio));
        tabs.addTab("Pedidos", new PedidosPanel(servicio));
        tabs.addTab("Cobro", new CobroPanel(servicio));
        tabs.addTab("Fidelización", new FidelizacionPanel(servicio));
        tabs.addTab("Reportes", new ReportesPanel(servicio));
        JLabel estado = new JLabel("Verificando conexión...");
        add(tabs, BorderLayout.CENTER);
        add(estado, BorderLayout.SOUTH);
        new SwingWorker<Boolean,Void>() {
            protected Boolean doInBackground() { try { servicio.probarConexion(); return true; } catch (Exception e) { return false; } }
            protected void done() { try { estado.setText(get() ? "Oracle: conectado" : "Oracle: sin conexión. Revise db.properties y ejecute los scripts SQL."); } catch (Exception e) { estado.setText("Oracle: sin conexión"); } }
        }.execute();
    }
}
