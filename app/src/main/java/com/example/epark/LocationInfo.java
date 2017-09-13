package com.example.epark;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 小言 on 2017/9/12.
 */

public class LocationInfo implements Serializable {

    private static final long serialVersionUID = -2033734155801636071L;

    private double latitude;
    private double longitude;
    private int imgId;
    private String name;
    private String distance;
    private int parkingTotalNum;
    private int parkingSpareNum;

    public static List<LocationInfo> infos = new ArrayList<LocationInfo>();

    static {
        infos.add(new LocationInfo(22.587779,113.972414, R.mipmap.parkinglot01, "丽山路",
                "距离1.7公里", 22, 16 ));
        infos.add(new LocationInfo(22.592598,113.973089, R.mipmap.parkinglot01, "平山一路",
                "距离760米", 103, 20));
        infos.add(new LocationInfo(22.593153,113.974791, R.mipmap.parkinglot01, "校园路",
                "距离149米", 30, 10));
        infos.add(new LocationInfo(22.595105,113.975478, R.mipmap.parkinglot01, "学苑大道",
                "距离229米", 72, 47));
        infos.add(new LocationInfo(22.593636,113.977145, R.mipmap.parkinglot01, "哈工大G座",
                "距离60米", 20, 11));
        infos.add(new LocationInfo(22.592987,113.972961, R.mipmap.parkinglot01, "桑泰丹华",
                "距离879米", 55, 23));

    }

    public LocationInfo(double latitude, double longitude, int imgId, String name, String distance, int parkingTotalNum, int parkingSpareNum) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.imgId = imgId;
        this.name = name;
        this.distance = distance;
        this.parkingTotalNum = parkingTotalNum;
        this.parkingSpareNum = parkingSpareNum;
    }



    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getImgId() {
        return imgId;
    }

    public void setImgId(int imgId) {
        this.imgId = imgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public int getParkingTotalNum() {
        return parkingTotalNum;
    }

    public void setParkingTotalNum(int parkingTotalNum) {
        this.parkingTotalNum = parkingTotalNum;
    }

    public int getParkingSpareNum() {
        return parkingSpareNum;
    }

    public void setParkingSpareNum(int parkingSpareNum) {
        this.parkingSpareNum = parkingSpareNum;
    }

}
