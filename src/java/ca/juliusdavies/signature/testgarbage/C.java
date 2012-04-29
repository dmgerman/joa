package ca.juliusdavies.signature.testgarbage;

import ca.juliusdavies.signature.Artifact;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;

public abstract class C<A extends Artifact> implements Collection<Number>, Comparable<C>, Serializable, Cloneable {

    public void blowup() throws SQLException, java.io.IOException {
        if (true) {
            throw new NumberFormatException("fooled ya!");
        }
    }


}


class Another {}

class YetAnother {}

