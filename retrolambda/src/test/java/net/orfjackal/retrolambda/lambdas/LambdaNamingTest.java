// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import net.orfjackal.retrolambda.lambdas.LambdaNaming;
import org.junit.Assert;
import org.junit.Test;

public class LambdaNamingTest {

    @Test
    public void isSerializationHookInputPositiveNotNullNotNullOutputTrue() {
        final int access = 2;
        final String name = "writeReplace";
        final String desc = "()Ljava/lang/Object;";
        final boolean retval = LambdaNaming.isSerializationHook(access, name, desc);
        Assert.assertTrue(retval);
    }

    @Test
    public void isSerializationHookInputZeroNotNullNotNullOutputFalse() {
        final int access = 0;
        final String name = "writeReplace";
        final String desc = "()Ljava/lang/Object;";
        final boolean retval = LambdaNaming.isSerializationHook(access, name, desc);
        Assert.assertFalse(retval);
    }

    @Test
    public void isPlatformFactoryMethodInputZeroNotNullNotNullNotNullOutputFalse() {
        final int access = 0;
        final String name = "get$Lambda";
        final String desc = "";
        final String targetDesc = "!";
        final boolean retval = LambdaNaming.isPlatformFactoryMethod(access, name, desc, targetDesc);
        Assert.assertFalse(retval);
    }

    @Test
    public void isPlatformFactoryMethodInputPositiveNotNullNotNullNotNullOutputTrue() {
        final int access = 10;
        final String name = "get$Lambda";
        final String desc = "\"\"\"\"\"\"\"";
        final String targetDesc = "\"\"\"\"\"\"\"";
        final boolean retval = LambdaNaming.isPlatformFactoryMethod(access, name, desc, targetDesc);
        Assert.assertTrue(retval);
    }

    @Test
    public void isDeserializationHookInputZeroNotNullNotNullOutputFalse() {
        final int access = 0;
        final String name = "$deserializeLambda$";
        final String desc = "000000000000001000000";
        final boolean retval = LambdaNaming.isDeserializationHook(access, name, desc);
        Assert.assertFalse(retval);
    }

    @Test
    public void isDeserializationHookInputPositiveNotNullNotNullOutputTrue() {
        final int access = 4106;
        final String name = "$deserializeLambda$";
        final String desc = "(Ljava/lang/invoke/SerializedLambda;)Ljava/lang/Object;";
        final boolean retval = LambdaNaming.isDeserializationHook(access, name, desc);
        Assert.assertTrue(retval);
    }

    @Test
    public void isBodyMethodInputPositiveNotNullOutputTrue() {
        final int access = 4096;
        final String name = "lambda$";
        final boolean retval = LambdaNaming.isBodyMethod(access, name);
        Assert.assertTrue(retval);
    }

    @Test
    public void isBodyMethodInputZeroNotNullOutputFalse() {
        final int access = 0;
        final String name = "lambda$";
        final boolean retval = LambdaNaming.isBodyMethod(access, name);
        Assert.assertFalse(retval);
    }

}
