// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.interfaces;

import java.util.List;

public interface MethodRelocations {

    MethodRef getMethodCallTarget(MethodRef original);

    MethodRef getMethodDefaultImplementation(MethodRef interfaceMethod);

    List<MethodRef> getInterfaceMethods(String interfaceName);

    String getCompanionClass(String className);
}
