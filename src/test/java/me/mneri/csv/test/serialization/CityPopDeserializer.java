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

package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvDeserializer;
import me.mneri.csv.RecyclableCsvLine;
import me.mneri.csv.test.model.CityPop;

public class CityPopDeserializer implements CsvDeserializer<CityPop> {
    @Override
    public CityPop deserialize(RecyclableCsvLine line) {
        CityPop city = new CityPop();

        //@formatter:off
        city.setCountry(line.getString(0));
        city.setCity(line.getString(1));
        city.setAccentCity(line.getString(2));
        city.setRegion(line.getString(3));
        try { city.setPopulation(Integer.parseInt(line.getString(4))); }  catch (Exception ignored) { }
        try { city.setLatitude(Double.parseDouble(line.getString(5))); }  catch (Exception ignored) { }
        try { city.setLongitude(Double.parseDouble(line.getString(6))); } catch (Exception ignored) { }
        //@formatter:on

        return city;
    }
}
