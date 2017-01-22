package com.amap.android_location_checkin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements AMap.OnCameraChangeListener, AMap.OnMapLoadedListener, AMapLocationListener, PoiSearch.OnPoiSearchListener, View.OnClickListener {
    private ListView listView;
    private AMap aMap;
    private MapView mapView;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private Marker locationMarker, checkinMarker;
    private LatLonPoint searchLatlonPoint;
    private List<PoiItem> resultData;
    private SearchResultAdapter searchResultAdapter;
    private WifiManager mWifiManager;
    private PoiSearch poisearch;
    private Circle mcircle;
    private LatLng checkinpoint,mlocation;
    private Button locbtn, checkinbtn;
    private boolean isItemClickAction, isLocationAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        resultData = new ArrayList<>();
        init();
        //初始化定位
        initLocation();
        //开始定位
        startLocation();

    }

    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.setOnCameraChangeListener(this);
        aMap.setOnMapLoadedListener(this);

        mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        listView = (ListView) findViewById(R.id.listview);
        searchResultAdapter = new SearchResultAdapter(MainActivity.this);
        searchResultAdapter.setData(resultData);
        listView.setAdapter(searchResultAdapter);

        listView.setOnItemClickListener(onItemClickListener);

        locbtn = (Button)findViewById(R.id.locbtn);
        locbtn.setOnClickListener(this);
        checkinbtn = (Button)findViewById(R.id.checkinbtn);
        checkinbtn.setOnClickListener(this);
    }

    /**
     * 列表点击监听
     */
    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position != searchResultAdapter.getSelectedPosition()) {
                PoiItem poiItem = (PoiItem) searchResultAdapter.getItem(position);
                LatLng curLatlng = new LatLng(poiItem.getLatLonPoint().getLatitude(), poiItem.getLatLonPoint().getLongitude());
                isItemClickAction = true;
                aMap.moveCamera(CameraUpdateFactory.changeLatLng(curLatlng));
                searchResultAdapter.setSelectedPosition(position);
                searchResultAdapter.notifyDataSetChanged();

            }
        }
    };

    /**
     * 初始化定位，设置回调监听
     */
    private void initLocation() {
        //初始化client
        mlocationClient = new AMapLocationClient(this.getApplicationContext());
        // 设置定位监听
        mlocationClient.setLocationListener(this);
    }

    /**
     * 设置定位参数
     * @return 定位参数类
     */
    private AMapLocationClientOption getOption() {
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        mOption.setLocationCacheEnable(false);//设置是否返回缓存中位置，默认是true
        mOption.setOnceLocation(true);//可选，设置是否单次定位。默认是false
        return mOption;
    }

    /**
     * 开始定位
     */
    private void startLocation(){
        checkWifiSetting();
        //设置定位参数
        mlocationClient.setLocationOption(getOption());
        // 启动定位
        mlocationClient.startLocation();
    }

    /**
     * 销毁定位
     *
     */
    private void destroyLocation(){
        if (null != mlocationClient) {
            mlocationClient.onDestroy();
            mlocationClient = null;}
    }

    /**
     * 地图移动过程回调
     * @param cameraPosition
     */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    /**
     * 地图移动结束回调
     * 在这里判断移动距离有无超过500米
     * @param cameraPosition
     */
    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        if (!isItemClickAction && !isLocationAction){
            searchResultAdapter.setSelectedPosition(-1);
            searchResultAdapter.notifyDataSetChanged();
        }
        if (isItemClickAction)
            isItemClickAction = false;
        if (isLocationAction)
            isLocationAction = false;

        if (mcircle != null) {
            if (mcircle.contains(cameraPosition.target)){
                checkinpoint = cameraPosition.target;
            } else{
                Toast.makeText(MainActivity.this, "微调距离不可超过500米", Toast.LENGTH_SHORT).show();
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mlocation ,16f ));
            }
        }else {
            startLocation();
            Toast.makeText(MainActivity.this, "重新定位中。。。", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 地图加载完成回调
     */
    @Override
    public void onMapLoaded() {
        addMarkerInScreenCenter();
    }

    /**
     * 添加选点marker
     */
    private void addMarkerInScreenCenter() {
        LatLng latLng = aMap.getCameraPosition().target;
        Point screenPosition = aMap.getProjection().toScreenLocation(latLng);
        locationMarker = aMap.addMarker(new MarkerOptions()
                .anchor(0.5f,0.5f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.purple_pin)));
        //设置Marker在屏幕上,不跟随地图移动
        locationMarker.setPositionByPixels(screenPosition.x,screenPosition.y);
    }

    /**
     * 检查wifi，并提示用户开启wifi
     */
    private void checkWifiSetting() {
        if (mWifiManager.isWifiEnabled()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);  //先得到构造器
        builder.setTitle("提示"); //设置标题
        builder.setMessage("开启WIFI模块会提升定位准确性"); //设置内容
        builder.setIcon(R.mipmap.ic_launcher);//设置图标，图片id即可
        builder.setPositiveButton("去开启", new DialogInterface.OnClickListener() { //设置确定按钮
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); //关闭dialog
                Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent); // 打开系统设置界面
            }
        });
        builder.setNegativeButton("不了", new DialogInterface.OnClickListener() { //设置取消按钮
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        //参数都设置完成了，创建并显示出来
        builder.create().show();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        destroyLocation();
    }

    /**
     * 返回定位结果的回调
     * @param aMapLocation 定位结果
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null
                && aMapLocation.getErrorCode() == 0) {
            mlocation = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
            searchLatlonPoint = new LatLonPoint(aMapLocation.getLatitude(), aMapLocation.getLongitude());
            checkinpoint = mlocation;
            isLocationAction = true;
            searchResultAdapter.setSelectedPosition(0);
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mlocation, 16f));
            if (mcircle != null) {
                mcircle.setCenter(mlocation);
            } else {
                mcircle = aMap.addCircle(new CircleOptions().center(mlocation).radius(500).strokeWidth(5));
            }
            if (searchLatlonPoint != null) {
                resultData.clear();
                resultData.add(new PoiItem("ID", searchLatlonPoint,"我的位置", searchLatlonPoint.toString()));
                doSearchQuery(searchLatlonPoint);
                searchResultAdapter.notifyDataSetChanged();
            }
        } else {
            String errText = "定位失败," + aMapLocation.getErrorCode()+ ": " + aMapLocation.getErrorInfo();
            Log.e("AmapErr",errText);
        }
    }

    /**
     * 搜索周边poi
     * @param centerpoint
     */
    private void doSearchQuery(LatLonPoint centerpoint) {
        PoiSearch.Query query = new PoiSearch.Query("","","");
        query.setPageSize(20);
        query.setPageNum(0);
        poisearch = new PoiSearch(this,query);
        poisearch.setOnPoiSearchListener(this);
        poisearch.setBound(new PoiSearch.SearchBound(centerpoint, 500, true));
        poisearch.searchPOIAsyn();
    }

    /**
     * 搜索Poi回调
     * @param poiResult 搜索结果
     * @param resultCode 错误码
     */
    @Override
    public void onPoiSearched(PoiResult poiResult, int resultCode) {

        if (resultCode == AMapException.CODE_AMAP_SUCCESS){
            if (poiResult != null && poiResult.getPois().size() > 0){
                List<PoiItem> poiItems = poiResult.getPois();
                resultData.addAll(poiItems);
                searchResultAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(MainActivity.this, "无搜索结果", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "搜索失败，错误 "+resultCode, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ID搜索poi的回调
     * @param poiItem 搜索结果
     * @param resultCode 错误码
     */
    @Override
    public void onPoiItemSearched(PoiItem poiItem, int resultCode) {

    }

    /**
     * Button点击事件
     * @param view
     */
    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.locbtn:
                startLocation();
                break;
            case R.id.checkinbtn:
                checkin();
                break;
            default:
                break;
        }
    }

    /**
     * 顶点签到，将签到点标注在地图上
     */
    private void checkin() {
        if (checkinMarker  == null ){
            if (checkinpoint != null) {
                SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd  hh:mm:ss");
                String date = sDateFormat.format(new java.util.Date());
                checkinMarker = aMap.addMarker(new MarkerOptions().position(checkinpoint).title("签到").snippet(date));
                Toast.makeText(MainActivity.this, "签到成功", Toast.LENGTH_SHORT).show();
            } else{
                startLocation();
                Toast.makeText(MainActivity.this, "请定位后重试，定位中。。。", Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(MainActivity.this, "今日已签到", Toast.LENGTH_SHORT).show();
        }
    }
}
