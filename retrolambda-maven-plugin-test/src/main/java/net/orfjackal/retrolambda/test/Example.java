package net.orfjackal.retrolambda.test;

import java.util.function.Consumer;

public class Example {

	public static void main(String[] args) {
		Consumer<Integer> c = (Integer x) -> {
			System.out.println(x);
		};
		c.accept(12);
	}
}
