package com.mycompany.panpuntosypremios;


import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class CobroPanel extends JPanel {
    private final ServicioRestaurante servicio;
    private final JComboBox<PedidoResumen> pendientes=new JComboBox<>();
    private final JComboBox<String> metodo=new JComboBox<>(new String[]{"EFECTIVO","TARJETA"});
    private final JTable detalle=new JTable();

    public CobroPanel(ServicioRestaurante servicio){
        this.servicio=servicio;setLayout(new BorderLayout(6,6));JButton refrescar=new JButton("Refrescar"),cobrar=new JButton("Cobrar"),anular=new JButton("Anular PENDIENTE"),entregar=new JButton("Marcar PAGADO como ENTREGADO");
        add(Ui.fila(new JLabel("Pedido pendiente"),pendientes,refrescar,new JLabel("Método"),metodo,cobrar,anular,entregar),BorderLayout.NORTH);add(new JScrollPane(detalle),BorderLayout.CENTER);
        pendientes.addActionListener(e->detalle());refrescar.addActionListener(e->cargar());cobrar.addActionListener(e->cobrar());anular.addActionListener(e->anular());entregar.addActionListener(e->entregar());cargar();
    }
    private void cargar(){try{pendientes.removeAllItems();for(PedidoResumen p:servicio.listarPedidosPorEstado("PENDIENTE"))pendientes.addItem(p);detalle();}catch(Exception e){Ui.error(this,e);}}
    private void detalle(){PedidoResumen p=(PedidoResumen)pendientes.getSelectedItem();if(p==null)return;try{detalle.setModel(servicio.detallePedido(p.id()));}catch(Exception e){Ui.error(this,e);}}
    private void cobrar(){PedidoResumen p=(PedidoResumen)pendientes.getSelectedItem();if(p==null){Ui.info(this,"No hay pedido pendiente seleccionado.");return;}try{String met=String.valueOf(metodo.getSelectedItem());BigDecimal recibido=null;String ref=null;boolean aprobado=true;if("EFECTIVO".equals(met)){String s=JOptionPane.showInputDialog(this,"Total Q"+p.total()+". Dinero recibido:");if(s==null)return;recibido=new BigDecimal(s.trim());}else{ref=JOptionPane.showInputDialog(this,"Referencia de la transacción:");if(ref==null)return;aprobado=JOptionPane.showConfirmDialog(this,"¿Tarjeta aprobada?","Simulación",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}String comp=servicio.pagarPedido(p.id(),met,recibido,ref,aprobado);Ui.info(this,comp);cargar();}catch(Exception e){Ui.error(this,e);}}
    private void anular(){PedidoResumen p=(PedidoResumen)pendientes.getSelectedItem();if(p==null)return;try{servicio.anularPedido(p.id());Ui.info(this,"Pedido anulado sin afectar inventario ni puntos.");cargar();}catch(Exception e){Ui.error(this,e);}}
    private void entregar(){try{java.util.List<PedidoResumen> pagados=servicio.listarPedidosPorEstado("PAGADO");if(pagados.isEmpty()){Ui.info(this,"No hay pedidos PAGADOS pendientes de entrega.");return;}Object sel=JOptionPane.showInputDialog(this,"Seleccione el pedido","Entregar",JOptionPane.QUESTION_MESSAGE,null,pagados.toArray(),pagados.get(0));if(sel instanceof PedidoResumen p){servicio.entregarPedido(p.id());Ui.info(this,"Pedido #"+p.id()+" marcado como ENTREGADO.");}}catch(Exception e){Ui.error(this,e);}}
}
