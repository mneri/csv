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
