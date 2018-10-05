// Copyright © 2013-2018 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.api;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class ApiMappingSetTest {

    @Test
    public void mapPackage() throws IOException {
        ApiMappingSet mappingSet = new ApiMappingSet(Collections.singletonList("streamsupport"));

        assertThat(mappingSet.mapClass("java/util/function/Function"), equalTo("java8/util/function/Function"));
        assertThat(mappingSet.mapClass("java/util/function/Predicate"), equalTo("java8/util/function/Predicate"));
        assertThat(mappingSet.mapClass("java/util/function/subpackage/MyClass"), equalTo("java8/util/function/subpackage/MyClass"));

    }


}