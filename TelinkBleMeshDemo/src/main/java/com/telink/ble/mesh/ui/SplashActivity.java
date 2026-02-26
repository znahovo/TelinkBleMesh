/********************************************************************************************************
 * @file SplashActivity.java
 *
 * @brief for TLSR chips
 *
 * @author telink
 * @date Sep. 30, 2010
 *
 * @par Copyright (c) 2010, Telink Semiconductor (Shanghai) Co., Ltd.
 *           All rights reserved.
 *
 *			 The information contained herein is confidential and proprietary property of Telink 
 * 		     Semiconductor (Shanghai) Co., Ltd. and is available under the terms 
 *			 of Commercial License Agreement between Telink Semiconductor (Shanghai) 
 *			 Co., Ltd. and the licensee in separate contract or the terms described here-in. 
 *           This heading MUST NOT be removed from this file.
 *
 * 			 Licensees are granted free, non-transferable use of the information in this 
 *			 file under Mutual Non-Disclosure Agreement. NO WARRENTY of ANY KIND is provided. 
 *
 *******************************************************************************************************/
package com.telink.ble.mesh.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.telink.ble.mesh.demo.R;
import com.telink.ble.mesh.util.MeshLogger;
import android.location.LocationManager;
import android.content.Context;

/**
 * splash page
 * Created by kee on 2019/4/8.
 */

public class SplashActivity extends BaseActivity {
    private static final int PERMISSIONS_REQUEST_ALL = 0x10;

    private final Handler delayHandler = new Handler();

    private AlertDialog settingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Intent intent = getIntent();
        if (!this.isTaskRoot()) {
            if (intent != null) {
                String action = intent.getAction();
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                        && Intent.ACTION_MAIN.equals(action)) {
                    finish();
                    return;
                }
            }
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (settingDialog != null) {
            settingDialog.dismiss();
        }
        requestBluetoothPermissions();
    // Accept either FINE or COARSE location because on Android 12+ users may grant approximate location
    boolean locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

    if (locationGranted
        && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            onPermissionChecked();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA
                    },
                    PERMISSIONS_REQUEST_ALL
            );
        }
    }

    private void onPermissionChecked() {
        MeshLogger.log("permission check pass");
        delayHandler.removeCallbacksAndMessages(null);
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }, 500);
    }

    private void onPermissionDenied() {
        if (settingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle("Warn");
            builder.setMessage("Location permission is necessary when searching bluetooth device on 6.0 or upper device");
            builder.setPositiveButton("Go Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
            settingDialog = builder.create();
        }
        settingDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        delayHandler.removeCallbacksAndMessages(null);
    }

    private static final int REQUEST_CODE_BLE = 1001;

    // 需要請求的權限（Android 12+ 只需這兩個）
    private static final String[] BLE_PERMISSIONS = {
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    };

    
    // 在你要開始掃描前呼叫這個方法
private void requestBluetoothPermissions() {
    // 先檢查位置服務是否開啟（Android 所有版本藍牙 LE 掃描都需要位置服務開啟）
    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    boolean locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                              locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

    if (!locationEnabled) {
        // 提示開啟位置服務
        new AlertDialog.Builder(this)
            .setTitle("位置服務未開啟")
            .setMessage("藍牙掃描需要開啟位置服務（Location Services），即使不使用 GPS 定位。")
            .setPositiveButton("前往設定", (dialog, which) -> {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            })
            .setNegativeButton("取消", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
        return;
    }

    // 檢查藍牙權限
    boolean scanGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    boolean connectGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

    if (scanGranted && connectGranted) {
        // 已授權，直接開始掃描
        // startBleScan();  // 你的掃描方法
    } else {
        // 請求權限
        ActivityCompat.requestPermissions(this, BLE_PERMISSIONS, REQUEST_CODE_BLE);
    }
}



// 權限回調
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == REQUEST_CODE_BLE) {
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            // startBleScan();  // 開始掃描
        } else {
            // 你的原有拒絕邏輯，但可以改提示文字更準確
            if (settingDialog == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(false);
                builder.setTitle("權限必要");
                builder.setMessage("需要「附近裝置」（Nearby devices）權限才能搜尋藍牙設備。\n\n請在設定中授予權限。");
                builder.setPositiveButton("前往設定", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                });
                builder.setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                });
                settingDialog = builder.create();
            }
            settingDialog.show();
        }
    }
}

}
