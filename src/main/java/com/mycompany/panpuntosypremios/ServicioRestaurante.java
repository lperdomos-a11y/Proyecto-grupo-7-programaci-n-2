package com.mycompany.panpuntosypremios;


import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServicioRestaurante {
    public void probarConexion() throws SQLException {
        try (Connection c = ConexionDB.conectar()) {
            if (!c.isValid(3)) throw new SQLException("No fue posible validar la conexión con Oracle.");
        }
    }

    public List<Cliente> listarClientes(String filtro, boolean soloActivos) throws SQLException {
        String sql = "SELECT id_cliente,identificador,nombre,telefono,correo,saldo_puntos,activo FROM clientes WHERE (UPPER(identificador) LIKE ? OR UPPER(nombre) LIKE ?)" + (soloActivos ? " AND activo=1" : "") + " ORDER BY nombre";
        List<Cliente> r = new ArrayList<>();
        String f = "%" + (filtro == null ? "" : filtro.trim().toUpperCase()) + "%";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, f);
            ps.setString(2, f);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) r.add(new Cliente(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6), rs.getInt(7) == 1));
            }
        }
        return r;
    }

    public void registrarCliente(String identificador, String nombre, String telefono, String correo) throws SQLException {
        if (identificador == null || identificador.isBlank() || nombre == null || nombre.isBlank()) throw new SQLException("Identificador y nombre son obligatorios.");
        String sql = "INSERT INTO clientes(identificador,nombre,telefono,correo,saldo_puntos,activo) VALUES(?,?,?,?,0,1)";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, identificador.trim());
            ps.setString(2, nombre.trim());
            ps.setString(3, vacioANull(telefono));
            ps.setString(4, vacioANull(correo));
            ps.executeUpdate();
        }
    }

    public void modificarCliente(long id, String nombre, String telefono, String correo, boolean activo) throws SQLException {
        if (nombre == null || nombre.isBlank()) throw new SQLException("El nombre es obligatorio.");
        String sql = "UPDATE clientes SET nombre=?,telefono=?,correo=?,activo=? WHERE id_cliente=?";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setString(2, vacioANull(telefono));
            ps.setString(3, vacioANull(correo));
            ps.setInt(4, activo ? 1 : 0);
            ps.setLong(5, id);
            if (ps.executeUpdate() == 0) throw new SQLException("Cliente no encontrado.");
        }
    }

    public void desactivarCliente(long id) throws SQLException {
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement("UPDATE clientes SET activo=0 WHERE id_cliente=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public List<ProductoIndividual> listarProductos(boolean soloActivos) throws SQLException {
        String sql = "SELECT codigo,nombre,precio,categoria FROM productos" + (soloActivos ? " WHERE activo=1" : "") + " ORDER BY categoria,nombre";
        List<ProductoIndividual> r = new ArrayList<>();
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) r.add(new ProductoIndividual(rs.getString("codigo"), rs.getString("nombre"), rs.getBigDecimal("precio"), 1, rs.getString("categoria")));
        }
        return r;
    }

    public DefaultTableModel tablaProductos() throws SQLException {
        return consultaTabla("SELECT codigo,categoria,nombre,precio,existencia,CASE activo WHEN 1 THEN 'SI' ELSE 'NO' END activo FROM productos ORDER BY categoria,nombre");
    }

    public void guardarProducto(String codigo, String categoria, String nombre, BigDecimal precio, Integer existenciaInicial, boolean activo, boolean nuevo) throws SQLException {
        if (codigo == null || codigo.isBlank() || nombre == null || nombre.isBlank()) throw new SQLException("Código y nombre son obligatorios.");
        if (precio == null || precio.signum() < 0) throw new SQLException("El precio no puede ser negativo.");
        if (nuevo) {
            if (existenciaInicial == null || existenciaInicial < 0) throw new SQLException("La existencia no puede ser negativa.");
            String sql = "INSERT INTO productos(codigo,categoria,nombre,precio,existencia,activo) VALUES(?,?,?,?,?,?)";
            try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, codigo.trim()); ps.setString(2, categoria); ps.setString(3, nombre.trim()); ps.setBigDecimal(4, precio); ps.setInt(5, existenciaInicial); ps.setInt(6, activo ? 1 : 0); ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE productos SET categoria=?,nombre=?,precio=?,activo=? WHERE codigo=?";
            try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, categoria); ps.setString(2, nombre.trim()); ps.setBigDecimal(3, precio); ps.setInt(4, activo ? 1 : 0); ps.setString(5, codigo.trim());
                if (ps.executeUpdate() == 0) throw new SQLException("Producto no encontrado.");
            }
        }
    }

    public void desactivarProducto(String codigo) throws SQLException {
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement("UPDATE productos SET activo=0 WHERE codigo=?")) {
            ps.setString(1, codigo); ps.executeUpdate();
        }
    }

    public void abastecer(String codigo, int cantidad) throws SQLException {
        if (cantidad <= 0) throw new SQLException("La cantidad de abastecimiento debe ser positiva.");
        try (Connection c = ConexionDB.conectar()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("UPDATE productos SET existencia=existencia+? WHERE codigo=?")) {
                    ps.setInt(1, cantidad); ps.setString(2, codigo);
                    if (ps.executeUpdate() == 0) throw new SQLException("Producto no encontrado.");
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO movimientos_inventario(producto_codigo,cantidad,tipo,referencia) VALUES(?,?,'ABASTECIMIENTO','Ingreso manual')")) {
                    ps.setString(1, codigo); ps.setInt(2, cantidad); ps.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            } finally { c.setAutoCommit(true); }
        }
    }

    public DefaultTableModel historialInventario() throws SQLException {
        return consultaTabla("SELECT m.fecha,m.producto_codigo,p.nombre,m.cantidad,m.tipo,m.referencia FROM movimientos_inventario m JOIN productos p ON p.codigo=m.producto_codigo ORDER BY m.fecha DESC");
    }

    public List<MenuCompleto> listarMenus(boolean soloActivos) throws SQLException {
        String sql = "SELECT id_menu,codigo,nombre,precio FROM menus" + (soloActivos ? " WHERE activo=1" : "") + " ORDER BY nombre";
        List<MenuCompleto> r = new ArrayList<>();
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) r.add(new MenuCompleto(rs.getLong("id_menu"), rs.getString("codigo"), rs.getString("nombre"), rs.getBigDecimal("precio"), 1));
        }
        return r;
    }

    public DefaultTableModel tablaMenus() throws SQLException {
        String sql = "SELECT m.id_menu,m.codigo,m.nombre,m.precio,CASE m.activo WHEN 1 THEN 'SI' ELSE 'NO' END activo," +
                "LISTAGG(p.nombre, ' + ') WITHIN GROUP (ORDER BY p.categoria) componentes " +
                "FROM menus m LEFT JOIN menu_componentes mc ON mc.id_menu=m.id_menu LEFT JOIN productos p ON p.codigo=mc.producto_codigo " +
                "GROUP BY m.id_menu,m.codigo,m.nombre,m.precio,m.activo ORDER BY m.nombre";
        return consultaTabla(sql);
    }

    public long guardarMenu(Long idMenu, String codigo, String nombre, BigDecimal precio, boolean activo, String sandwich, String bebida, String acompanamiento) throws SQLException {
        if (codigo == null || codigo.isBlank() || nombre == null || nombre.isBlank()) throw new SQLException("Código y nombre del menú son obligatorios.");
        if (precio == null || precio.signum() < 0) throw new SQLException("El precio del menú no puede ser negativo.");
        if (sandwich == null || bebida == null || acompanamiento == null) throw new SQLException("El menú debe tener sándwich, bebida y acompañamiento.");
        try (Connection c = ConexionDB.conectar()) {
            c.setAutoCommit(false);
            try {
                long id;
                if (idMenu == null) {
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO menus(codigo,nombre,precio,activo) VALUES(?,?,?,?)", new String[]{"id_menu"})) {
                        ps.setString(1, codigo.trim()); ps.setString(2, nombre.trim()); ps.setBigDecimal(3, precio); ps.setInt(4, activo ? 1 : 0); ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) { if (!rs.next()) throw new SQLException("No se obtuvo el ID del menú."); id = rs.getLong(1); }
                    }
                } else {
                    id = idMenu;
                    try (PreparedStatement ps = c.prepareStatement("UPDATE menus SET codigo=?,nombre=?,precio=?,activo=? WHERE id_menu=?")) {
                        ps.setString(1, codigo.trim()); ps.setString(2, nombre.trim()); ps.setBigDecimal(3, precio); ps.setInt(4, activo ? 1 : 0); ps.setLong(5, id); ps.executeUpdate();
                    }
                    try (PreparedStatement ps = c.prepareStatement("DELETE FROM menu_componentes WHERE id_menu=?")) { ps.setLong(1, id); ps.executeUpdate(); }
                }
                insertarComponente(c,id,sandwich);
                insertarComponente(c,id,bebida);
                insertarComponente(c,id,acompanamiento);
                c.commit();
                return id;
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            } finally { c.setAutoCommit(true); }
        }
    }

    private void insertarComponente(Connection c, long idMenu, String codigo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO menu_componentes(id_menu,producto_codigo,cantidad) VALUES(?,?,1)")) {
            ps.setLong(1,idMenu); ps.setString(2,codigo); ps.executeUpdate();
        }
    }

    public List<String> componentesMenu(long idMenu) throws SQLException {
        List<String> r = new ArrayList<>();
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement("SELECT producto_codigo FROM menu_componentes WHERE id_menu=? ORDER BY producto_codigo")) {
            ps.setLong(1,idMenu);
            try (ResultSet rs=ps.executeQuery()) { while(rs.next()) r.add(rs.getString(1)); }
        }
        return r;
    }

    public void desactivarMenu(long idMenu) throws SQLException {
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement("UPDATE menus SET activo=0 WHERE id_menu=?")) { ps.setLong(1,idMenu); ps.executeUpdate(); }
    }

    public long crearPedido(long idCliente, List<ItemVenta> items) throws SQLException {
        if (items == null || items.isEmpty()) throw new SQLException("Debe agregar al menos un producto al pedido.");
        BigDecimal total = items.stream().map(ItemVenta::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        int puntos = items.stream().mapToInt(ItemVenta::calcularPuntos).sum();
        try (Connection c = ConexionDB.conectar()) {
            c.setAutoCommit(false);
            try {
                validarClienteActivo(c,idCliente);
                validarDisponibilidadPedido(c,items,false);
                long idPedido;
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO pedidos(id_cliente,total,puntos_generados,estado) VALUES(?,?,?,'PENDIENTE')", new String[]{"id_pedido"})) {
                    ps.setLong(1,idCliente); ps.setBigDecimal(2,total); ps.setInt(3,puntos); ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { if (!rs.next()) throw new SQLException("No se obtuvo el número de pedido."); idPedido=rs.getLong(1); }
                }
                String sql = "INSERT INTO detalle_pedido(id_pedido,tipo_item,producto_codigo,id_menu,descripcion,cantidad,precio_unitario,subtotal,puntos) VALUES(?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    for (ItemVenta item : items) {
                        ps.setLong(1,idPedido); ps.setString(2,item.getTipo());
                        if (item instanceof ProductoIndividual) { ps.setString(3,item.getCodigo()); ps.setNull(4,Types.NUMERIC); }
                        else { ps.setNull(3,Types.VARCHAR); ps.setLong(4,((MenuCompleto)item).getIdMenu()); }
                        ps.setString(5,item.getNombre()); ps.setInt(6,item.getCantidad()); ps.setBigDecimal(7,item.getPrecio()); ps.setBigDecimal(8,item.getSubtotal()); ps.setInt(9,item.calcularPuntos()); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return idPedido;
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            } finally { c.setAutoCommit(true); }
        }
    }

    public List<PedidoResumen> listarPedidosPorEstado(String estado) throws SQLException {
        String sql = "SELECT p.id_pedido,c.nombre,p.fecha,p.estado,p.total,p.puntos_generados FROM pedidos p JOIN clientes c ON c.id_cliente=p.id_cliente WHERE p.estado=? ORDER BY p.fecha DESC";
        List<PedidoResumen> r = new ArrayList<>();
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,estado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) r.add(new PedidoResumen(rs.getLong(1),rs.getString(2),rs.getTimestamp(3).toLocalDateTime(),rs.getString(4),rs.getBigDecimal(5),rs.getInt(6)));
            }
        }
        return r;
    }

    public DefaultTableModel detallePedido(long idPedido) throws SQLException {
        return consultaTabla("SELECT tipo_item,descripcion,cantidad,precio_unitario,subtotal,puntos FROM detalle_pedido WHERE id_pedido=? ORDER BY id_detalle", idPedido);
    }

    public String pagarPedido(long idPedido, String metodo, BigDecimal recibido, String referencia, boolean tarjetaAprobada) throws SQLException {
        try (Connection c = ConexionDB.conectar()) {
            c.setAutoCommit(false);
            try {
                long idCliente;
                BigDecimal total;
                int puntos;
                try (PreparedStatement ps = c.prepareStatement("SELECT id_cliente,total,puntos_generados,estado FROM pedidos WHERE id_pedido=? FOR UPDATE")) {
                    ps.setLong(1,idPedido);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Pedido no encontrado.");
                        if (!"PENDIENTE".equals(rs.getString("estado"))) throw new SQLException("Solo se pueden cobrar pedidos PENDIENTES.");
                        idCliente=rs.getLong("id_cliente"); total=rs.getBigDecimal("total"); puntos=rs.getInt("puntos_generados");
                    }
                }
                if ("TARJETA".equals(metodo) && !tarjetaAprobada) throw new SQLException("La operación con tarjeta fue rechazada. No se modificó inventario ni puntos.");
                BigDecimal cambio = null;
                if ("EFECTIVO".equals(metodo)) {
                    if (recibido == null || recibido.compareTo(total) < 0) throw new SQLException("El dinero recibido es insuficiente.");
                    cambio = recibido.subtract(total).setScale(2,RoundingMode.HALF_UP);
                } else if ("TARJETA".equals(metodo)) {
                    if (referencia == null || referencia.isBlank()) throw new SQLException("Debe ingresar la referencia de la tarjeta.");
                } else throw new SQLException("Método de pago inválido.");

                List<ItemVenta> items = cargarItemsPedido(c,idPedido);
                validarDisponibilidadPedido(c,items,true);
                for (ItemVenta item : items) descontarInventario(c,item,"VENTA","Pedido #"+idPedido);

                try (PreparedStatement ps = c.prepareStatement("INSERT INTO pagos(id_pedido,metodo,monto,dinero_recibido,cambio,referencia,aprobado) VALUES(?,?,?,?,?,?,1)")) {
                    ps.setLong(1,idPedido); ps.setString(2,metodo); ps.setBigDecimal(3,total);
                    if (recibido == null) ps.setNull(4,Types.NUMERIC); else ps.setBigDecimal(4,recibido);
                    if (cambio == null) ps.setNull(5,Types.NUMERIC); else ps.setBigDecimal(5,cambio);
                    ps.setString(6,vacioANull(referencia)); ps.executeUpdate();
                }
                if (puntos > 0) {
                    try (PreparedStatement ps = c.prepareStatement("UPDATE clientes SET saldo_puntos=saldo_puntos+? WHERE id_cliente=?")) { ps.setInt(1,puntos); ps.setLong(2,idCliente); ps.executeUpdate(); }
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO movimientos_puntos(id_cliente,tipo,referencia,puntos) VALUES(?,'ACUMULACION',?,?)")) { ps.setLong(1,idCliente); ps.setString(2,"Pedido #"+idPedido); ps.setInt(3,puntos); ps.executeUpdate(); }
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE pedidos SET estado='PAGADO' WHERE id_pedido=?")) { ps.setLong(1,idPedido); ps.executeUpdate(); }
                c.commit();
                return "Pedido #"+idPedido+" PAGADO\nTotal: Q"+total+"\nPuntos acreditados: "+puntos+(cambio==null?"":"\nCambio: Q"+cambio);
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            } finally { c.setAutoCommit(true); }
        }
    }

    public void anularPedido(long idPedido) throws SQLException {
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement("UPDATE pedidos SET estado='ANULADO' WHERE id_pedido=? AND estado='PENDIENTE'")) {
            ps.setLong(1,idPedido); if (ps.executeUpdate()==0) throw new SQLException("Solo puede anularse un pedido PENDIENTE.");
        }
    }

    public void entregarPedido(long idPedido) throws SQLException {
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement("UPDATE pedidos SET estado='ENTREGADO' WHERE id_pedido=? AND estado='PAGADO'")) {
            ps.setLong(1,idPedido); if (ps.executeUpdate()==0) throw new SQLException("Solo puede entregarse un pedido PAGADO.");
        }
    }

    public List<Recompensa> listarRecompensas() throws SQLException {
        List<Recompensa> r = new ArrayList<>();
        try (Connection c=ConexionDB.conectar(); PreparedStatement ps=c.prepareStatement("SELECT id_recompensa,nombre,puntos_necesarios,tipo_objetivo,producto_codigo,id_menu FROM recompensas WHERE activo=1 ORDER BY puntos_necesarios"); ResultSet rs=ps.executeQuery()) {
            while(rs.next()) {
                long menu=rs.getLong("id_menu");
                boolean menuNulo=rs.wasNull();
                String producto=rs.getString("producto_codigo");
                r.add(new Recompensa(rs.getLong("id_recompensa"),rs.getString("nombre"),rs.getInt("puntos_necesarios"),rs.getString("tipo_objetivo"),producto,menuNulo?null:menu));
            }
        }
        return r;
    }

    public String canjear(long idCliente, long idRecompensa) throws SQLException {
        try (Connection c=ConexionDB.conectar()) {
            c.setAutoCommit(false);
            try {
                validarClienteActivo(c,idCliente);
                int saldo;
                try(PreparedStatement ps=c.prepareStatement("SELECT saldo_puntos FROM clientes WHERE id_cliente=? FOR UPDATE")){ ps.setLong(1,idCliente); try(ResultSet rs=ps.executeQuery()){ if(!rs.next()) throw new SQLException("Cliente no encontrado."); saldo=rs.getInt(1); }}
                Recompensa rec;
                try(PreparedStatement ps=c.prepareStatement("SELECT id_recompensa,nombre,puntos_necesarios,tipo_objetivo,producto_codigo,id_menu FROM recompensas WHERE id_recompensa=? AND activo=1")){ ps.setLong(1,idRecompensa); try(ResultSet rs=ps.executeQuery()){ if(!rs.next()) throw new SQLException("Recompensa no disponible."); long m=rs.getLong("id_menu"); boolean menuNulo=rs.wasNull(); String producto=rs.getString("producto_codigo"); rec=new Recompensa(rs.getLong(1),rs.getString(2),rs.getInt(3),rs.getString(4),producto,menuNulo?null:m); }}
                if(saldo<rec.puntosNecesarios()) throw new SQLException("Puntos insuficientes. Saldo actual: "+saldo);
                ItemVenta item;
                if("PRODUCTO".equals(rec.tipoObjetivo())) {
                    try(PreparedStatement ps=c.prepareStatement("SELECT codigo,nombre,precio,categoria FROM productos WHERE codigo=? AND activo=1")){ ps.setString(1,rec.productoCodigo()); try(ResultSet rs=ps.executeQuery()){ if(!rs.next()) throw new SQLException("Producto de recompensa no disponible."); item=new ProductoIndividual(rs.getString(1),rs.getString(2),rs.getBigDecimal(3),1,rs.getString(4)); }}
                } else {
                    try(PreparedStatement ps=c.prepareStatement("SELECT id_menu,codigo,nombre,precio FROM menus WHERE id_menu=? AND activo=1")){ ps.setLong(1,rec.idMenu()); try(ResultSet rs=ps.executeQuery()){ if(!rs.next()) throw new SQLException("Menú de recompensa no disponible."); item=new MenuCompleto(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getBigDecimal(4),1); }}
                }
                validarDisponibilidad(c,item,true);
                descontarInventario(c,item,"CANJE","Recompensa #"+rec.id());
                try(PreparedStatement ps=c.prepareStatement("UPDATE clientes SET saldo_puntos=saldo_puntos-? WHERE id_cliente=?")){ ps.setInt(1,rec.puntosNecesarios()); ps.setLong(2,idCliente); ps.executeUpdate(); }
                try(PreparedStatement ps=c.prepareStatement("INSERT INTO movimientos_puntos(id_cliente,tipo,referencia,puntos) VALUES(?,'CANJE',?,?)")){ ps.setLong(1,idCliente); ps.setString(2,"Recompensa #"+rec.id()); ps.setInt(3,-rec.puntosNecesarios()); ps.executeUpdate(); }
                try(PreparedStatement ps=c.prepareStatement("INSERT INTO canjes(id_cliente,id_recompensa,puntos_utilizados) VALUES(?,?,?)")){ ps.setLong(1,idCliente); ps.setLong(2,idRecompensa); ps.setInt(3,rec.puntosNecesarios()); ps.executeUpdate(); }
                c.commit();
                return "Canje realizado: "+rec.nombre()+"\nPuntos utilizados: "+rec.puntosNecesarios()+"\nNuevo saldo: "+(saldo-rec.puntosNecesarios());
            } catch(Exception e){ c.rollback(); if(e instanceof SQLException se) throw se; throw new SQLException(e); } finally { c.setAutoCommit(true); }
        }
    }

    public DefaultTableModel historialPuntos(long idCliente) throws SQLException {
        return consultaTabla("SELECT fecha,tipo,referencia,puntos FROM movimientos_puntos WHERE id_cliente=? ORDER BY fecha DESC",idCliente);
    }

    public DefaultTableModel historialPedidosCliente(long idCliente) throws SQLException {
        return consultaTabla("SELECT id_pedido,fecha,estado,total,puntos_generados FROM pedidos WHERE id_cliente=? ORDER BY fecha DESC",idCliente);
    }

    public DefaultTableModel reporteVentas(LocalDate inicio, LocalDate fin) throws SQLException {
        String sql="SELECT p.id_pedido,c.nombre cliente,p.fecha,p.total,pg.metodo FROM pedidos p JOIN clientes c ON c.id_cliente=p.id_cliente JOIN pagos pg ON pg.id_pedido=p.id_pedido WHERE p.estado IN ('PAGADO','ENTREGADO') AND p.fecha>=? AND p.fecha<? ORDER BY p.fecha";
        return consultaTabla(sql,Timestamp.valueOf(inicio.atStartOfDay()),Timestamp.valueOf(fin.plusDays(1).atStartOfDay()));
    }

    public DefaultTableModel reporteTotalesVenta(LocalDate inicio, LocalDate fin) throws SQLException {
        String sql="SELECT NVL(SUM(p.total),0) total_vendido, NVL(SUM(CASE WHEN pg.metodo='EFECTIVO' THEN p.total ELSE 0 END),0) efectivo, NVL(SUM(CASE WHEN pg.metodo='TARJETA' THEN p.total ELSE 0 END),0) tarjeta FROM pedidos p JOIN pagos pg ON pg.id_pedido=p.id_pedido WHERE p.estado IN ('PAGADO','ENTREGADO') AND p.fecha>=? AND p.fecha<?";
        return consultaTabla(sql,Timestamp.valueOf(inicio.atStartOfDay()),Timestamp.valueOf(fin.plusDays(1).atStartOfDay()));
    }

    public DefaultTableModel reporteMasVendidos() throws SQLException {
        String sql="SELECT origen,producto,SUM(unidades) unidades FROM ("+
                "SELECT 'INDIVIDUAL' origen,pr.nombre producto,d.cantidad unidades FROM detalle_pedido d JOIN productos pr ON pr.codigo=d.producto_codigo JOIN pedidos p ON p.id_pedido=d.id_pedido WHERE d.tipo_item='PRODUCTO' AND p.estado IN ('PAGADO','ENTREGADO') " +
                "UNION ALL SELECT 'COMPONENTE_MENU',pr.nombre,d.cantidad*mc.cantidad FROM detalle_pedido d JOIN menu_componentes mc ON mc.id_menu=d.id_menu JOIN productos pr ON pr.codigo=mc.producto_codigo JOIN pedidos p ON p.id_pedido=d.id_pedido WHERE d.tipo_item='MENU' AND p.estado IN ('PAGADO','ENTREGADO')) x GROUP BY origen,producto ORDER BY unidades DESC";
        return consultaTabla(sql);
    }

    public DefaultTableModel reporteBajoInventario() throws SQLException { return consultaTabla("SELECT codigo,nombre,categoria,existencia FROM productos WHERE existencia<=5 ORDER BY existencia,nombre"); }
    public DefaultTableModel reportePuntosClientes() throws SQLException { return consultaTabla("SELECT identificador,nombre,saldo_puntos FROM clientes WHERE activo=1 ORDER BY nombre"); }
    public DefaultTableModel reporteCanjes() throws SQLException { return consultaTabla("SELECT c.nombre cliente,ca.fecha,r.nombre recompensa,ca.puntos_utilizados FROM canjes ca JOIN clientes c ON c.id_cliente=ca.id_cliente JOIN recompensas r ON r.id_recompensa=ca.id_recompensa ORDER BY ca.fecha DESC"); }
    public DefaultTableModel reporteMenusVendidos() throws SQLException { return consultaTabla("SELECT m.codigo,m.nombre,SUM(d.cantidad) cantidad,SUM(d.subtotal) importe FROM detalle_pedido d JOIN menus m ON m.id_menu=d.id_menu JOIN pedidos p ON p.id_pedido=d.id_pedido WHERE d.tipo_item='MENU' AND p.estado IN ('PAGADO','ENTREGADO') GROUP BY m.codigo,m.nombre ORDER BY cantidad DESC"); }

    private void validarClienteActivo(Connection c,long idCliente) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT activo FROM clientes WHERE id_cliente=?")){ ps.setLong(1,idCliente); try(ResultSet rs=ps.executeQuery()){ if(!rs.next()) throw new SQLException("Cliente no encontrado."); if(rs.getInt(1)!=1) throw new SQLException("El cliente está desactivado."); }}
    }

    private void validarDisponibilidadPedido(Connection c,List<ItemVenta> items,boolean bloquear) throws SQLException {
        java.util.Map<String,Integer> requeridos=new java.util.LinkedHashMap<>();
        java.util.Map<String,String> nombres=new java.util.LinkedHashMap<>();
        for(ItemVenta item:items){
            if(item.getCantidad()<=0) throw new SQLException("La cantidad debe ser positiva.");
            if(item instanceof ProductoIndividual){
                requeridos.merge(item.getCodigo(),item.getCantidad(),Integer::sum);
                nombres.put(item.getCodigo(),item.getNombre());
            } else if(item instanceof MenuCompleto menu){
                try(PreparedStatement ps=c.prepareStatement("SELECT activo FROM menus WHERE id_menu=?")){ ps.setLong(1,menu.getIdMenu()); try(ResultSet rs=ps.executeQuery()){ if(!rs.next()||rs.getInt(1)!=1) throw new SQLException("Menú no disponible: "+item.getNombre()); }}
                int componentes=0;
                try(PreparedStatement ps=c.prepareStatement("SELECT mc.producto_codigo,mc.cantidad,p.nombre FROM menu_componentes mc JOIN productos p ON p.codigo=mc.producto_codigo WHERE mc.id_menu=?")){
                    ps.setLong(1,menu.getIdMenu());
                    try(ResultSet rs=ps.executeQuery()){
                        while(rs.next()){
                            componentes++;
                            String codigo=rs.getString(1);
                            requeridos.merge(codigo,rs.getInt(2)*item.getCantidad(),Integer::sum);
                            nombres.put(codigo,rs.getString(3));
                        }
                    }
                }
                if(componentes==0) throw new SQLException("El menú no tiene componentes configurados.");
            }
        }
        for(java.util.Map.Entry<String,Integer> e:requeridos.entrySet()){
            String sql="SELECT existencia,activo FROM productos WHERE codigo=?"+(bloquear?" FOR UPDATE":"");
            try(PreparedStatement ps=c.prepareStatement(sql)){
                ps.setString(1,e.getKey());
                try(ResultSet rs=ps.executeQuery()){
                    if(!rs.next()) throw new SQLException("Producto no encontrado: "+e.getKey());
                    if(rs.getInt("activo")!=1) throw new SQLException("Producto desactivado: "+nombres.getOrDefault(e.getKey(),e.getKey()));
                    int stock=rs.getInt("existencia");
                    if(stock<e.getValue()) throw new SQLException("Existencia insuficiente de "+nombres.getOrDefault(e.getKey(),e.getKey())+". Disponible: "+stock+", requerido: "+e.getValue());
                }
            }
        }
    }

    private void validarDisponibilidad(Connection c,ItemVenta item,boolean bloquear) throws SQLException {
        if(item.getCantidad()<=0) throw new SQLException("La cantidad debe ser positiva.");
        if(item instanceof ProductoIndividual) {
            String sql="SELECT existencia,activo FROM productos WHERE codigo=?"+(bloquear?" FOR UPDATE":"");
            try(PreparedStatement ps=c.prepareStatement(sql)){ ps.setString(1,item.getCodigo()); try(ResultSet rs=ps.executeQuery()){ if(!rs.next()) throw new SQLException("Producto no encontrado: "+item.getNombre()); if(rs.getInt("activo")!=1) throw new SQLException("Producto desactivado: "+item.getNombre()); int stock=rs.getInt("existencia"); if(stock<item.getCantidad()) throw new SQLException("Existencia insuficiente de "+item.getNombre()+". Disponible: "+stock); }}
        } else if(item instanceof MenuCompleto menu) {
            try(PreparedStatement ps=c.prepareStatement("SELECT activo FROM menus WHERE id_menu=?")){ ps.setLong(1,menu.getIdMenu()); try(ResultSet rs=ps.executeQuery()){ if(!rs.next()||rs.getInt(1)!=1) throw new SQLException("Menú no disponible: "+item.getNombre()); }}
            String sql="SELECT p.nombre,p.existencia,p.activo,mc.cantidad FROM menu_componentes mc JOIN productos p ON p.codigo=mc.producto_codigo WHERE mc.id_menu=?"+(bloquear?" FOR UPDATE OF p.existencia":"");
            int componentes=0;
            try(PreparedStatement ps=c.prepareStatement(sql)){ ps.setLong(1,menu.getIdMenu()); try(ResultSet rs=ps.executeQuery()){ while(rs.next()){ componentes++; int req=rs.getInt("cantidad")*item.getCantidad(); if(rs.getInt("activo")!=1) throw new SQLException("Componente desactivado: "+rs.getString("nombre")); if(rs.getInt("existencia")<req) throw new SQLException("Existencia insuficiente de "+rs.getString("nombre")+" para el menú. Disponible: "+rs.getInt("existencia")+", requerido: "+req); } }}
            if(componentes==0) throw new SQLException("El menú no tiene componentes configurados.");
        }
    }

    private List<ItemVenta> cargarItemsPedido(Connection c,long idPedido) throws SQLException {
        List<ItemVenta> r=new ArrayList<>();
        String sql="SELECT d.tipo_item,d.producto_codigo,d.id_menu,d.descripcion,d.cantidad,d.precio_unitario,p.categoria,m.codigo menu_codigo FROM detalle_pedido d LEFT JOIN productos p ON p.codigo=d.producto_codigo LEFT JOIN menus m ON m.id_menu=d.id_menu WHERE d.id_pedido=? ORDER BY d.id_detalle";
        try(PreparedStatement ps=c.prepareStatement(sql)){ ps.setLong(1,idPedido); try(ResultSet rs=ps.executeQuery()){ while(rs.next()){ if("PRODUCTO".equals(rs.getString("tipo_item"))) r.add(new ProductoIndividual(rs.getString("producto_codigo"),rs.getString("descripcion"),rs.getBigDecimal("precio_unitario"),rs.getInt("cantidad"),rs.getString("categoria"))); else r.add(new MenuCompleto(rs.getLong("id_menu"),rs.getString("menu_codigo"),rs.getString("descripcion"),rs.getBigDecimal("precio_unitario"),rs.getInt("cantidad"))); } }}
        return r;
    }

    private void descontarInventario(Connection c,ItemVenta item,String tipo,String referencia) throws SQLException {
        if(item instanceof ProductoIndividual) descontarProducto(c,item.getCodigo(),item.getCantidad(),tipo,referencia);
        else if(item instanceof MenuCompleto menu) {
            try(PreparedStatement ps=c.prepareStatement("SELECT producto_codigo,cantidad FROM menu_componentes WHERE id_menu=?")){ ps.setLong(1,menu.getIdMenu()); try(ResultSet rs=ps.executeQuery()){ while(rs.next()) descontarProducto(c,rs.getString(1),rs.getInt(2)*item.getCantidad(),tipo,referencia); }}
        }
    }

    private void descontarProducto(Connection c,String codigo,int cantidad,String tipo,String referencia) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("UPDATE productos SET existencia=existencia-? WHERE codigo=? AND existencia>=?")){ ps.setInt(1,cantidad); ps.setString(2,codigo); ps.setInt(3,cantidad); if(ps.executeUpdate()==0) throw new SQLException("No fue posible descontar inventario de "+codigo); }
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO movimientos_inventario(producto_codigo,cantidad,tipo,referencia) VALUES(?,?,?,?)")){ ps.setString(1,codigo); ps.setInt(2,-cantidad); ps.setString(3,tipo); ps.setString(4,referencia); ps.executeUpdate(); }
    }

    private DefaultTableModel consultaTabla(String sql,Object... params) throws SQLException {
        try(Connection c=ConexionDB.conectar(); PreparedStatement ps=c.prepareStatement(sql)){
            for(int i=0;i<params.length;i++) ps.setObject(i+1,params[i]);
            try(ResultSet rs=ps.executeQuery()){
                ResultSetMetaData md=rs.getMetaData(); int cols=md.getColumnCount(); String[] nombres=new String[cols]; for(int i=0;i<cols;i++) nombres[i]=md.getColumnLabel(i+1);
                DefaultTableModel model=new DefaultTableModel(nombres,0){ @Override public boolean isCellEditable(int r,int c){ return false; } };
                while(rs.next()){ Object[] row=new Object[cols]; for(int i=0;i<cols;i++) row[i]=rs.getObject(i+1); model.addRow(row); }
                return model;
            }
        }
    }

    private String vacioANull(String s){ return s==null||s.isBlank()?null:s.trim(); }
}
