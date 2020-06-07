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

import me.mneri.csv.serialize.CsvSerializer;
import me.mneri.csv.test.model.CityPop;

import java.util.List;

public class CityPopSerializer implements CsvSerializer<CityPop> {
    @Override
    public void serialize(CityPop city, List<String> out) {
        out.add(city.getCountry());
        out.add(city.getCity());
        out.add(city.getAccentCity());
        out.add(city.getRegion());
        out.add(city.getPopulation() != null ? city.getPopulation().toString() : null);
        out.add(city.getLatitude() != null ? city.getLatitude().toString() : null);
        out.add(city.getLongitude() != null ? city.getLongitude().toString() : null);
    }
}
