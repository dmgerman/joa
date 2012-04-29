package org.glassfish.enterprise.ha.store.criteria;

/**
 * A class that represents a Criteria. Currently only an Expression<Boolean>
 *  can be specified using a Criteria. In future this class may be modified
 *  to support selction of Attributes from V
 *
 * @param <V> The type of
 */
public final class Criteria<V> {

    private Class<V> entryClazz;

    private Expression<Boolean> expression;


    Criteria(Class<V> entryClazz) {
        this.entryClazz = entryClazz;
    }

    public Expression<Boolean> getExpression() {
        return expression;
    }

    public void setExpression(Expression<Boolean> expression) {
        this.expression = expression;
    }

}
