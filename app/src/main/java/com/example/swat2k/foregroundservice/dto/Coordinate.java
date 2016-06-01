package com.example.swat2k.foregroundservice.dto;

import com.google.gson.annotations.SerializedName;

public class Coordinate {
    @SerializedName("lat")
    final double lat;

    @SerializedName("lon")
    final double lon;

    @SerializedName("uuid")
    final String uuid;

    public Coordinate(double lat, double lon, String uuid) {
        this.lat = lat;
        this.lon = lon;
        this.uuid = uuid;
    }

}

