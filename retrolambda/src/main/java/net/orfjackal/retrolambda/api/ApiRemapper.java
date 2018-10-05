// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.api;

import org.objectweb.asm.commons.Remapper;

class ApiRemapper extends Remapper {

    private ApiMappingSet mappingSet;

    public ApiRemapper(ApiMappingSet mappingSet) {
        this.mappingSet = mappingSet;
    }

    @Override
    public String map(String typeName) {
        return mappingSet.mapClass(typeName);
    }
}
