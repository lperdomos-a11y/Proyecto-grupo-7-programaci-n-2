package com.mycompany.panpuntosypremios;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PedidosPanel extends JPanel {
    private final ServicioRestaurante servicio;
    private final JTextField buscarCliente=new JTextField(12);
    private final JComboBox<Cliente> cliente=new JComboBox<>();
    private final JLabel saldo=new JLabel("Saldo: -");
    private final JComboBox<ItemVenta> catalogo=new JComboBox<>();
    private final JSpinner cantidad=new JSpinner(new SpinnerNumberModel(1,1,1000,1));
    private final JTable tabla=new JTable();
    private final JLabel subtotal=new JLabel("Subtotal: Q0.00"),total=new JLabel("Total: Q0.00"),puntos=new JLabel("Puntos: 0");
    private final JComboBox<String> metodo=new JComboBox<>(new String[]{"EFECTIVO","TARJETA"});
    private final List<ItemVenta> carrito=new ArrayList<>();

    public PedidosPanel(ServicioRestaurante servicio){
        this.servicio=servicio;setLayout(new BorderLayout(6,6));
        JButton bBuscar=new JButton("Buscar cliente"),bRecargar=new JButton("Recargar catálogo"),bAgregar=new JButton("Agregar"),bEliminar=new JButton("Eliminar seleccionado"),bVaciar=new JButton("Vaciar"),bCrear=new JButton("Crear PENDIENTE"),bCrearCobrar=new JButton("Crear y cobrar");
        JPanel p1=Ui.fila(new JLabel("Cliente"),buscarCliente,bBuscar,cliente,saldo);
        JPanel p2=Ui.fila(new JLabel("Producto / menú"),catalogo,new JLabel("Cantidad"),cantidad,bAgregar,bEliminar,bVaciar,bRecargar);
        JPanel p3=Ui.fila(subtotal,total,puntos,new JLabel("Método de pago"),metodo,bCrear,bCrearCobrar);
        JPanel top=new JPanel(new GridLayout(3,1));top.add(p1);top.add(p2);top.add(p3);add(top,BorderLayout.NORTH);add(new JScrollPane(tabla),BorderLayout.CENTER);
        bBuscar.addActionListener(e->cargarClientes());buscarCliente.addActionListener(e->cargarClientes());cliente.addActionListener(e->actualizarSaldo());bRecargar.addActionListener(e->cargarCatalogo());bAgregar.addActionListener(e->agregar());bEliminar.addActionListener(e->eliminar());bVaciar.addActionListener(e->{carrito.clear();actualizarTabla();});bCrear.addActionListener(e->crear(false));bCrearCobrar.addActionListener(e->crear(true));
        cargarClientes();cargarCatalogo();actualizarTabla();
    }

    private void cargarClientes(){try{cliente.removeAllItems();for(Cliente c:servicio.listarClientes(buscarCliente.getText(),true))cliente.addItem(c);actualizarSaldo();}catch(Exception e){Ui.error(this,e);}}
    private void actualizarSaldo(){Cliente c=(Cliente)cliente.getSelectedItem();saldo.setText(c==null?"Saldo: -":"Saldo: "+c.getSaldoPuntos()+" puntos");}
    private void cargarCatalogo(){try{catalogo.removeAllItems();for(ProductoIndividual p:servicio.listarProductos(true))catalogo.addItem(p);for(MenuCompleto m:servicio.listarMenus(true))catalogo.addItem(m);}catch(Exception e){Ui.error(this,e);}}
    private void agregar(){ItemVenta sel=(ItemVenta)catalogo.getSelectedItem();if(sel==null)return;int q=(Integer)cantidad.getValue();ItemVenta existente=null;for(ItemVenta i:carrito){if(i.getTipo().equals(sel.getTipo())&&i.getCodigo().equals(sel.getCodigo())){existente=i;break;}}if(existente!=null)existente.setCantidad(existente.getCantidad()+q);else if(sel instanceof ProductoIndividual p)carrito.add(new ProductoIndividual(p.getCodigo(),p.getNombre(),p.getPrecio(),q,p.getCategoria()));else if(sel instanceof MenuCompleto m)carrito.add(new MenuCompleto(m.getIdMenu(),m.getCodigo(),m.getNombre(),m.getPrecio(),q));actualizarTabla();}
    private void eliminar(){int r=tabla.getSelectedRow();if(r>=0&&r<carrito.size()){carrito.remove(r);actualizarTabla();}}
    private void actualizarTabla(){DefaultTableModel m=new DefaultTableModel(new String[]{"Tipo","Código","Descripción","Cantidad","Precio","Subtotal","Puntos"},0){public boolean isCellEditable(int r,int c){return false;}};BigDecimal t=BigDecimal.ZERO;int pts=0;for(ItemVenta i:carrito){m.addRow(new Object[]{i.getTipo(),i.getCodigo(),i.getNombre(),i.getCantidad(),i.getPrecio(),i.getSubtotal(),i.calcularPuntos()});t=t.add(i.getSubtotal());pts+=i.calcularPuntos();}tabla.setModel(m);subtotal.setText("Subtotal: Q"+t.setScale(2));total.setText("Total: Q"+t.setScale(2));puntos.setText("Puntos: "+pts);}
    private void crear(boolean cobrar){Cliente c=(Cliente)cliente.getSelectedItem();if(c==null){Ui.info(this,"Seleccione un cliente.");return;}try{long id=servicio.crearPedido(c.getId(),new ArrayList<>(carrito));if(!cobrar){Ui.info(this,"Pedido #"+id+" creado como PENDIENTE. El inventario y los puntos se modificarán únicamente al confirmar el pago.");carrito.clear();actualizarTabla();return;}String met=String.valueOf(metodo.getSelectedItem());BigDecimal recibido=null;String referencia=null;boolean aprobado=true;if("EFECTIVO".equals(met)){String s=JOptionPane.showInputDialog(this,"Dinero recibido:","0.00");if(s==null)return;recibido=new BigDecimal(s.trim());}else{referencia=JOptionPane.showInputDialog(this,"Número de referencia de la tarjeta:");if(referencia==null)return;aprobado=JOptionPane.showConfirmDialog(this,"¿La operación fue aprobada?","Simulación de tarjeta",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}String comp=servicio.pagarPedido(id,met,recibido,referencia,aprobado);JTextArea area=new JTextArea(comp);area.setEditable(false);JOptionPane.showMessageDialog(this,new JScrollPane(area),"Comprobante de venta",JOptionPane.INFORMATION_MESSAGE);carrito.clear();actualizarTabla();cargarClientes();cargarCatalogo();}catch(Exception e){Ui.error(this,e);}}
}
