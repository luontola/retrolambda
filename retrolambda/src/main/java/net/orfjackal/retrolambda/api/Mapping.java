// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.api;

class Mapping {

    private final String owner;
    private final String name;

    Mapping(String signature) {
        String[] parts = signature.split("\\.");
        this.owner = parts[0];
        this.name = parts[1];
    }

    public Mapping(String owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }
}
