/*
 * Copyright 2018 Massimo Neri <hello@mneri.me>
 *
 * This file is part of mneri/csv.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.mneri.csv.extension.internal

import groovy.transform.CompileStatic
import me.mneri.csv.extension.VectorHelper
import spock.lang.Specification

class ExtensionsTest extends Specification {
    def "SIMD is supported in the test JVM"() {
        expect: "build.gradle runs the tests with --add-modules=jdk.incubator.vector"
        Extensions.SIMD_SUPPORTED
    }

    def "SIMD is not supported when VectorHelper can't load, as on Java 16"() {
        given:
        def extensions = Class.forName(Extensions.name, true, new Java16ClassLoader())

        expect: "the module jdk.incubator.vector is there, but VectorHelper is not"
        !extensions.SIMD_SUPPORTED
    }

    /**
     * Load a copy of Extensions, and reject VectorHelper as Java 16 rejects a class compiled for Java 17.
     */
    @CompileStatic
    static class Java16ClassLoader extends ClassLoader {
        Java16ClassLoader() {
            super(Extensions.classLoader)
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) {
            if (name == VectorHelper.name) {
                throw new UnsupportedClassVersionError(name)
            }
            if (name == Extensions.name) {
                byte[] bytes = Extensions.getResourceAsStream("Extensions.class").bytes
                return defineClass(name, bytes, 0, bytes.length)
            }
            return super.loadClass(name, resolve)
        }
    }
}
