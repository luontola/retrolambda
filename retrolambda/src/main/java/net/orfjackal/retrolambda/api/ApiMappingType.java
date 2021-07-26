// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.api;

public enum ApiMappingType {

    /**
     * Maps all classes in a package and its subpackages to a new package
     */
    PACKAGE,

    /**
     * Maps a class and all its member to a new class
     */
    CLASS,

    /**
     * Maps a static method from one class to a new class and method name
     */
    INVOKESTATIC,

    /**
     * Maps a static field from one class to a new class and field name
     */
    GETSTATIC

}
