package com.mycompany.panpuntosypremios;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ClientesPanel extends JPanel {
    private final ServicioRestaurante servicio;
    private final JTextField buscar = new JTextField(15);
    private final JTextField identificador = new JTextField(12);
    private final JTextField nombre = new JTextField(18);
    private final JTextField telefono = new JTextField(12);
    private final JTextField correo = new JTextField(18);
    private final JCheckBox activo = new JCheckBox("Activo", true);
    private final JTable tabla = new JTable();
    private List<Cliente> clientes;
    private Cliente seleccionado;

    public ClientesPanel(ServicioRestaurante servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout(8,8));
        JButton btnBuscar = new JButton("Buscar");
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnGuardar = new JButton("Guardar");
        JButton btnDesactivar = new JButton("Desactivar");
        JButton btnPedidos = new JButton("Historial pedidos");
        JButton btnPuntos = new JButton("Historial puntos");
        add(Ui.fila(new JLabel("Buscar:"),buscar,btnBuscar,btnNuevo,btnGuardar,btnDesactivar,btnPedidos,btnPuntos),BorderLayout.NORTH);
        JPanel form = new JPanel(new GridLayout(2,5,6,6));
        form.add(new JLabel("Identificador")); form.add(new JLabel("Nombre")); form.add(new JLabel("Teléfono")); form.add(new JLabel("Correo")); form.add(new JLabel("Estado"));
        form.add(identificador); form.add(nombre); form.add(telefono); form.add(correo); form.add(activo);
        JPanel center = new JPanel(new BorderLayout(5,5)); center.add(form,BorderLayout.NORTH); center.add(new JScrollPane(tabla),BorderLayout.CENTER); add(center,BorderLayout.CENTER);
        btnBuscar.addActionListener(e->cargar());
        buscar.addActionListener(e->cargar());
        btnNuevo.addActionListener(e->limpiar());
        btnGuardar.addActionListener(e->guardar());
        btnDesactivar.addActionListener(e->desactivar());
        btnPedidos.addActionListener(e->historialPedidos());
        btnPuntos.addActionListener(e->historialPuntos());
        tabla.getSelectionModel().addListSelectionListener(e->{ if(!e.getValueIsAdjusting()) seleccionar(); });
        cargar();
    }

    private void cargar() {
        try {
            clientes=servicio.listarClientes(buscar.getText(),false);
            DefaultTableModel m=new DefaultTableModel(new String[]{"ID","Identificador","Nombre","Teléfono","Correo","Puntos","Activo"},0){ public boolean isCellEditable(int r,int c){return false;} };
            for(Cliente c:clientes) m.addRow(new Object[]{c.getId(),c.getIdentificador(),c.getNombre(),c.getTelefono(),c.getCorreo(),c.getSaldoPuntos(),c.isActivo()?"Sí":"No"});
            tabla.setModel(m);
        } catch(Exception e){ Ui.error(this,e); }
    }

    private void seleccionar() {
        int r=tabla.getSelectedRow(); if(r<0||clientes==null||r>=clientes.size()) return;
        seleccionado=clientes.get(r); identificador.setText(seleccionado.getIdentificador()); identificador.setEditable(false); nombre.setText(seleccionado.getNombre()); telefono.setText(seleccionado.getTelefono()); correo.setText(seleccionado.getCorreo()); activo.setSelected(seleccionado.isActivo());
    }

    private void limpiar(){ seleccionado=null; identificador.setEditable(true); identificador.setText(""); nombre.setText(""); telefono.setText(""); correo.setText(""); activo.setSelected(true); tabla.clearSelection(); }
    private void guardar(){ try { if(seleccionado==null) servicio.registrarCliente(identificador.getText(),nombre.getText(),telefono.getText(),correo.getText()); else servicio.modificarCliente(seleccionado.getId(),nombre.getText(),telefono.getText(),correo.getText(),activo.isSelected()); Ui.info(this,"Cliente guardado."); limpiar(); cargar(); } catch(Exception e){Ui.error(this,e);} }
    private void desactivar(){ if(seleccionado==null){Ui.info(this,"Seleccione un cliente.");return;} try{servicio.desactivarCliente(seleccionado.getId());Ui.info(this,"Cliente desactivado sin eliminar su historial.");limpiar();cargar();}catch(Exception e){Ui.error(this,e);} }
    private void historialPedidos(){ if(seleccionado==null){Ui.info(this,"Seleccione un cliente.");return;} try{Ui.tablaDialog(this,"Pedidos de "+seleccionado.getNombre(),servicio.historialPedidosCliente(seleccionado.getId()));}catch(Exception e){Ui.error(this,e);} }
    private void historialPuntos(){ if(seleccionado==null){Ui.info(this,"Seleccione un cliente.");return;} try{Ui.tablaDialog(this,"Movimientos de puntos de "+seleccionado.getNombre(),servicio.historialPuntos(seleccionado.getId()));}catch(Exception e){Ui.error(this,e);} }
}
