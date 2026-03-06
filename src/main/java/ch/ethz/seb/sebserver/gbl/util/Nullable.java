package ch.ethz.seb.sebserver.gbl.util;

public final class Nullable<T> {

    public final T element;

    public Nullable(T element) {
        this.element = element;
    }

    public boolean isNull() {
        return element == null;
    }

    public T getElement() {
        return element;
    }

    @Override
    public String toString() {
        return "Nullable{" +
                "element=" + element +
                '}';
    }

    public static <T> Nullable<T> ofNull() {
        return new Nullable<>(null);
    }
}
