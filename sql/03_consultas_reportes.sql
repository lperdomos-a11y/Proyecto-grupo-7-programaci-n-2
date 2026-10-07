SELECT p.id_pedido, c.nombre cliente, p.fecha, p.total, pg.metodo
FROM pedidos p
JOIN clientes c ON c.id_cliente=p.id_cliente
JOIN pagos pg ON pg.id_pedido=p.id_pedido
WHERE p.estado IN ('PAGADO','ENTREGADO')
  AND p.fecha BETWEEN :fecha_inicio AND :fecha_fin
ORDER BY p.fecha;

SELECT origen, producto, SUM(unidades) unidades
FROM (
    SELECT 'INDIVIDUAL' origen, pr.nombre producto, d.cantidad unidades
    FROM detalle_pedido d
    JOIN productos pr ON pr.codigo=d.producto_codigo
    JOIN pedidos p ON p.id_pedido=d.id_pedido
    WHERE d.tipo_item='PRODUCTO' AND p.estado IN ('PAGADO','ENTREGADO')
    UNION ALL
    SELECT 'COMPONENTE_MENU' origen, pr.nombre producto, d.cantidad*mc.cantidad unidades
    FROM detalle_pedido d
    JOIN menu_componentes mc ON mc.id_menu=d.id_menu
    JOIN productos pr ON pr.codigo=mc.producto_codigo
    JOIN pedidos p ON p.id_pedido=d.id_pedido
    WHERE d.tipo_item='MENU' AND p.estado IN ('PAGADO','ENTREGADO')
)
GROUP BY origen, producto
ORDER BY unidades DESC;

SELECT codigo,nombre,categoria,existencia FROM productos WHERE existencia <= 5 ORDER BY existencia,nombre;
SELECT identificador,nombre,saldo_puntos FROM clientes WHERE activo=1 ORDER BY nombre;
SELECT c.nombre cliente, ca.fecha, r.nombre recompensa, ca.puntos_utilizados FROM canjes ca JOIN clientes c ON c.id_cliente=ca.id_cliente JOIN recompensas r ON r.id_recompensa=ca.id_recompensa ORDER BY ca.fecha DESC;
SELECT m.codigo,m.nombre,SUM(d.cantidad) cantidad,SUM(d.subtotal) importe FROM detalle_pedido d JOIN menus m ON m.id_menu=d.id_menu JOIN pedidos p ON p.id_pedido=d.id_pedido WHERE d.tipo_item='MENU' AND p.estado IN ('PAGADO','ENTREGADO') GROUP BY m.codigo,m.nombre ORDER BY cantidad DESC;
