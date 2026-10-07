package com.mycompany.panpuntosypremios;


import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class ReportesPanel extends JPanel {
    private final ServicioRestaurante servicio;
    private final JComboBox<String> reporte=new JComboBox<>(new String[]{"Ventas por período","Productos más vendidos","Productos agotados o con bajo inventario","Puntos acumulados por cliente","Historial de canjes","Ventas de menús completos"});
    private final JTextField inicio=new JTextField(LocalDate.now().withDayOfMonth(1).toString(),10),fin=new JTextField(LocalDate.now().toString(),10);
    private final JTable tabla=new JTable(),resumen=new JTable();

    public ReportesPanel(ServicioRestaurante servicio){
        this.servicio=servicio;setLayout(new BorderLayout(6,6));JButton generar=new JButton("Generar reporte");add(Ui.fila(new JLabel("Reporte"),reporte,new JLabel("Fecha inicio YYYY-MM-DD"),inicio,new JLabel("Fecha fin"),fin,generar),BorderLayout.NORTH);JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(tabla),new JScrollPane(resumen));split.setResizeWeight(.8);add(split,BorderLayout.CENTER);generar.addActionListener(e->generar());generar();
    }
    private void generar(){try{String r=String.valueOf(reporte.getSelectedItem());resumen.setModel(new javax.swing.table.DefaultTableModel());switch(r){case "Ventas por período"->{LocalDate i=LocalDate.parse(inicio.getText().trim()),f=LocalDate.parse(fin.getText().trim());if(f.isBefore(i))throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial.");tabla.setModel(servicio.reporteVentas(i,f));resumen.setModel(servicio.reporteTotalesVenta(i,f));}case "Productos más vendidos"->tabla.setModel(servicio.reporteMasVendidos());case "Productos agotados o con bajo inventario"->tabla.setModel(servicio.reporteBajoInventario());case "Puntos acumulados por cliente"->tabla.setModel(servicio.reportePuntosClientes());case "Historial de canjes"->tabla.setModel(servicio.reporteCanjes());case "Ventas de menús completos"->tabla.setModel(servicio.reporteMenusVendidos());}}catch(Exception e){Ui.error(this,e);}}
}
