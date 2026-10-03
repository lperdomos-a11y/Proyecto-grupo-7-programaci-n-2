package com.mycompany.panpuntosypremios;


import javax.swing.*;
import java.awt.*;

public class FidelizacionPanel extends JPanel {
    private final ServicioRestaurante servicio;
    private final JTextField buscar=new JTextField(12);
    private final JComboBox<Cliente> cliente=new JComboBox<>();
    private final JComboBox<Recompensa> recompensa=new JComboBox<>();
    private final JLabel saldo=new JLabel("Saldo: -");
    private final JTable historial=new JTable();

    public FidelizacionPanel(ServicioRestaurante servicio){
        this.servicio=servicio;setLayout(new BorderLayout(6,6));JButton bBuscar=new JButton("Buscar"),bCanjear=new JButton("Canjear"),bActualizar=new JButton("Actualizar");
        add(Ui.fila(new JLabel("Cliente"),buscar,bBuscar,cliente,saldo,new JLabel("Recompensa"),recompensa,bCanjear,bActualizar),BorderLayout.NORTH);add(new JScrollPane(historial),BorderLayout.CENTER);
        bBuscar.addActionListener(e->cargarClientes());buscar.addActionListener(e->cargarClientes());cliente.addActionListener(e->actualizar());bCanjear.addActionListener(e->canjear());bActualizar.addActionListener(e->{cargarClientes();cargarRecompensas();});cargarClientes();cargarRecompensas();
    }
    private void cargarClientes(){try{cliente.removeAllItems();for(Cliente c:servicio.listarClientes(buscar.getText(),true))cliente.addItem(c);actualizar();}catch(Exception e){Ui.error(this,e);}}
    private void cargarRecompensas(){try{recompensa.removeAllItems();for(Recompensa r:servicio.listarRecompensas())recompensa.addItem(r);}catch(Exception e){Ui.error(this,e);}}
    private void actualizar(){Cliente c=(Cliente)cliente.getSelectedItem();saldo.setText(c==null?"Saldo: -":"Saldo: "+c.getSaldoPuntos()+" puntos");if(c!=null)try{historial.setModel(servicio.historialPuntos(c.getId()));}catch(Exception e){Ui.error(this,e);}}
    private void canjear(){Cliente c=(Cliente)cliente.getSelectedItem();Recompensa r=(Recompensa)recompensa.getSelectedItem();if(c==null||r==null)return;int ok=JOptionPane.showConfirmDialog(this,"Canjear "+r.nombre()+" por "+r.puntosNecesarios()+" puntos?","Confirmar canje",JOptionPane.YES_NO_OPTION);if(ok!=JOptionPane.YES_OPTION)return;try{Ui.info(this,servicio.canjear(c.getId(),r.id()));cargarClientes();}catch(Exception e){Ui.error(this,e);}}
}
