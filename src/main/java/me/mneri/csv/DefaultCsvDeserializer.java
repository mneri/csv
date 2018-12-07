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
 * A {@link CsvDeserializer} to deserialize objects after they have been serialized with {@link DefaultCsvSerializer}.
 * Values are passed to the fields using the <b>setters</b> of the class. <b>Transient</b> fields will be ignored.
 * 
 * @author George Zougianos <a href="https://github.com/gzougianos">github.com/gzougianos</a>
 *
 * @param <T> The type of the Java objects to deserialize.
 */
public class DefaultCsvDeserializer<T> implements CsvDeserializer<T> {
	private List<Method> setters;
	private Class<T> clazz;

	public DefaultCsvDeserializer(Class<T> clazz) {
		this.clazz = clazz;
		setters = new ArrayList<>();
		Field[] fields = clazz.getDeclaredFields();
		final Method[] methods = clazz.getMethods();
		for (Field f : fields) {
			f.setAccessible(true); // Need to access private field
			if (Modifier.isTransient(f.getModifiers())) // Skip Transient fields
				continue;
			findSetter(methods, f.getName());
		}
	}

	private void findSetter(Method[] methods, String fieldName) {
		for (Method m : methods) {
			if (m.getName().equalsIgnoreCase("set" + fieldName))
				setters.add(m);
		}
	}

	@Override
	public T deserialize(RecyclableCsvLine line) throws Exception {
		T obj = clazz.newInstance();
		int lineId = 0;
		for (Method m : setters) {
			// Recognize the type of the value by the argument the setter has.
			String argType = m.getParameterTypes()[0].getSimpleName();
			Object val = null;
			switch (argType.toLowerCase()) {
			case "string":
				val = line.getString(lineId);
				break;
			case "date":
				val = line.getString(lineId);
				if (val != null) {
					long timestamp = Long.parseLong(String.valueOf(val));
					val = new Date(timestamp);
				}
				break;
			case "int":
			case "integer":
				val = line.getInteger(lineId);
				break;
			case "double":
				val = line.getDouble(lineId);
				break;
			case "long":
				val = line.getLong(lineId);
				break;
			case "float":
				val = line.getFloat(lineId);
				break;
			case "short":
				val = line.getShort(lineId);
				break;
			case "biginteger":
				val = line.getBigInteger(lineId);
				break;
			case "bigdecimal":
				val = line.getBigDecimal(lineId);
				break;
			default:
				throw new CsvException("Cannot deserialize value for type " + argType + ".");
			}
			lineId++;
			if (val != null) {
				m.invoke(obj, val);
			}
		}
		return obj;
	}

}
