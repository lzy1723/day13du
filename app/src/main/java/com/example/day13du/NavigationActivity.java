package com.example.day13du;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;

import java.util.ArrayList;

public class NavigationActivity extends AppCompatActivity implements View.OnClickListener {

    private boolean isPermissionRequested;
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private LocationClient mLocationClient;
    private LatLng mStart;//导航的开始，就是定位的我的当前位置
    private LatLng mEnd;//导航的结束位置，通过点击获得
    /**
     * 开始导航
     */
    private Button mBtnNavi;
    private WalkNaviLaunchParam mParam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        requestPermission();

        initView();
        //开始定位
        local();


    }

    /**
     * Android6.0之后需要动态申请权限
     */
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {
            isPermissionRequested = true;
            ArrayList<String> permissionsList = new ArrayList<>();
            String[] permissions = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.VIBRATE,
                    Manifest.permission.CAMERA
            };
            for (String perm : permissions) {
                if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(perm)) {
                    permissionsList.add(perm);
                    // 进入到这里代表没有权限.
                }
            }

            if (permissionsList.isEmpty()) {
                return;
            } else {
                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), 0);
            }
        }
    }

    private void initView() {
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();

        mBtnNavi = (Button) findViewById(R.id.btn_navi);
        mBtnNavi.setOnClickListener(this);

        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //得到点击的位置的经纬度,,在地图上进行标记，打marker
                addMarker(latLng);
                mEnd = latLng;
            }

            @Override
            public void onMapPoiClick(MapPoi mapPoi) {

            }
        });
    }
    private void addMarker(LatLng point) {
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);
        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions()
                .position(point)//位置
                .icon(bitmap)//图标
                .draggable(true);//可拖拽
//在地图上添加Marker，并显示
        mBaiduMap.addOverlay(option);
    }

    private void local() {
        mBaiduMap.setMyLocationEnabled(true);//开启地图的定位图层
        //定位初始化
        mLocationClient = new LocationClient(this);
//通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);//定位的频率  1000毫秒定位一次
//设置locationClientOption
        mLocationClient.setLocOption(option);
//注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
//开启地图定位图层
        mLocationClient.start();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.btn_navi:
                startNavi();
                break;
        }
    }
//开始导航
    private void startNavi() {
        // 获取导航控制类
// 引擎初始化
        WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {

            @Override
            public void engineInitSuccess() {
                //引擎初始化成功的回调，发起算法
                routeWalkPlanWithParam();
            }

            @Override
            public void engineInitFail() {
                //引擎初始化失败的回调
            }
        });


    }

    private void routeWalkPlanWithParam() {
        //发起算路
        mParam = new WalkNaviLaunchParam().stPt(mStart).endPt(mEnd);
        //发起算路
        WalkNavigateHelper.getInstance().routePlanWithParams(mParam, new IWRoutePlanListener() {
                    @Override
                    public void onRoutePlanStart() {
                        //开始算路的回调
                    }

                    @Override
                    public void onRoutePlanSuccess() {
                        //算路成功
                        //跳转至诱导页面
                        Intent intent = new Intent(NavigationActivity.this, WNaviGuideActivity.class);
                        startActivity(intent);
                    }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError walkRoutePlanError) {
                //算路失败的回调
            }
        });
    }

    //通过继承抽象类BDAbstractListener并重写其onReceieveLocation方法来获取定位数据，并将其传给MapView。
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            Log.i("111", "我的位置: " + location.getLongitude() + "," + location.getLatitude());
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection())
                    .latitude(location.getLatitude())//维度
                    .longitude(location.getLongitude())//经度
                    .build();
            mBaiduMap.setMyLocationData(locData);

            //把我的位置拉倒地图的中心
            //得到我的坐标，设置为全局，供POI检索使用
            mStart = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate status2 = MapStatusUpdateFactory.newLatLng(mStart);
            mBaiduMap.setMapStatus(status2);
        }
    }
}
