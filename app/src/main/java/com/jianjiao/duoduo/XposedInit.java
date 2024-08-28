package com.jianjiao.duoduo;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage {
    public String TAG = "__尖叫__xp";
    Context sContext;
    String ACTIONR = "com.jianjiao.test.PDDGUANGBO";
    ClassLoader classLoader;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws ClassNotFoundException {
        classLoader = lpparam.classLoader;
        //!"com.baidu.searchbox".equals(lpparam.packageName) ||
        if (!"com.baidu.searchbox".equals(lpparam.packageName)) {
            return;
        }
        Log.d(TAG, "开始hook：" + lpparam.packageName);
        XposedHelpers.findAndHookMethod("com.baidu.swan.apps.SwanAppActivity",
                lpparam.classLoader,
                "onCreate",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        //Activity activity = (Activity) param.thisObject;
                        Activity mActivity = (Activity) param.thisObject;
                        sContext = (Context) mActivity.getApplicationContext();
                        Toast.makeText(sContext, "插件已加载", Toast.LENGTH_SHORT);
                        sendIntent(1, "插件已加载");
                        /*dataReceiver=new DataReceiver();
                        IntentFilter filter = new IntentFilter();//创建IntentFilter对象
                        filter.addAction(ACTIONR);
                        mActivity.registerReceiver(dataReceiver, filter);//注册Broadcast Receiver*/
                        //createFloatView();
                    }
                });


        //获取并上传当前渲染的商品数据，
        Class GsonClass = lpparam.classLoader.loadClass("com.google.gson.Gson");
        Object gsonClass = XposedHelpers.newInstance(GsonClass);
        //Class<?> JSONObject = XposedHelpers.findClass("org.json.JSONObject", lpparam.classLoader);
        Class<?> ResponseBody = XposedHelpers.findClass("okhttp3.ResponseBody", lpparam.classLoader);
        XposedHelpers.findAndHookMethod("wu4.f",
                lpparam.classLoader,
                "Y",
                JSONObject.class,
                ResponseBody,
                String.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        JSONObject jsons = new JSONObject(param.args[0].toString());
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //String str = jsons.toString();
                                JSONObject data = null;
                                try {
                                    if (jsons.has("data")) {
                                        data = jsons.getJSONObject("data");
                                        if (data != null) {
                                            if (data.has("count")) {
                                                Log.d(TAG, "开始发送:" + data.toString());
                                                sendIntent(11, data.toString());
                                            } else if (data.has("roomGoodsList")) {
                                                Log.d(TAG, "开始发送:" + data.toString());
                                                sendIntent(12, data.toString());
                                            } else {
                                                //XposedBridge.log("开始发送:" + data.toString());
                                                //sendIntent(11, data.toString());
                                                Log.d(TAG, "其他请求,无需发送1: " + data);
                                            }
                                        } else {
                                            Log.d(TAG, "data为空: " + jsons);
                                        }
                                    }
                                } catch (JSONException e) {
                                    Log.d(TAG, "json解析失败: " + e + "___" + data);
                                }
                            }
                        }).start();
                    }
                });
    }


    public void sendIntent(int code, String data) {
        // 确定切片大小
        final int SLICE_SIZE = 1024 * 100;

        // 分割数据
        List<String> slices = new ArrayList<>();
        for (int i = 0; i < data.length(); i += SLICE_SIZE) {
            slices.add(data.substring(i, Math.min(i + SLICE_SIZE, data.length())));
        }
        Log.d(TAG, "开始发送数据: " + data.length() + "| " + slices.size());
        // 发送每个切片
        for (int i = 0; i < slices.size(); i++) {
            String sliceData = slices.get(i);
            // 创建 Intent 对象
            Intent intent = new Intent();
            intent.setAction(ACTIONR);
            // 添加切片索引和数据
            intent.putExtra("code", code);
            intent.putExtra("index", i);
            intent.putExtra("total", slices.size());
            intent.putExtra("data", sliceData);
            Log.d(TAG, "发送: " + i + "|" + slices.size() + "|" + sliceData);
            // 发送广播
            try {
                if (sContext == null) {
                    // 获取 application context 并发送广播
                    Class<?> SearchBox = classLoader.loadClass("com.baidu.searchbox.SearchBox");
                    sContext = (Context) XposedHelpers.callMethod(SearchBox, "getAppContext");
                    sContext.sendBroadcast(intent);
                    XposedBridge.log("发送广播: 自行获取application " + sContext);
                } else {
                    // 使用现有的 Context 发送广播
                    sContext.sendBroadcast(intent);
                    XposedBridge.log("发送广播: 使用原context " + sContext);
                }
            } catch (Exception e) {
                XposedBridge.log("发送消息失败: " + e);
                Log.d(TAG, "发送消息失败: " + e);
            }
        }
    }
}













