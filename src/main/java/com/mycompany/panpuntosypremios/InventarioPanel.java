package com.mycompany.panpuntosypremios;


import javax.swing.*;
import java.awt.*;

public class InventarioPanel extends JPanel {
    private final ServicioRestaurante servicio;
    private final JComboBox<ProductoIndividual> producto=new JComboBox<>();
    private final JSpinner cantidad=new JSpinner(new SpinnerNumberModel(1,1,100000,1));
    private final JTable tabla=new JTable();

    public InventarioPanel(ServicioRestaurante servicio){
        this.servicio=servicio;setLayout(new BorderLayout(6,6));
        JButton abastecer=new JButton("Abastecer"),refrescar=new JButton("Refrescar");
        add(Ui.fila(new JLabel("Producto"),producto,new JLabel("Cantidad"),cantidad,abastecer,refrescar),BorderLayout.NORTH);add(new JScrollPane(tabla),BorderLayout.CENTER);
        abastecer.addActionListener(e->abastecer());refrescar.addActionListener(e->cargar());cargar();
    }
    private void cargar(){try{producto.removeAllItems();for(ProductoIndividual p:servicio.listarProductos(false))producto.addItem(p);tabla.setModel(servicio.historialInventario());}catch(Exception e){Ui.error(this,e);}}
    private void abastecer(){ProductoIndividual p=(ProductoIndividual)producto.getSelectedItem();if(p==null)return;try{servicio.abastecer(p.getCodigo(),(Integer)cantidad.getValue());Ui.info(this,"Inventario actualizado y movimiento registrado.");cargar();}catch(Exception e){Ui.error(this,e);}}
}
