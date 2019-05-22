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

package me.mneri.csv;

import java.lang.reflect.Field;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A {@link CsvSerializer} to serialize all (non <b>transient</b>) fields of an Object using {@link java.lang.reflect}
 * package. <b>Getter methods</b> will be invoked in order to get the values of the fields. <b>Transient</b> fields will
 * be ignored.
 * @see DefaultCsvDeserializer
 * 
 * @author George Zougianos <a href="https://github.com/gzougianos">github.com/gzougianos</a>
 *
 * @param <T> The type of the Java objects to serialize.
 */
public class DefaultCsvSerializer<T> implements CsvSerializer<T> {
	private List<Method> getters;

	public DefaultCsvSerializer(Class<T> clazz) {
		getters = new ArrayList<>();
		Field[] fields = clazz.getDeclaredFields();
		final Method[] methods = clazz.getMethods();
		for (Field f : fields) {
			f.setAccessible(true); // Need to access private field
			if (Modifier.isTransient(f.getModifiers())) // Skip Transient fields
				continue;
			findGetter(methods, f.getName());
		}
	}

	private void findGetter(Method[] methods, String fieldName) {
		for (Method m : methods) {
			if (m.getName().equalsIgnoreCase("get" + fieldName))
				getters.add(m);
		}
	}

	@Override
	public void serialize(T object, List<String> out) throws Exception {
		for (Method getter : getters) {
			Object obj = getter.invoke(object);
			String value = "";
			String returnType = getter.getReturnType().getName().toString();
			if (obj != null) {
				switch (returnType.toLowerCase()) {
				case "java.util.date": // If it is a date, save it as timestamp
					Date d = (Date) obj;
					value = Long.toString(d.getTime());
					break;
				default:
					value = obj.toString();
				}
			}
			out.add(value.equalsIgnoreCase("null") ? "" : value);
		}
	}
}
