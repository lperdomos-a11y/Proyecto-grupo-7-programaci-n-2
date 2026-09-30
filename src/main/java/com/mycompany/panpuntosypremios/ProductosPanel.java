package com.mycompany.panpuntosypremios;


import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductosPanel extends JPanel {
    private final ServicioRestaurante servicio;

    public ProductosPanel(ServicioRestaurante servicio) {
        this.servicio=servicio;
        setLayout(new BorderLayout());
        JTabbedPane tabs=new JTabbedPane();
        tabs.addTab("Productos",new ProductosTab());
        tabs.addTab("Menús completos",new MenusTab());
        add(tabs,BorderLayout.CENTER);
    }

    private class ProductosTab extends JPanel {
        private final JTextField codigo=new JTextField(10), nombre=new JTextField(18), precio=new JTextField(8), existencia=new JTextField(6);
        private final JComboBox<String> categoria=new JComboBox<>(new String[]{"SANDWICH","BEBIDA","ACOMPANAMIENTO"});
        private final JCheckBox activo=new JCheckBox("Activo",true);
        private final JTable tabla=new JTable();
        private boolean nuevo=true;

        ProductosTab(){
            setLayout(new BorderLayout(6,6));
            JPanel form=Ui.fila(new JLabel("Código"),codigo,new JLabel("Categoría"),categoria,new JLabel("Nombre"),nombre,new JLabel("Precio Q"),precio,new JLabel("Existencia inicial"),existencia,activo);
            JButton bNuevo=new JButton("Nuevo"),bGuardar=new JButton("Guardar"),bDesactivar=new JButton("Desactivar"),bRefrescar=new JButton("Refrescar");
            JPanel top=new JPanel(new BorderLayout());top.add(form,BorderLayout.CENTER);top.add(Ui.fila(bNuevo,bGuardar,bDesactivar,bRefrescar),BorderLayout.SOUTH);add(top,BorderLayout.NORTH);add(new JScrollPane(tabla),BorderLayout.CENTER);
            bNuevo.addActionListener(e->limpiar());bGuardar.addActionListener(e->guardar());bDesactivar.addActionListener(e->desactivar());bRefrescar.addActionListener(e->cargar());
            tabla.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())seleccionar();});
            cargar();
        }
        private void cargar(){try{tabla.setModel(servicio.tablaProductos());}catch(Exception e){Ui.error(this,e);}}
        private void limpiar(){nuevo=true;codigo.setEditable(true);codigo.setText("");nombre.setText("");precio.setText("");existencia.setText("0");existencia.setEditable(true);activo.setSelected(true);tabla.clearSelection();}
        private void seleccionar(){int r=tabla.getSelectedRow();if(r<0)return;nuevo=false;codigo.setText(String.valueOf(tabla.getValueAt(r,0)));codigo.setEditable(false);categoria.setSelectedItem(String.valueOf(tabla.getValueAt(r,1)));nombre.setText(String.valueOf(tabla.getValueAt(r,2)));precio.setText(String.valueOf(tabla.getValueAt(r,3)));existencia.setText(String.valueOf(tabla.getValueAt(r,4)));existencia.setEditable(false);activo.setSelected("SI".equals(String.valueOf(tabla.getValueAt(r,5))));}
        private void guardar(){try{BigDecimal p=new BigDecimal(precio.getText().trim());Integer ex=nuevo?Integer.valueOf(existencia.getText().trim()):null;servicio.guardarProducto(codigo.getText(),String.valueOf(categoria.getSelectedItem()),nombre.getText(),p,ex,activo.isSelected(),nuevo);Ui.info(this,"Producto guardado.");limpiar();cargar();}catch(Exception e){Ui.error(this,e);}}
        private void desactivar(){if(nuevo){Ui.info(this,"Seleccione un producto.");return;}try{servicio.desactivarProducto(codigo.getText());Ui.info(this,"Producto desactivado.");limpiar();cargar();}catch(Exception e){Ui.error(this,e);}}
    }

    private class MenusTab extends JPanel {
        private final JTextField codigo=new JTextField(8),nombre=new JTextField(18),precio=new JTextField(8);
        private final JCheckBox activo=new JCheckBox("Activo",true);
        private final JComboBox<ProductoIndividual> sandwich=new JComboBox<>(), bebida=new JComboBox<>(), acompanamiento=new JComboBox<>();
        private final JTable tabla=new JTable();
        private Long idMenu=null;
        private final Map<String,ProductoIndividual> mapa=new HashMap<>();

        MenusTab(){
            setLayout(new BorderLayout(6,6));
            JPanel f1=Ui.fila(new JLabel("Código"),codigo,new JLabel("Nombre"),nombre,new JLabel("Precio Q"),precio,activo);
            JPanel f2=Ui.fila(new JLabel("Sándwich"),sandwich,new JLabel("Bebida"),bebida,new JLabel("Acompañamiento"),acompanamiento);
            JButton bNuevo=new JButton("Nuevo"),bGuardar=new JButton("Guardar"),bDesactivar=new JButton("Desactivar"),bRefrescar=new JButton("Refrescar");
            JPanel top=new JPanel(new GridLayout(3,1));top.add(f1);top.add(f2);top.add(Ui.fila(bNuevo,bGuardar,bDesactivar,bRefrescar));add(top,BorderLayout.NORTH);add(new JScrollPane(tabla),BorderLayout.CENTER);
            bNuevo.addActionListener(e->limpiar());bGuardar.addActionListener(e->guardar());bDesactivar.addActionListener(e->desactivar());bRefrescar.addActionListener(e->{cargarCombos();cargar();});
            tabla.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())seleccionar();});
            cargarCombos();cargar();
        }
        private void cargarCombos(){try{sandwich.removeAllItems();bebida.removeAllItems();acompanamiento.removeAllItems();mapa.clear();for(ProductoIndividual p:servicio.listarProductos(true)){mapa.put(p.getCodigo(),p);switch(p.getCategoria()){case "SANDWICH"->sandwich.addItem(p);case "BEBIDA"->bebida.addItem(p);case "ACOMPANAMIENTO"->acompanamiento.addItem(p);}}}catch(Exception e){Ui.error(this,e);}}
        private void cargar(){try{tabla.setModel(servicio.tablaMenus());}catch(Exception e){Ui.error(this,e);}}
        private void limpiar(){idMenu=null;codigo.setEditable(true);codigo.setText("");nombre.setText("");precio.setText("");activo.setSelected(true);tabla.clearSelection();}
        private void seleccionar(){int r=tabla.getSelectedRow();if(r<0)return;try{idMenu=((Number)tabla.getValueAt(r,0)).longValue();codigo.setText(String.valueOf(tabla.getValueAt(r,1)));codigo.setEditable(false);nombre.setText(String.valueOf(tabla.getValueAt(r,2)));precio.setText(String.valueOf(tabla.getValueAt(r,3)));activo.setSelected("SI".equals(String.valueOf(tabla.getValueAt(r,4))));List<String> comps=servicio.componentesMenu(idMenu);for(String c:comps){ProductoIndividual p=mapa.get(c);if(p!=null){switch(p.getCategoria()){case "SANDWICH"->sandwich.setSelectedItem(p);case "BEBIDA"->bebida.setSelectedItem(p);case "ACOMPANAMIENTO"->acompanamiento.setSelectedItem(p);}}}}catch(Exception e){Ui.error(this,e);}}
        private void guardar(){try{ProductoIndividual s=(ProductoIndividual)sandwich.getSelectedItem(),b=(ProductoIndividual)bebida.getSelectedItem(),a=(ProductoIndividual)acompanamiento.getSelectedItem();if(s==null||b==null||a==null)throw new IllegalArgumentException("Debe tener al menos un producto activo de cada categoría.");idMenu=servicio.guardarMenu(idMenu,codigo.getText(),nombre.getText(),new BigDecimal(precio.getText().trim()),activo.isSelected(),s.getCodigo(),b.getCodigo(),a.getCodigo());Ui.info(this,"Menú guardado.");limpiar();cargar();}catch(Exception e){Ui.error(this,e);}}
        private void desactivar(){if(idMenu==null){Ui.info(this,"Seleccione un menú.");return;}try{servicio.desactivarMenu(idMenu);Ui.info(this,"Menú desactivado.");limpiar();cargar();}catch(Exception e){Ui.error(this,e);}}
    }
}
