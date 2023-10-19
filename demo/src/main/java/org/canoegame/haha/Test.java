package org.canoegame.haha;

public class Test implements Cloneable {
    public int a = 1;
    public int b = 2;

    @Override
    public String toString() {
        return "Test{" +
                "a=" + a +
                ", b=" + b +
                '}';
    }

    static class Test1 extends Test {

    }
    public static void main(String[] args) throws CloneNotSupportedException {
        var test = new Test1();
        var b = test.clone();
        System.out.println(b);
    }
}
