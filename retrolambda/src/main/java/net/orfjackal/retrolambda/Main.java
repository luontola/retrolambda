// Copyright © 2013-2015 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.io.*;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {
        System.out.println("Retrolambda " + getVersion());

        if (!isRunningJava8()) {
            System.out.println("Error! Not running under Java 8");
            System.exit(1);
        }

        SystemPropertiesConfig config = new SystemPropertiesConfig(System.getProperties());
        if (!config.isFullyConfigured()) {
            System.out.print(config.getHelp());
            return;
        }
        try {
            Retrolambda.run(config);
        } catch (Throwable t) {
            System.out.println("Error! Failed to transform some classes");
            t.printStackTrace(System.out);
            System.exit(1);
        }
    }

    public static boolean isRunningJava8() {
        try {
            Class.forName("java.util.stream.Stream");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static String getVersion() {
        Properties p = new Properties();
        try (InputStream in = Main.class.getResourceAsStream("/META-INF/maven/net.orfjackal.retrolambda/retrolambda/pom.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return p.getProperty("version", "DEVELOPMENT-VERSION");
    }
}
