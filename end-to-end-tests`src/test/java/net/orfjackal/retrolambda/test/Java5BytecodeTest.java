// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import com.google.common.io.CharStreams;
import org.apache.commons.lang.SystemUtils;
import org.junit.Test;

import java.io.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Java5BytecodeTest {

    @Test
    public void does_not_generate_stack_map_tables_for_Java_5() throws IOException {
        String javapOutput = javap(Dummy.class);

        if (SystemUtils.isJavaVersionAtLeast(1.6f)) {
            assertThat(javapOutput, containsString("StackMap"));
        } else {
            assertThat(javapOutput, not(containsString("StackMap")));
        }
    }

    private static String javap(Class<?> aClass) throws IOException {
        Process process = new ProcessBuilder()
                .directory(TestEnv.testClassesDir)
                .command("javap", "-v", "-p", aClass.getName())
                .redirectErrorStream(true)
                .start();
        return CharStreams.toString(new InputStreamReader(process.getInputStream()));
    }


    public static class Dummy {
        public Dummy() {
            // cause this method to have a stack map table
            for (int i = 0; i < 3; i++) {
                System.out.println(i);
            }
        }
    }
}
