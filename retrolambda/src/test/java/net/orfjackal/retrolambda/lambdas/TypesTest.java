// Copyright © 2013-2016 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import org.junit.Test;
import org.objectweb.asm.*;

import java.lang.invoke.*;
import java.util.function.Predicate;

import static net.orfjackal.retrolambda.lambdas.Types.asmToJdkType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SuppressWarnings("UnnecessaryLocalVariable")
public class TypesTest {

    private ClassLoader classLoader = getClass().getClassLoader();
    private MethodHandles.Lookup lookup = MethodHandles.lookup();

    @Test
    public void asmToJdkType_MethodType() throws Exception {
        Type input = Type.getMethodType("(I)Ljava/util/function/Predicate;");
        MethodType output = MethodType.methodType(Predicate.class, int.class);

        assertThat(asmToJdkType(input, classLoader, lookup), is(output));
    }

    @Test
    public void asmToJdkType_MethodHandle() throws Exception {
        Handle input = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);
        MethodHandle output = lookup.findStatic(String.class, "valueOf", MethodType.methodType(String.class, int.class));

        assertThat(asmToJdkType(input, classLoader, lookup).toString(), is(output.toString()));
    }

    @Test
    public void asmToJdkType_Class() throws Exception {
        Type input = Type.getType(String.class);
        Class<?> output = String.class;

        assertThat(asmToJdkType(input, classLoader, lookup), is(equalTo(output)));
    }

    @Test
    public void asmToJdkType_everything_else() throws Exception {
        String input = "foo";
        String output = input;

        assertThat(asmToJdkType(input, classLoader, lookup), is(output));
    }
}
