package com.example.day13du;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.ArcOptions;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private boolean isPermissionRequested;
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private InfoWindow mInfoWindow;
    private LocationClient mLocationClient;
    private LatLng mMySelfLocation;
    /**
     * 请输入搜索的内容
     */
    private EditText mEtKey;
    /**
     * 检索
     */
    private Button mBtnSeach;
    private PoiSearch mPoiSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
//        normal();
        local();//定位

        requestPermission();//危险权限，动态授权
        //创建POI检索实例
        mPoiSearch = PoiSearch.newInstance();
        //设置检索监听器
        mPoiSearch.setOnGetPoiSearchResultListener(listener);
    }
    //创建POI检索监听器
    OnGetPoiSearchResultListener listener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {
            if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
                mBaiduMap.clear();
                //创建PoiOverlay对象
                PoiOverlay poiOverlay = new PoiOverlay(mBaiduMap);
                //设置Poi检索数据
                poiOverlay.setData(poiResult);
                //将poiOverlay添加至地图并缩放至合适级别
                poiOverlay.addToMap();
                poiOverlay.zoomToSpan();

                List<PoiInfo> allPoi = poiResult.getAllPoi();//得到所有检索到的结果集合
                for (int i = 0; i < allPoi.size(); i++) {//遍历
                    PoiInfo poiInfo = allPoi.get(i);//得到每一个POI
                    String address = poiInfo.address;//地址
                    String name = poiInfo.name;//名字
                    Log.i("222", name + "-------" + address);
                    //练习。。。。。。。。。。。。。。。。。。。。。。。
                }

            }
        }
        @Override
        public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {

        }
        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }
        //废弃
        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

        }
    };

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
            case R.id.btn_seach:
                startSeach();//开始检索
                break;
        }
    }

    private void startSeach() {
        String key = mEtKey.getText().toString().trim();
//        mPoiSearch.searchInCity(new PoiCitySearchOption()
//                .city("银川") //必填
//                .keyword(key) //必填
//                .pageNum(10));

        /**
         * 以我为中心，搜索半径100米以内的餐厅
         */
        mPoiSearch.searchNearby(new PoiNearbySearchOption()
                .location(mMySelfLocation)
                .radius(5000)
                .keyword(key)
                .pageNum(10));//数量

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
            mMySelfLocation = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate status2 = MapStatusUpdateFactory.newLatLng(mMySelfLocation);
            mBaiduMap.setMapStatus(status2);
        }
    }

    private void normal() {
        addMarker();//添加标记
        addLine();
        addArc();//画弧
        addText();//添加文本

    }

    private void addText() {
        //文字覆盖物位置坐标
//        LatLng llText = new LatLng(39.86923, 116.397428);
//
////构建TextOptions对象
//        OverlayOptions mTextOptions = new TextOptions()
//                .text("我好帅") //文字内容
//                .bgColor(0xAAFFFF00) //背景色
//                .fontSize(120) //字号
//                .fontColor(0xFFFF00FF) //文字颜色
//                .rotate(30) //旋转角度
//                .position(llText);
//
////在地图上显示文字覆盖物
//        Overlay mText = mBaiduMap.addOverlay(mTextOptions);

        //用来构造InfoWindow的Button
        Button button = new Button(getApplicationContext());
        button.setBackgroundResource(R.drawable.popup);
        button.setText("InfoWindow");
        LatLng point = new LatLng(39.963175, 116.400244);

//构造InfoWindow
//point 描述的位置点
//-100 InfoWindow相对于point在y轴的偏移量
        mInfoWindow = new InfoWindow(button, point, -100);

//使InfoWindow生效
        mBaiduMap.showInfoWindow(mInfoWindow);
    }

    private void addArc() {
        // 添加弧线坐标数据
        LatLng p1 = new LatLng(39.97923, 116.357428);//起点
        LatLng p2 = new LatLng(39.94923, 116.397428);//中间点
        LatLng p3 = new LatLng(39.97923, 116.437428);//终点

//构造ArcOptions对象
        OverlayOptions mArcOptions = new ArcOptions()
                .color(Color.RED)
                .width(10)
                .points(p1, p2, p3);

//在地图上显示弧线
        Overlay mArc = mBaiduMap.addOverlay(mArcOptions);
    }

    private void addLine() {
//        //构建折线点坐标
//        LatLng p1 = new LatLng(39.97923, 116.357428);
//        LatLng p2 = new LatLng(39.94923, 116.397428);
//        LatLng p3 = new LatLng(39.97923, 116.437428);
//        List<LatLng> points = new ArrayList<LatLng>();
//        points.add(p1);
//        points.add(p2);
//        points.add(p3);
//
////设置折线的属性
//        OverlayOptions mOverlayOptions = new PolylineOptions()
//                .width(10)
//                .color(0xAAFF0000)
//                .points(points)
//                .dottedLine(true);//添加 是虚线
////在地图上绘制折线
////mPloyline 折线对象
//        Overlay mPolyline = mBaiduMap.addOverlay(mOverlayOptions);

        //构建折线点坐标
        List<LatLng> points = new ArrayList<LatLng>();
        points.add(new LatLng(39.965, 116.404));
        points.add(new LatLng(39.925, 116.454));
        points.add(new LatLng(39.955, 116.494));
        points.add(new LatLng(39.905, 116.554));
        points.add(new LatLng(39.965, 116.604));

        List<Integer> colors = new ArrayList<>();
        colors.add(Integer.valueOf(Color.BLUE));
        colors.add(Integer.valueOf(Color.RED));
        colors.add(Integer.valueOf(Color.YELLOW));
        colors.add(Integer.valueOf(Color.GREEN));

//设置折线的属性
        OverlayOptions mOverlayOptions = new PolylineOptions()
                .width(10)
                .color(0xAAFF0000)
                .points(points)
                .colorsValues(colors);//设置每段折线的颜色

//在地图上绘制折线
//mPloyline 折线对象
        Overlay mPolyline = mBaiduMap.addOverlay(mOverlayOptions);
    }

    private void addMarker() {
        //定义Maker坐标点
        final LatLng point = new LatLng(39.963175, 116.400244);
        //构建Marker图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);
        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions()
                .position(point)//位置
                .icon(bitmap)//图标
                .draggable(true);//可拖拽
//在地图上添加Marker，并显示
        mBaiduMap.addOverlay(option);

        //Marker点击事件监听
        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                LatLng position = marker.getPosition();//得到点击的图标的经纬度
                double lon = position.longitude;//经度
                double la = position.latitude;//维度
                Toast.makeText(MainActivity.this, "点击了图标，经度：" + lon + ",维度：" + la, Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        //Marker拖拽事件
        mBaiduMap.setOnMarkerDragListener(new BaiduMap.OnMarkerDragListener() {

            //在Marker拖拽过程中回调此方法，这个Marker的位置可以通过getPosition()方法获取
            //marker 被拖动的Marker对象
            @Override
            public void onMarkerDrag(Marker marker) {
                //对marker处理拖拽逻辑
            }

            //在Marker拖动完成后回调此方法， 这个Marker的位可以通过getPosition()方法获取
            //marker 被拖拽的Marker对象
            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng position = marker.getPosition();//得到点击的图标的经纬度
                double lon = position.longitude;//经度
                double la = position.latitude;//维度
                Toast.makeText(MainActivity.this, "拖拽了图标，最后停在 经度：" + lon + ",维度：" + la, Toast.LENGTH_SHORT).show();
            }

            //在Marker开始被拖拽时回调此方法， 这个Marker的位可以通过getPosition()方法获取
            //marker 被拖拽的Marker对象
            @Override
            public void onMarkerDragStart(Marker marker) {

            }
        });

    }

    private void initView() {
        //得到地图组件对象
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();//得到具体的地图
        //设置地图的缩放级别：
//        MapStatus.Builder builder = new MapStatus.Builder();
//        builder.zoom(18.0f);
//        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

        //设置上海为地图的中心
//        LatLng GEO_SHANGHAI = new LatLng(31.227, 121.481);//上海坐标
        //上海为地图中心
//        MapStatusUpdate status2 = MapStatusUpdateFactory.newLatLng(GEO_SHANGHAI);
//        mBaiduMap.setMapStatus(status2);
        //普通地图 ,mBaiduMap是地图控制器对象
//        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        //卫星地图
//        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
        //空白地图
//        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NONE);
        //开启交通图
//        mBaiduMap.setTrafficEnabled(true);
        //开启热力图
//        mBaiduMap.setBaiduHeatMapEnabled(true);


        mEtKey = (EditText) findViewById(R.id.et_key);
        mBtnSeach = (Button) findViewById(R.id.btn_seach);
        mBtnSeach.setOnClickListener(this);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mPoiSearch.destroy();//销毁检索
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
                    Manifest.permission.ACCESS_FINE_LOCATION
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


}
