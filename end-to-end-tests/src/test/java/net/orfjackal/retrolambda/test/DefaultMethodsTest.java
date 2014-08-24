// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.junit.Test;

import java.util.Comparator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DefaultMethodsTest {

    /**
     * JDK 8 adds a bridge method to an interface when it overrides a method
     * from the parent interface and refines its return type. This uses Java 8's
     * default methods feature, which won't work on Java 7 and below, so we have
     * to remove it for it - this makes the bytecode same as what JDK 7 produces.
     */
    @Test
    public void will_remove_bridge_methods_from_interfaces() {
        class Foo implements Child {
            @Override
            public String foo() {
                return "foo";
            }
        }
        assertThat("direct call", new Foo().foo(), is("foo"));
        assertThat("bridged call", ((Parent) new Foo()).foo(), is((Object) "foo"));
    }

    public interface Parent {
        Object foo();
    }

    public interface Child extends Parent {
        String foo(); // refined return type
    }

	public interface Parent2 {
		default Object method() {
			return "Parent";
		}
	}

	public interface Child2 extends Parent2{
		@Override
		default String method() {
			return "Child2";
		}
	}

	@Test
	public void will_return_right_string() {
		boolean sameStrings = new Child2() {

		}.method().equals("Child2");
		assertThat("they are equal", sameStrings);
	}

	interface Primitives {
		default int anInt() {
			return 1;
		}
		default short aShort() {
			return 2;
		}
		default long aLong() {
			return 1L << 50;
		}
		default boolean aBoolean() {
			return true;
		}
		default float aFloat() {
			return 0f;
		}
		default double aDouble() {
			return 0.0;
		}
		default void aVoid() {
		}
	}

	@Test
	public void primitives_run() {
		Primitives p = new Primitives() {
		};
		assertThat("booleans ok", p.aBoolean());
		assertThat("ints ok", p.anInt() == 1);
		assertThat("shorts ok", p.aShort() == 2);
		assertThat("longs ok", p.aLong() == 1L << 50);
		assertThat("floats ok", p.aFloat() == 0f);
		assertThat("doubles ok", p.aDouble() == 0.0);
		p.aVoid(); // would crash
	}

	interface Chaining {
		default String myString() {
			return "Interface";
		}
		default String join(Chaining other) {
			return myString() + other.myString();
		}
	}

	@Test
	public void anonymous_instances() {
		Chaining c1 = new Chaining() {

		};
		Chaining c2 = new Chaining() {

		};
		assertThat("Strings equals", c1.join(c2).equals("InterfaceInterface"));
		Chaining anon = new Chaining() {
			@Override
			public String myString() {
				return "Anon";
			}
		};
		assertThat("Anonymous override equals", c1.join(anon).equals("InterfaceAnon"));
	}

	interface DeepParent {
		default int level() {
			return 1;
		}
	}
	interface DeepChild extends DeepParent {
		@Override
		default int level() {
			return DeepParent.super.level() + 1;
		}
	}

	@Test
	public void test_override_primitive() {
		DeepChild d1 = new DeepChild() {

		};
		assertThat("override works", d1.level() == 2);
		DeepChild d2 = new DeepChild() {
			@Override
			public int level() {
				return 1 + DeepChild.super.level();
			}
		};
		assertThat("super call interface works", d2.level() == 3);
	}

	interface Conflict1 {
		default String confl() {
			return "1";
		}
	}

	interface Conflict2 {
		default String confl() {
			return "2";
		}
	}

	@Test
	public void will_handle_override_proprly() {
		class C implements Conflict1, Conflict2 {
			public String confl() {
				return Conflict1.super.confl() + Conflict2.super.confl();
			}
		}
		assertThat("Handles method conflict", new C().confl().equals("12"));
	}

	interface DeepParent2 {
		int anInt();
		default int method(DeepParent2 p1, DeepParent2 p2, DeepParent2 p3) {
			return p1.anInt() + p2.anInt() + p3.anInt() + anInt();
		}
	}

	@Test
	public void will_handle_long_paramlist() {
		DeepParent2 dp = new DeepParent2() {
			@Override
			public int anInt() {
				return 2;
			}
		};
		assertThat("Long call parameter list works", dp.method(dp, dp, dp) == 8);
	}

		@Test
	public void will_handle_lambda() {
		DeepParent2 dp = () -> 2;
		assertThat("Long call parameter list with lambda works", dp.method(dp, dp, dp) == 8);
	}

	interface BridgeTest<T> {
		default T max(T t1, T t2, Comparator<? super T> comparator) {
			return comparator.compare(t1, t2) > 0 ? t1 : t2;
		}
	}

	interface StringBridge extends BridgeTest<String> {
		default boolean compare() {
			return max("A", "B", String.CASE_INSENSITIVE_ORDER).equals("B");
		}
	}

	@Test
	public void handles_bridge_methods() {
		StringBridge sb = new StringBridge() {
		};
		assertThat("returns true", sb.compare());
		BridgeTest<String> sb2 = sb;
		assertThat("still returns true", sb2.max("A", "B", String.CASE_INSENSITIVE_ORDER).equals("B"));
	}

	interface MiddleParent {
		default int anInt() {
			return 1;
		}
	}
	interface Middle2Parent extends MiddleParent{
		@Override
		default int anInt() {
			return 2;
		}
	}
	interface Middle3aParent extends MiddleParent, Middle2Parent {

	}
	interface Middle3bParent extends Middle2Parent, MiddleParent {

	}
	@Test
	public void right_method_chosen() {
		assertThat(new Middle3aParent() {

		}.anInt(), is(2));

		assertThat(new Middle3bParent() {

		}.anInt(), is(2));
	}

	interface Top<T>  {
		T anObject();
		default int anInt() {
			return 1;
		}
	}

	interface SubTop<T extends CharSequence> extends Top<T> {
		default int anInt() {
			return Top.super.anInt() + 1;
		}
	}
	interface SubSub extends SubTop<String> {
		default int anInt() {
			return SubTop.super.anInt() + 1;
		}
		default String anObject() {
			return "0";
		}
	}
	interface SubSub2 extends SubTop<String> {
		default String anObject() {
			return "1";
		}
	}

	@Test
	public void yet_another_deep_hiearchy_test_with_bridges() {
		assertThat(new SubSub2() {

		}.anInt(), is(2));

		assertThat(new SubSub() {

		}.anInt(), is(3));
		SubSub sub = new SubSub() {

		};
		assertThat(sub.anInt(), is(3));
		Top<?> top = sub;
		assertThat("is instanceof string", top.anObject() instanceof String);
	}

	interface DefaultToStatic {
		default int ifMeth() {
			return staticMeth();
		}

		static int staticMeth() {
			return 3;
		}
	}

	@Test
	public void call_static_methods_from_default() {
		DefaultToStatic i = new DefaultToStatic() {
		};
		assertThat(i.ifMeth(), is(3));
		assertThat(DefaultToStatic.staticMeth(), is(3));
	}
//
}
