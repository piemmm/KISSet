package org.prowl.ax25;


import java.util.Objects;

/**
 * Holds a KISS parameter and it's data
 */
public class KissParameter {

    int[] data;
    KissParameterType parameter;

    public KissParameter(KissParameterType parameter, int[] data) {
        this.parameter = parameter;
        this.data = data;
    }

    public KissParameter(KissParameterType parameter, int data) {
        this.parameter = parameter;
        this.data = new int[]{data};
    }

    public int[] getData() {
        return data;
    }

    public KissParameterType getParameter() {
        return parameter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KissParameter that = (KissParameter) o;
        return parameter == that.parameter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameter);
    }
}
