# android-location-checkin
本工程为基于高德地图Android SDK进行封装，实现了支持微调整位置的定点签到功能
## 前述 ##
- [高德官网申请Key](http://lbs.amap.com/dev/#/).
- 阅读[参考手册](http://a.amap.com/lbs/static/unzip/Android_Map_Doc/index.html).
- 工程基于Android 3D地图SDK和定位SDK实现

## 功能描述 ##
基于3D地图SDK和定位SDK进行封装，高精度定位获取用户位置（开启wifi的，如果没有开启wifi提示开启wifi），确定位置后，给出周边POI列表供用户选择准确位置；支持移动地图调整位置，微调范围限定在500米以内，超过500米时弹出toast告知用户“微调距离不可超过500米”。

## 效果图如下 ##
### APP显示效果###

![Screenshot](https://raw.githubusercontent.com/amap-demo/android-location-checkin/master/apk/picture.jpg)    

## 扫一扫安装##
![Screenshot]( https://raw.githubusercontent.com/amap-demo/android-location-checkin/master/apk/download.png)  

