// Copyright © 2013 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;

public class PreMain {

    private static boolean agentLoaded = false;

    public static void premain(String agentArgs, Instrumentation inst) {
        Config config = new Config(System.getProperties());
        int bytecodeVersion = config.getBytecodeVersion();
        Path outputDir = config.getOutputDir();
        inst.addTransformer(new LambdaSavingClassFileTransformer(outputDir, bytecodeVersion));
        agentLoaded = true;
    }

    public static boolean isAgentLoaded() {
        return agentLoaded;
    }
}
