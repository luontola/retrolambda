// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.Type;

public class LambdaFactoryMethod {

    private final String owner;
    private final String desc;

    public LambdaFactoryMethod(String lambdaClass, Type invokedType) {
        owner = lambdaClass;
        // TODO: get rid of toFactoryMethodDesc by changing the method's return type to be same as invokedType
        desc = LambdaClassBackporter.toFactoryMethodDesc(lambdaClass, invokedType);
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return LambdaClassBackporter.FACTORY_METHOD_NAME;
    }

    public String getDesc() {
        return desc;
    }
}
