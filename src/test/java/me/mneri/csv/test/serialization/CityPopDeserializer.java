package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvDeserializer;
import me.mneri.csv.test.model.CityPop;

import java.util.List;

public class CityPopDeserializer implements CsvDeserializer<CityPop> {
    @Override
    public CityPop deserialize(List<String> line) {
        CityPop city = new CityPop();

        //@formatter:off
        city.setCountry(line.get(0));
        city.setCity(line.get(1));
        city.setAccentCity(line.get(2));
        city.setRegion(line.get(3));
        try { city.setPopulation(Integer.parseInt(line.get(4))); }  catch (Exception ignored) { }
        try { city.setLatitude(Double.parseDouble(line.get(5))); }  catch (Exception ignored) { }
        try { city.setLongitude(Double.parseDouble(line.get(6))); } catch (Exception ignored) { }
        //@formatter:on

        return city;
    }
}
