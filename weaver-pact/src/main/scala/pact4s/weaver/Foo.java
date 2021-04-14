package foo;
import foo.Foo.Bar1;
import java.util.List;

public abstract class Foo {
    public static final class Bar1 extends Foo {}
    public static final class Bar2 extends Foo {}
}

class Baz {
    public void bars(List<Bar1> bs) {
        bs.clear();
    }
}
