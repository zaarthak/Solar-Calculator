package com.solar.sarthak.solarcalculator.utils;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;
import java.util.Date;

/**
 * Class that implements the Sunrise/sunset algorithm to obtain
 * the time of sunset/sunrise in IST(Indian Standard Time).
 */
public class SolarEventCalculator {

    int timeOfDay;

    int day, month, year;
    double latitude, longitude;

    double sinDec, cosDec;
    double RA;
    double cosZenith = -0.01454;

    Date selectedDate;

    Context mContext;

    public SolarEventCalculator() {}

    public SolarEventCalculator(Context context, int time, Date date, LatLng latLng) {

        mContext = context;

        timeOfDay = time;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        selectedDate = cal.getTime();

        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);

        latitude = latLng.latitude;
        longitude = latLng.longitude;
    }

    public int getDayOfYear() {

        double n1, n2, n3;

        n1 = Math.floor(275 * month / 9);
        n2 = Math.floor((month + 9) / 12);
        n3 = (1 + Math.floor((year - 4 * Math.floor(year / 4) + 2) / 3));

        int dayOfYear = (int) (n1 - (n2 * n3) + day - 30);

        Log.d("dayOfYear", dayOfYear + "");
        return dayOfYear;
    }

    public double getApproximateTime(int time) {

        double lngHour = longitude / 15;

        double approxTime;
        int dayOfYear = getDayOfYear();

        if (time == 0) {

            approxTime = dayOfYear + ((6 - lngHour) / 24);
        } else {

            approxTime = dayOfYear + ((18 - lngHour) / 24);
        }

        Log.d("approxTime", approxTime + "");
        return approxTime;
    }

    public double getSunMeanAnamoly() {

        double approxTime = getApproximateTime(timeOfDay);

        double meanAnamoly = (0.9856 * approxTime) - 3.289;

        Log.d("meanAnamoly", meanAnamoly + "");
        return meanAnamoly;
    }

    public double getSunTrueLongitude() {

        double m = getSunMeanAnamoly();

        double trueLong = m + (1.916 * sineOfRadian(m)) + (0.020 * sineOfRadian(2 * m)) + 282.634;

        if (trueLong < 0) {

            trueLong = trueLong + 360;
        } else if (trueLong >= 360) {

            trueLong = trueLong - 360;
        }

        Log.d("trueLong", trueLong + "");
        return trueLong;
    }

    public double getSunRightAscensionInHour() {

        double trueLong = getSunTrueLongitude();

        RA = atanOfRadian(0.91764 * tanOfRadian(trueLong));

        double Lquad = (Math.floor(trueLong / 90)) * 90;
        double Rquad = (Math.floor(RA / 90)) * 90;

        RA = RA + (Lquad - Rquad);

        RA = RA / 15;

        Log.d("rightAscension", RA + "");
        return RA;
    }

    public void getSunDeclination() {

        sinDec = 0.39782 * sineOfRadian(getSunTrueLongitude());
        cosDec = cosOfRadian(asinOfRadian(sinDec));

        Log.d("sunDeclination", sinDec + " " + cosDec);
    }

    public double getSunLocalHourAngle() {

        getSunDeclination();
        double cosH = (cosZenith - (sinDec * sineOfRadian(latitude))) / (cosDec * cosOfRadian(latitude));

        /*if (cosH > 1) {

        } else if (cosH < -1) {

        }*/

        Log.d("sunLocalHour", cosH + "");
        return cosH;
    }

    public double getHourFromLocalHourAngle(int time) {

        double H;

        if (time == 0) {

            H = 360 - acosOfRadian(getSunLocalHourAngle());
        } else {

            H = acosOfRadian(getSunLocalHourAngle());
        }

        H = H / 15;

        Log.d("hour", H + "");
        return H;
    }

    public double getLocalMeanTime() {

        getSunRightAscensionInHour();

        double H = getHourFromLocalHourAngle(timeOfDay);
        double localTime = H + RA - (0.06571 * getApproximateTime(timeOfDay)) - 6.622;

        Log.d("localMeanTime", localTime + "");
        return localTime;
    }

    public double getIstTime() {

        double lngHour = longitude / 15;

        double utc = getLocalMeanTime() - lngHour;
        Log.d("utc11", utc + "");
        
        if (utc < 0) {

            utc = utc + 24;
        } else if (utc >= 24) {

            utc = utc - 24;
        }

        double ist = (utc + 5.383) % 24;

        return ist;
    }

    private double sineOfRadian(double d) {

        return Math.sin(d * Math.PI / 180);
    }

    private double cosOfRadian(double d) {

        return Math.cos(d * Math.PI / 180);
    }

    private double tanOfRadian(double d) {

        return Math.tan(d * Math.PI / 180);
    }

    private double asinOfRadian(double d) {

        return Math.asin(d) * 180 / Math.PI;
    }

    private double acosOfRadian(double d) {

        return Math.acos(d) * 180 / Math.PI;
    }

    private double atanOfRadian(double d) {

        return Math.atan(d) * 180 / Math.PI;
    }
}
