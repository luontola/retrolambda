// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.test;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

public class MissingStackMapFramesTest {

    @Test
    public void fixes_missing_stackmap_frames_in_Java_6_compiled_dependencies() {
        assumeTrue(SystemUtils.isJavaVersionAtLeast(160));

        new GuineaPig();
    }

    public static class GuineaPig extends com.google.android.gms.maps.SupportMapFragment {
        Runnable r = () -> {
        };
    }
}
