// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.*;
import org.junit.rules.ExpectedException;

public class ProcessClassesMojoTest {

    private final ProcessMainClassesMojo mojo = new ProcessMainClassesMojo();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Before
    public void sensibleDefaults() {
        mojo.target = "1.7";
        mojo.java8home = System.getProperty("java.home");
    }

    @Test
    public void error_message_lists_the_accepted_targets() throws MojoExecutionException {
        mojo.target = "1.0";

        thrown.expect(MojoExecutionException.class);
        thrown.expectMessage("1.5, 1.6, 1.7, 1.8");
        mojo.execute();
    }
}
