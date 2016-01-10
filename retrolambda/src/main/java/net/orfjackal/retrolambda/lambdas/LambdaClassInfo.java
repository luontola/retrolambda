// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.lambdas;

public class LambdaClassInfo {

    private final String lambdaClass;
    private final String referenceName;
    private final String referenceDesc;
    private final int argumentCount;

    public LambdaClassInfo(String lambdaClass, String referenceName, String referenceDesc, int argumentCount) {
        this.lambdaClass = lambdaClass;
        this.referenceName = referenceName;
        this.referenceDesc = referenceDesc;
        this.argumentCount = argumentCount;
    }

    public boolean isStateless() {
        return getArgumentCount() == 0;
    }

    public String getLambdaClass() {
        return lambdaClass;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public String getReferenceDesc() {
        return referenceDesc;
    }

    public int getArgumentCount() {
        return argumentCount;
    }
}
