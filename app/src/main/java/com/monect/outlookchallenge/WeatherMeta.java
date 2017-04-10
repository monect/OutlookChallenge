package com.monect.outlookchallenge;

/**
 * Created by Monect on 12/10/2016.
 */

class WeatherMeta {

    String mWeatherTime;
    String mWeatherIcon;
    double mTemperature;

    WeatherMeta(String weatherTime, String weatherIcon, double temperature) {
        mWeatherTime = weatherTime;
        mWeatherIcon = weatherIcon;
        mTemperature = temperature;

    }
}
