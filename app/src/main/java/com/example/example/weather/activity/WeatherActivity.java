package com.example.example.weather.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.example.R;
import com.example.example.weather.net.HttpCallbackListener;
import com.example.example.weather.net.HttpUtil;
import com.example.example.weather.service.AutoUpdateService;
import com.example.example.weather.utils.ResponseHandlerUtil;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.NoTitle;
import org.androidannotations.annotations.ViewById;

/*
 * PROJECT_NAME :ExampleSet
 * VERSION :[V 1.0.0]
 * AUTHOR : yulong sun
 * CREATE AT : 7/21/2015 2:01 PM
 * COPYRIGHT : InSigma HengTian Software Ltd.
 * NOTE : 显示天气
 */
@EActivity(R.layout.activity_weather)
@NoTitle
public class WeatherActivity extends ActionBarActivity {

    @ViewById
    TextView tv_city_name; //城市名字
    @ViewById
    TextView tv_temp1;
    @ViewById
    TextView tv_temp2;
    @ViewById
    TextView tv_weather_desp; //用于显示天气描述信息
    @ViewById
    LinearLayout ll_weather_info_layout;
    @ViewById
    TextView tv_current_date;
    @ViewById
    TextView tv_publish_time; //发布时间

    @AfterViews
    void initView(){
        String countyCode = getIntent().getStringExtra("county_code");
        if (!TextUtils.isEmpty(countyCode)) {
            // 有县级代号时就去查询天气
            tv_publish_time.setText("同步中...");
            ll_weather_info_layout.setVisibility(View.INVISIBLE);
            tv_city_name.setVisibility(View.INVISIBLE);
            queryWeatherCode(countyCode);
        } else {
            // 没有县级代号时就直接显示本地天气
            showWeather();
        }
    }

    @Click(R.id.btn_switch_city)
    void switchCity(){
        Intent intent = new Intent(this, ChooseAreaActivity_.class);
        intent.putExtra("from_weather_activity", true);
        startActivity(intent);
        finish();
    }

    @Click( R.id.btn_refresh_weather)
    void refreshWeather(){
        tv_publish_time.setText("同步中...");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherCode = prefs.getString("weather_code", "");
        if (!TextUtils.isEmpty(weatherCode)) {
            queryWeatherInfo(weatherCode);
        }
    }

    /**
     * 查询县级代号所对应的天气代号。
     */
    private void queryWeatherCode(String countyCode) {
        String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
        queryFromServer(address, "countyCode");
    }

    /**
     * 查询天气代号所对应的天气。
     */
    private void queryWeatherInfo(String weatherCode) {
        String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
        queryFromServer(address, "weatherCode");
    }

    /**
     * 根据传入的地址和类型去向服务器查询天气代号或者天气信息。
     */
    private void queryFromServer(final String address, final String type) {
        HttpUtil.sendHttpGetRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(final String response) {
                Log.d("WeatherActivity:", response);

                if ("countyCode".equals(type)) {
                    if (!TextUtils.isEmpty(response)) {
                        // 从服务器返回的数据中解析出天气代号
                        String[] array = response.split("\\|");
                        if (array != null && array.length == 2) {
                            String weatherCode = array[1];
                            queryWeatherInfo(weatherCode);
                        }
                    }
                } else if ("weatherCode".equals(type)) {
                    // 处理服务器返回的天气信息
                    ResponseHandlerUtil.handleWeatherResponse(WeatherActivity.this, response);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWeather();
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_publish_time.setText("同步失败");
                    }
                });
            }
        });
    }
    /**
     * 从SharedPreferences文件中读取存储的天气信息，并显示到界面上。
     */
    private void showWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        tv_city_name.setText(prefs.getString("city_name", ""));
        tv_temp1.setText(prefs.getString("temp1", ""));
        tv_temp2.setText(prefs.getString("temp2", ""));
        tv_weather_desp.setText(prefs.getString("weather_desp", ""));
        tv_publish_time.setText("今天" + prefs.getString("publish_time", "") + "发布");
        tv_current_date.setText(prefs.getString("current_date", ""));
        ll_weather_info_layout.setVisibility(View.VISIBLE);
        tv_city_name.setVisibility(View.VISIBLE);
        //开启自动更新服务
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }



}
