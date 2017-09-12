package com.example.epark;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;

import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;

import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MapMainActivity extends AppCompatActivity implements View.OnClickListener {

    private MapView mMapView;
    private BaiduMap mBaiduMap;

    //定位相关
    private LocationClient mLocationClient;
    private BDLocationListener mLocationListener;
    private double mLatitude;
    private double mLongitude;
    private boolean isFirstLocate = true;
    private BitmapDescriptor mIconLocation; //初始化定位图标
    private MyOrientationListener myOrientationListener;
    private float mCurrentX;
    private MyLocationConfiguration.LocationMode mLocationMode;
    private Button mLocationBtn;
    private Button searchBtn;

    //覆盖物相关
    private BitmapDescriptor mMarker;
    private RelativeLayout mMarkerLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map_main);

        initView();
        initLocation();
        initMarker();
        //创建集合permissionList进行权限申请
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MapMainActivity.this, Manifest.
                permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MapMainActivity.this, Manifest.
                permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MapMainActivity.this, Manifest.
                permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MapMainActivity.this, permissions, 1);
        } else {
            requestLocation();
        }

        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Bundle extraInformation = marker.getExtraInfo();
                LocationInfo info = (LocationInfo) extraInformation.getSerializable("info");
                ImageView iv = (ImageView) mMarkerLayout.findViewById(R.id.id_img_parkinglot);
                TextView name = (TextView) mMarkerLayout.findViewById(R.id.id_info_name);
                TextView distance = (TextView) mMarkerLayout.findViewById(R.id.id_info_distance);
                TextView parkingTotalNum = (TextView) mMarkerLayout.findViewById(R.id.id_info_parknum_total);
                TextView parkingSpareNum = (TextView) mMarkerLayout.findViewById(R.id.id_info_parknum_spare);

                iv.setImageResource(info.getImgId());
                name.setText(info.getName());
                distance.setText(info.getDistance());
                parkingTotalNum.setText("总车位: " + info.getParkingTotalNum());
                parkingSpareNum.setText("总车位: " + info.getParkingSpareNum());

                InfoWindow infoWindow;
                TextView tv = new TextView(MapMainActivity.this);
                tv.setBackgroundResource(R.mipmap.location_tips);
                tv.setPadding(30, 20, 30, 50);
                tv.setText(info.getName());
                tv.setTextColor(Color.parseColor("#ffffff"));

                final LatLng latLing = marker.getPosition();
                Point p = mBaiduMap.getProjection().toScreenLocation(latLing);
                p.y -= 47;
                LatLng ll = mBaiduMap.getProjection().fromScreenLocation(p);
                BitmapDescriptor tvBD = BitmapDescriptorFactory.fromView(tv);
                infoWindow = new InfoWindow(tvBD, ll, 0, new InfoWindow.OnInfoWindowClickListener() {
                    @Override
                    public void onInfoWindowClick() {
                        mBaiduMap.hideInfoWindow();
                    }
                });
                mBaiduMap.showInfoWindow(infoWindow);
                mMarkerLayout.setVisibility(View.VISIBLE);
                return true;
            }
        });

        //点击地图上的其他地方，Marker上的信息消失
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mMarkerLayout.setVisibility(View.GONE);
                mBaiduMap.hideInfoWindow();
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                return false;
            }
        });

    }

    private void initMarker() {
        mMarker = BitmapDescriptorFactory.fromResource(R.mipmap.park_marker);
        mMarkerLayout = (RelativeLayout) findViewById(R.id.id_marker_ly);

    }

    private void requestLocation() {
        if (! mLocationClient.isStarted()) {
            //启动定位
            mLocationClient.start();
        }
    }


    private void initLocation() {
        //声明定位图层显示方式
        mLocationMode = MyLocationConfiguration.LocationMode.NORMAL;
        //声明LocationClient类
        mLocationClient = new LocationClient(this);
        mLocationListener = new MyLocationListener();
        //注册监听函数
        mLocationClient.registerLocationListener(mLocationListener);

        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        option.setOpenGps(true);
        option.setScanSpan(1000);
        mLocationClient.setLocOption(option);
        //初始化定位图标
        mIconLocation = new BitmapDescriptorFactory().fromResource(R.mipmap.navi_map_gps_locked);

        myOrientationListener = new MyOrientationListener(this);
        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });
    }

    private void initView() {
        mMapView = (MapView) findViewById(R.id.id_bmapView);
        mLocationBtn = (Button) findViewById(R.id.btn_my_location);
        searchBtn = (Button) findViewById(R.id.btn_search);

        mBaiduMap = mMapView.getMap();
        //开启定位
        MapStatusUpdate update = MapStatusUpdateFactory.zoomTo(15.0f);
        mBaiduMap.setMapStatus(update);

        mLocationBtn.setOnClickListener(this);
        searchBtn.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        //启动方向传感器
        myOrientationListener.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //关闭定位图层并停止定位
        mBaiduMap.setMyLocationEnabled(false);
        mLocationClient.stop();
        //停止方向传感器
        myOrientationListener.stop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }


    /**
     * 通过循环将申请的每个权限进行判断，如果有任何一个权限被拒绝，就直接调用finish()方法关闭当前程序
     * 只有当所有权限都被用户同意了，才会调用requestLocation()开始定位
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意所以权限才能使用本程序", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    private class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            // 构造定位数据
            MyLocationData data = new MyLocationData.Builder()
                    .direction(mCurrentX)
                    .accuracy(bdLocation.getRadius())
                    .latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude())
                    .build();
            //设置定位数据
            mBaiduMap.setMyLocationData(data);
            //设置自定义定位图标
            MyLocationConfiguration config = new MyLocationConfiguration(mLocationMode, true, mIconLocation);
            mBaiduMap.setMyLocationConfiguration(config);
            //记录初始位置的经纬度
            mLatitude = bdLocation.getLatitude();
            mLongitude = bdLocation.getLongitude();

            if (isFirstLocate) {
                LatLng ll = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
                mBaiduMap.animateMapStatus(update);
                isFirstLocate = false;
            }
        }
    }

    /**
     * 添加覆盖物
     * @param infos
     */
    private void addOverlays(List<LocationInfo> infos) {
        mBaiduMap.clear();
        LatLng ll = null;
        Marker marker = null;
        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions options;
        for (LocationInfo info : infos) {
            //定义Marker经纬度
            ll = new LatLng(info.getLatitude(), info.getLongitude());
            //构建Marker图标
            options = new MarkerOptions().position(ll).icon(mMarker).zIndex(5);
            //在地图上添加Marker，并显示
            marker = (Marker) mBaiduMap.addOverlay(options);
            Bundle extraInfo = new Bundle();
            extraInfo.putSerializable("info", info);
            marker.setExtraInfo(extraInfo);
        }
        MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
        mBaiduMap.setMapStatus(update);
    }

    /**
     * 定位到我的位置
     */
    private void centerToMyLocation() {
        LatLng ll = new LatLng(mLatitude, mLongitude);
        MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
        mBaiduMap.animateMapStatus(update);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.id_map_common:
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                break;
            case R.id.id_map_site:
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.id_map_traffic:
                if (mBaiduMap.isTrafficEnabled()) {
                    mBaiduMap.setTrafficEnabled(false);
                    item.setTitle("实时交通(off)");
                } else {
                    mBaiduMap.setTrafficEnabled(true);
                    item.setTitle("实时交通(on)");
                }
                break;
            case R.id.id_map_mode_normal:
                mLocationMode = MyLocationConfiguration.LocationMode.NORMAL;
                break;
            case R.id.id_map_mode_following:
                mLocationMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                break;
            case R.id.id_map_mode_compass:
                mLocationMode = MyLocationConfiguration.LocationMode.COMPASS;
                break;
            default:
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_my_location:
                centerToMyLocation();
                break;
            case R.id.btn_search:
                addOverlays(LocationInfo.infos);
                break;
            default:
                break;
        }
    }
}
