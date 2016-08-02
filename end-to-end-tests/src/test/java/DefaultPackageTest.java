// Copyright © 2013-2016 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import org.junit.Test;

public final class DefaultPackageTest {
    @Test
    public void method_reference_to_sibling_class() {
        SiblingClass sibling = new SiblingClass();
        Runnable lambda = sibling::method;
        lambda.run();
    }
}

class SiblingClass {
    void method() {
    }
}
