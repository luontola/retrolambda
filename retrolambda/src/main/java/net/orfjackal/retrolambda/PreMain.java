// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.lambdas.*;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

public class PreMain {

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        // Append the agent JAR to the bootstrap search path so that the instrumented InnerClassLambdaMetaFactory
        // could refer to Agent.
        inst.appendToBootstrapClassLoaderSearch(new JarFile(getAgentJarFile()));

        inst.addTransformer(new InnerClassLambdaMetafactoryTransformer(), true);
        inst.retransformClasses(Class.forName("java.lang.invoke.InnerClassLambdaMetafactory"));
    }

    private static File getAgentJarFile() throws URISyntaxException {
        return new File(PreMain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
}
