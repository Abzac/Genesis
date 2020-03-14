import java.util.LinkedList;

public class SimpleStack<E> extends LinkedList<E> {
    private int limit;

    public SimpleStack(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E o) {
        super.addFirst(o);
        while (size() > limit) { super.removeLast(); }
        return true;
    }
}
