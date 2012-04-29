package org.glassfish.enterprise.ha.store.criteria;

import org.glassfish.enterprise.ha.store.spi.AttributeMetadata;
import org.glassfish.enterprise.ha.store.criteria.spi.*;

/**
 * A Class to construct portable Criteria objects
 * 
 */
public class ExpressionBuilder<V> {

    Class<V> entryClazz;

    public ExpressionBuilder(Class<V> entryClazz) {
        this.entryClazz = entryClazz;
    }

    public Criteria<V> setCriteria(Expression<Boolean> expr) {
        Criteria<V> c = new Criteria<V>(entryClazz);
        c.setExpression(expr);

        return c;
    }

    public <T> AttributeAccessNode<V, T> attr(AttributeMetadata<V, T> meta) {
        return new AttributeAccessNode<V, T>(meta);
    }

    public <T> LiteralNode<T> literal(Class<T> type, T value) {
        return new LiteralNode<T>(type, value);
    }

    public <T> LogicalExpressionNode eq(T value, AttributeMetadata<V, T> meta) {
        return new LogicalExpressionNode(Opcode.EQ,
                new LiteralNode<T>(meta.getAttributeType(), value),
                new AttributeAccessNode<V, T>(meta));
    }

    public <T> LogicalExpressionNode eq(AttributeMetadata<V, T> meta, T value) {
        return new LogicalExpressionNode(Opcode.EQ,
                new AttributeAccessNode<V, T>(meta),
                new LiteralNode<T>(meta.getAttributeType(), value));
    }

    public <T> LogicalExpressionNode eq(AttributeMetadata<V, T> meta1,
                                           AttributeMetadata<V, T> meta2) {
        return new LogicalExpressionNode(Opcode.EQ,
                new AttributeAccessNode<V, T>(meta1),
                new AttributeAccessNode<V, T>(meta2));
    }

    public <T> LogicalExpressionNode eq(ExpressionNode<T> expr1, ExpressionNode<T> expr2) {
        return new LogicalExpressionNode(Opcode.EQ, expr1, expr2);
    }

    public <T extends Number> LogicalExpressionNode eq(LiteralNode<T> value, AttributeMetadata<V, T> meta) {
        return new LogicalExpressionNode(Opcode.EQ,
                value, new AttributeAccessNode<V, T>(meta));
    }

    public <T extends Number> LogicalExpressionNode eq(AttributeMetadata<V, T> meta, LiteralNode<T> value) {
        return new LogicalExpressionNode(Opcode.EQ,
                new AttributeAccessNode<V, T>(meta), value);
    }

}
