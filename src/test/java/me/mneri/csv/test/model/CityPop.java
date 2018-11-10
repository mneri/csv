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

package me.mneri.csv.test.model;

import java.util.Objects;

public class CityPop implements Cloneable {
    private String accentCity;
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;
    private Integer population;
    private String region;

    @Override
    public CityPop clone() {
        try {
            CityPop clone = (CityPop) super.clone();

            clone.accentCity = accentCity;
            clone.city = city;
            clone.country = country;
            clone.latitude = latitude;
            clone.longitude = longitude;
            clone.population = population;
            clone.region = region;

            return clone;
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (object == null) {
            return false;
        }

        if (!(object instanceof CityPop)) {
            return false;
        }

        CityPop other = (CityPop) object;

        //@formatter:off
        return Objects.equals(getAccentCity(), other.getAccentCity()) &&
               Objects.equals(getCity(),       other.getCity())       &&
               Objects.equals(getCountry(),    other.getCountry())    &&
               Objects.equals(getLatitude(),   other.getLatitude())   &&
               Objects.equals(getLongitude(),  other.getLongitude())  &&
               Objects.equals(getPopulation(), other.getPopulation()) &&
               Objects.equals(getRegion(),     other.getRegion());
        //@formatter:on
    }

    public String getAccentCity() {
        return accentCity;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getPopulation() {
        return population;
    }

    public String getRegion() {
        return region;
    }

    @Override
    public int hashCode() {
        //@formatter:off
        return Objects.hash(getAccentCity(),
                            getCity(),
                            getCountry(),
                            getLatitude(),
                            getLongitude(),
                            getPopulation(),
                            getRegion());
        //@formatter:on
    }

    public void setAccentCity(String accentCity) {
        this.accentCity = accentCity;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setPopulation(Integer population) {
        this.population = population;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public String toString() {
        //@formatter:off
        return String.format("{%s, %s, %s, %s, %d, %f, %f}", getCountry(),
                                                                getCity(),
                                                                getAccentCity(),
                                                                getRegion(),
                                                                getPopulation(),
                                                                getLatitude(),
                                                                getLongitude());
        //@formatter:on
    }
}
