// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

import org.objectweb.asm.Type;

public class LambdaFactoryMethod {

    private final String owner;
    private final String desc;

    public LambdaFactoryMethod(String lambdaClass, Type invokedType) {
        owner = lambdaClass;
        desc = invokedType.getDescriptor();
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return "lambdaFactory$";
    }

    public String getDesc() {
        return desc;
    }
}
