package com.example.epark;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import com.baidu.navisdk.adapter.BNCommonSettingParam;
import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
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

    //导航相关
    public static List<Activity> activityList = new LinkedList<Activity>();

    private static final String APP_FOLDER_NAME = "BNSDKSimpleDemo-example-epark";

    public static final String ROUTE_PLAN_NODE = "routePlanNode";

    private final static String authBaseArr[] =
            { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION };
    private final static String authComArr[] = { Manifest.permission.READ_PHONE_STATE };
    private final static int authBaseRequestCode = 1;
    private final static int authComRequestCode = 2;
    private String mSDCardPath = null;
    private String markerName = null;
    private boolean hasInitSuccess = false;
    private boolean hasRequestComAuth = false;

    private LatLng mLastLocationData;
    private LatLng mDestinationData;
    private Button mBtnMockNav;
    private Button mBtnRealNav;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        activityList.add(this);
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

        //初始化导航相关
        if (initDirs()) {
            initNavi();
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

                markerName = info.getName();

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

                //获取目标地点坐标
                mDestinationData = new LatLng(info.getLatitude(), info.getLongitude());

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

    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    String authinfo = null;

    /**
     * 内部TTS播报状态回传handler
     */
    private Handler ttsHandler = new Handler() {
        public void handleMessage(Message msg) {
            int type = msg.what;
            switch (type) {
                case BaiduNaviManager.TTSPlayMsgType.PLAY_START_MSG: {
                    // showToastMsg("Handler : TTS play start");
                    break;
                }
                case BaiduNaviManager.TTSPlayMsgType.PLAY_END_MSG: {
                    // showToastMsg("Handler : TTS play end");
                    break;
                }
                default:
                    break;
            }
        }
    };

    /**
     * 内部TTS播报状态回调接口
     */
    private BaiduNaviManager.TTSPlayStateListener ttsPlayStateListener = new BaiduNaviManager.TTSPlayStateListener() {

        @Override
        public void playEnd() {
            // showToastMsg("TTSPlayStateListener : TTS play end");
        }

        @Override
        public void playStart() {
            // showToastMsg("TTSPlayStateListener : TTS play start");
        }
    };

    private boolean hasBasePhoneAuth() {
        // TODO Auto-generated method stub

        PackageManager pm = this.getPackageManager();
        for (String auth : authBaseArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCompletePhoneAuth() {
        // TODO Auto-generated method stub

        PackageManager pm = this.getPackageManager();
        for (String auth : authComArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void initNavi() {

        BNOuterTTSPlayerCallback ttsCallback = null;

        // 申请权限
        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (!hasBasePhoneAuth()) {

                this.requestPermissions(authBaseArr, authBaseRequestCode);
                return;

            }
        }

        BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME, new BaiduNaviManager.NaviInitListener() {
            @Override
            public void onAuthResult(int status, String msg) {
                if (0 == status) {
                    authinfo = "key校验成功!";
                } else {
                    authinfo = "key校验失败, " + msg;
                }
                MapMainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MapMainActivity.this, authinfo, Toast.LENGTH_LONG).show();
                    }
                });
            }

            public void initSuccess() {
                Toast.makeText(MapMainActivity.this, "百度导航引擎初始化成功", Toast.LENGTH_SHORT).show();
                hasInitSuccess = true;
                initSetting();
            }

            public void initStart() {
                Toast.makeText(MapMainActivity.this, "百度导航引擎初始化开始", Toast.LENGTH_SHORT).show();
            }

            public void initFailed() {
                Toast.makeText(MapMainActivity.this, "百度导航引擎初始化失败", Toast.LENGTH_SHORT).show();
            }

        }, null, ttsHandler, ttsPlayStateListener);

    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
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
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
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
        mBtnMockNav = (Button) findViewById(R.id.id_btn_route);
        mBtnRealNav = (Button) findViewById(R.id.id_btn_navi);

        mBaiduMap = mMapView.getMap();
        //开启定位
        MapStatusUpdate update = MapStatusUpdateFactory.zoomTo(15.0f);
        mBaiduMap.setMapStatus(update);

        mLocationBtn.setOnClickListener(this);
        searchBtn.setOnClickListener(this);
        mBtnMockNav.setOnClickListener(this);
        mBtnRealNav.setOnClickListener(this);
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

                if (requestCode == authBaseRequestCode) {
                    for (int ret : grantResults) {
                        if (ret == 0) {
                            continue;
                        } else {
                            Toast.makeText(this, "缺少导航基本的权限!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    initNavi();
                } else if (requestCode == authComRequestCode) {
                    for (int ret : grantResults) {
                        if (ret == 0) {
                            continue;
                        }
                    }
                    routeplanToNavi(mCoordinateType, false);
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
            case R.id.id_btn_route:
                if (BaiduNaviManager.isNaviInited()) {
                    routeplanToNavi(BNRoutePlanNode.CoordinateType.BD09LL, false);
                }
                break;
            case R.id.id_btn_navi:
                if (BaiduNaviManager.isNaviInited()) {
                    routeplanToNavi(BNRoutePlanNode.CoordinateType.BD09LL, true);
                }
                break;
            default:
                break;
        }
    }

    private BNRoutePlanNode.CoordinateType mCoordinateType = null;

    private void routeplanToNavi(BNRoutePlanNode.CoordinateType coType, boolean mockOrNot) {
        mCoordinateType = coType;
        if (!hasInitSuccess) {
            Toast.makeText(MapMainActivity.this, "还未初始化!", Toast.LENGTH_SHORT).show();
        }
        // 权限申请
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            // 保证导航功能完备
            if (!hasCompletePhoneAuth()) {
                if (!hasRequestComAuth) {
                    hasRequestComAuth = true;
                    this.requestPermissions(authComArr, authComRequestCode);
                    return;
                } else {
                    Toast.makeText(MapMainActivity.this, "没有完备的权限!", Toast.LENGTH_SHORT).show();
                }
            }

        }
        BNRoutePlanNode sNode = null;
        BNRoutePlanNode eNode = null;

        switch (coType) {
            case GCJ02: {
                sNode = new BNRoutePlanNode(mLongitude, mLatitude, "当前位置", null, coType);
                eNode = new BNRoutePlanNode(mDestinationData.longitude, mDestinationData.latitude, markerName, null, coType);
                break;
            }
            case WGS84: {
                sNode = new BNRoutePlanNode(mLongitude, mLatitude, "当前位置", null, coType);
                eNode = new BNRoutePlanNode(mDestinationData.longitude, mDestinationData.latitude, markerName, null, coType);
                break;
            }
            case BD09_MC: {
                sNode = new BNRoutePlanNode(mLongitude, mLatitude, "当前位置", null, coType);
                eNode = new BNRoutePlanNode(mDestinationData.longitude, mDestinationData.latitude, markerName, null, coType);
                break;
            }
            case BD09LL: {
                sNode = new BNRoutePlanNode(mLongitude, mLatitude, "当前位置", null, coType);
                eNode = new BNRoutePlanNode(mDestinationData.longitude, mDestinationData.latitude, markerName, null, coType);
                break;
            }
            default:
                break;
        }
        if (sNode != null && eNode != null) {
            List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
            list.add(sNode);
            list.add(eNode);

            // 开发者可以使用旧的算路接口，也可以使用新的算路接口,可以接收诱导信息等
            // BaiduNaviManager.getInstance().launchNavigator(this, list, 1, true, new DemoRoutePlanListener(sNode));
            BaiduNaviManager.getInstance().launchNavigator(this, list, 1, mockOrNot, new DemoRoutePlanListener(sNode),
                    eventListerner);
        }
    }

    BaiduNaviManager.NavEventListener eventListerner = new BaiduNaviManager.NavEventListener() {

        @Override
        public void onCommonEventCall(int what, int arg1, int arg2, Bundle bundle) {
            BNEventHandler.getInstance().handleNaviEvent(what, arg1, arg2, bundle);
        }
    };

    public class DemoRoutePlanListener implements BaiduNaviManager.RoutePlanListener {

        private BNRoutePlanNode mBNRoutePlanNode = null;

        public DemoRoutePlanListener(BNRoutePlanNode node) {
            mBNRoutePlanNode = node;
        }

        @Override
        public void onJumpToNavigator() {
            /*
             * 设置途径点以及resetEndNode会回调该接口
             */

            for (Activity ac : activityList) {

                if (ac.getClass().getName().endsWith("BNDemoGuideActivity")) {

                    return;
                }
            }
            Intent intent = new Intent(MapMainActivity.this, BNDemoGuideActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(ROUTE_PLAN_NODE, (BNRoutePlanNode) mBNRoutePlanNode);
            intent.putExtras(bundle);
            startActivity(intent);

        }

        @Override
        public void onRoutePlanFailed() {
            // TODO Auto-generated method stub
            Toast.makeText(MapMainActivity.this, "算路失败", Toast.LENGTH_SHORT).show();
        }
    }




    private void initSetting() {
        // BNaviSettingManager.setDayNightMode(BNaviSettingManager.DayNightMode.DAY_NIGHT_MODE_DAY);
        BNaviSettingManager
                .setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
        BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Veteran);
        // BNaviSettingManager.setPowerSaveMode(BNaviSettingManager.PowerSaveMode.DISABLE_MODE);
        BNaviSettingManager.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_ON);
        BNaviSettingManager.setIsAutoQuitWhenArrived(true);
        Bundle bundle = new Bundle();
        // 必须设置APPID，否则会静音
        bundle.putString(BNCommonSettingParam.TTS_APP_ID, "10119486");
        BNaviSettingManager.setNaviSdkParam(bundle);
    }

    private BNOuterTTSPlayerCallback mTTSCallback = new BNOuterTTSPlayerCallback() {

        @Override
        public void stopTTS() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "stopTTS");
        }

        @Override
        public void resumeTTS() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "resumeTTS");
        }

        @Override
        public void releaseTTSPlayer() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "releaseTTSPlayer");
        }

        @Override
        public int playTTSText(String speech, int bPreempt) {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "playTTSText" + "_" + speech + "_" + bPreempt);

            return 1;
        }

        @Override
        public void phoneHangUp() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "phoneHangUp");
        }

        @Override
        public void phoneCalling() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "phoneCalling");
        }

        @Override
        public void pauseTTS() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "pauseTTS");
        }

        @Override
        public void initTTSPlayer() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "initTTSPlayer");
        }

        @Override
        public int getTTSState() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "getTTSState");
            return 1;
        }
    };
}
