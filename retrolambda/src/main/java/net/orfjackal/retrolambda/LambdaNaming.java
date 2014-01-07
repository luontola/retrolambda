// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import java.util.regex.Pattern;

public class LambdaNaming {

    public static final String LAMBDA_METAFACTORY = "java/lang/invoke/LambdaMetafactory";
    public static final String MAGIC_LAMBDA_IMPL = "java/lang/invoke/MagicLambdaImpl";
    public static final Pattern LAMBDA_CLASS = Pattern.compile("^.+\\$\\$Lambda\\$\\d+$");
    public static final Pattern LAMBDA_IMPL_METHOD = Pattern.compile("^lambda\\$.*\\$\\d+$");
}
