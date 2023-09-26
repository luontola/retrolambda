// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.toolchain.*;
import org.apache.maven.toolchain.java.*;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class ProcessClassesMojoTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final Log log = mock(Log.class);
    private final FakeToolchainManager toolchainManager = new FakeToolchainManager();
    private final ProcessMainClassesMojo mojo = new ProcessMainClassesMojo();

    @Before
    public void sensibleDefaults() {
        mojo.setLog(log);
        mojo.toolchainManager = toolchainManager;
        mojo.target = "1.7";
    }

    @Test
    public void error_message_lists_the_accepted_targets() throws MojoExecutionException {
        mojo.target = "1.0";

        thrown.expect(MojoExecutionException.class);
        thrown.expectMessage("1.5, 1.6, 1.7, 1.8");
        mojo.execute();
    }

    @Test
    public void java_command_defaults_to_current_jvm() {
        assertThat(mojo.getJavaCommand(), is(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath()));
    }

    @Test
    public void java_command_from_toolchain_overrides_the_current_jvm() {
        toolchainManager.setJdkToolChain(new FakeJavaToolChain("jdk-from-toolchain"));

        assertThat(mojo.getJavaCommand(), is("jdk-from-toolchain/bin/java"));
        verify(log).info("Toolchain in retrolambda-maven-plugin: JDK[jdk-from-toolchain]");
    }

    @Test
    public void java_command_from_local_configuration_overrides_the_toolchain() {
        toolchainManager.setJdkToolChain(new FakeJavaToolChain("jdk-from-toolchain"));
        mojo.java8home = new File("jdk-from-local-configuration");

        assertThat(mojo.getJavaCommand().replace('\\', '/'), is("jdk-from-local-configuration/bin/java"));
        verify(log).warn("Toolchains are ignored, 'java8home' parameter is set to jdk-from-local-configuration");
    }


    private static class FakeToolchainManager implements ToolchainManager {

        private final Map<String, Toolchain> toolChainsByType = new HashMap<String, Toolchain>();

        @Override
        public Toolchain getToolchainFromBuildContext(String type, MavenSession context) {
            return toolChainsByType.get(type);
        }

        public void setJdkToolChain(JavaToolchain toolChain) {
            toolChainsByType.put("jdk", toolChain);
        }

        @Override
        public List<Toolchain> getToolchains(MavenSession session, String type,
            Map<String, String> requirements) {
          Toolchain tc = toolChainsByType.get("jdk");
          if(tc == null) {
            return Collections.emptyList();
          } else {
            return Collections.singletonList(tc);
          }
        }
    }

    private static class FakeJavaToolChain extends DefaultJavaToolChain {

        public FakeJavaToolChain(String javaHome) {
            super(null, null);
            setJavaHome(javaHome);
        }

        @Override
        public String findTool(String toolName) {
            return getJavaHome() + "/bin/" + toolName;
        }
    }
}
