package com.example.cabbooking.models;

import androidx.annotation.NonNull;

public class Driver {
    private long id;
    public double lat;
    public double lng;
    public float angle;

    public Driver(long id, double lat, double lng, float angle) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.angle = angle;
    }

    public Driver() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void update(double lat, double lng, float angle) {
        this.lat = lat;
        this.lng = lng;
        this.angle = angle;
    }

    @NonNull
    @Override
    public String toString() {
        return "Driver{" +
                "id='" + id + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                ", angle=" + angle +
                '}';
    }
}
