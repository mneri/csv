package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvSerializer;
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
