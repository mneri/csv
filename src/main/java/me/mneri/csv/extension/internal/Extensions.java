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

package me.mneri.csv.extension.internal;

import me.mneri.csv.extension.VectorHelper;

/**
 * Utility class for JVM APIs.
 */
public final class Extensions {
    /**
     * {@code true} if the Vector API is available, {@code false} otherwise.
     */
    public static final boolean SIMD_SUPPORTED = isSimdSupported();

    private Extensions() {
    }

    /**
     * Return {@code true} if the Vector API is available, {@code false} otherwise.
     *
     * @return {@code true} if the Vector API is available, {@code false} otherwise.
     */
    private static boolean isSimdSupported() {
        try {
            // VectorHelper is compiled for Java 17, and loads only with the module jdk.incubator.vector
            Class.forName(VectorHelper.class.getName());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
