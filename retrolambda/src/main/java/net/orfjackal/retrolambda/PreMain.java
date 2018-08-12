// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import net.orfjackal.retrolambda.lambdas.*;

import java.lang.instrument.Instrumentation;

public class PreMain {

    private static final LambdaClassSaverAgent agent = new LambdaClassSaverAgent();
    private static boolean agentLoaded = false;

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(agent);
        agentLoaded = true;
    }

    public static boolean isAgentLoaded() {
        return agentLoaded;
    }

    public static void setLambdaClassSaver(LambdaClassSaver lambdaClassSaver, boolean isJavacHacksEnabled) {
        agent.setLambdaClassSaver(lambdaClassSaver, isJavacHacksEnabled);
    }
}
