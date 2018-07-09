package com.solar.sarthak.solarcalculator.models;

public class Place {

    private String name;
    private String latitude;
    private String longitude;
    private String favorite;

    public Place(String name, String latitude, String longitude, String favorite) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.favorite = favorite;
    }

    public String getName() {
        return name;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getFavorite() {
        return favorite;
    }
}
