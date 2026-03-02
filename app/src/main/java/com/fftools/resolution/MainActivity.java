package com.fftools.resolution;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.io.*;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public class MainActivity extends Activity {

    private static final String[] NAMES = {
        "iPhone 11","iPhone 11 Pro","iPhone 11 Pro Max",
        "iPhone 12 Mini","iPhone 12","iPhone 12 Pro","iPhone 12 Pro Max",
        "iPhone 13 Mini","iPhone 13","iPhone 13 Pro","iPhone 13 Pro Max",
        "iPhone 14","iPhone 14 Plus","iPhone 14 Pro","iPhone 14 Pro Max",
        "iPhone 15","iPhone 15 Plus","iPhone 15 Pro","iPhone 15 Pro Max",
        "iPhone 16","iPhone 16 Plus","iPhone 16 Pro","iPhone 16 Pro Max",
        "iPhone 17","iPhone 17 Plus","iPhone 17 Pro","iPhone 17 Pro Max"
    };

    private static final int[] W = {
        828,1125,1242,1080,1170,1170,1284,
        1080,1170,1170,1284,1170,1284,1179,1290,
        1179,1290,1179,1290,1179,1290,1206,1320,
        1179,1290,1206,1320
    };

    private static final int[] H = {
        1792,2436,2688,2340,2532,2532,2778,
        2340,2532,2532,2778,2532,2778,2556,2796,
        2556,2796,2556,2796,2556,2796,2622,2868,
        2556,2796,2622,2868
    };

    private static final int[] DPI = {
        326,458,458,476,460,460,458,
        476,460,460,458,460,458,460,460,
        460,460,460,460,460,460,460,460,
        460,460,460,460
    };

    private static final int SHIZUKU_CODE = 1001;
    private Spinner spinner;
    private TextView tvWidth,tvHeight,tvDpi,tvStatus;
    private Button btnApply,btnReset;
    private int selectedIndex = 26;
    private boolean isReset = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinner  = findViewById(R.id.spinner);
        tvWidth  = findViewById(R.id.tvWidth);
        tvHeight = findViewById(R.id.tvHeight);
        tvDpi    = findViewById(R.id.tvDpi);
        tvStatus = findViewById(R.id.tvStatus);
        btnApply = findViewById(R.id.btnApply);
        btnReset = findViewById(R.id.btnReset);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedIndex);
        updatePreview(selectedIndex);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p,View v,int pos,long id) {
                selectedIndex=pos; updatePreview(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnApply.setOnClickListener(v -> showWarning());
        btnReset.setOnClickListener(v -> showResetWarning());
        Shizuku.addRequestPermissionResultListener(this::onShizukuPermissionResult);
    }

    private void updatePreview(int i) {
        tvWidth.setText(String.valueOf(W[i]));
        tvHeight.setText(String.valueOf(H[i]));
        tvDpi.setText(String.valueOf(DPI[i]));
    }

    private void showWarning() {
        new AlertDialog.Builder(this)
            .setTitle("Peringatan")
            .setMessage("Semua Resiko Di Tanggung Sendiri\n\nResolusi layar HP akan berubah secara nyata. Pastikan Shizuku sudah aktif.")
            .setPositiveButton("Lanjutkan",(d,w)->checkShizuku(false))
            .setNegativeButton("Batal",null)
            .show();
    }

    private void showResetWarning() {
        new AlertDialog.Builder(this)
            .setTitle("Reset Resolusi")
            .setMessage("Kembalikan resolusi ke pengaturan awal?")
            .setPositiveButton("Ya, Reset",(d,w)->checkShizuku(true))
            .setNegativeButton("Batal",null)
            .show();
    }

    private void checkShizuku(boolean reset) {
        isReset=reset;
        try {
            if (!Shizuku.pingBinder()) {
                setStatus("Shizuku tidak aktif. Buka app Shizuku dulu.",false); return;
            }
            if (Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED) {
                runCommand(reset);
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                setStatus("Izin ditolak. Buka Shizuku dan izinkan app ini.",false);
            } else {
                Shizuku.requestPermission(SHIZUKU_CODE);
            }
        } catch (Exception e) {
            setStatus("Error: "+e.getMessage(),false);
        }
    }

    private void onShizukuPermissionResult(int code,int result) {
        if (result==PackageManager.PERMISSION_GRANTED) runCommand(isReset);
        else setStatus("Izin Shizuku ditolak.",false);
    }

    private void runCommand(boolean reset) {
        new Thread(()->{
            try {
                if (reset) {
                    exec("wm size reset");
                    exec("wm density reset");
                    runOnUiThread(()->setStatus("Resolusi berhasil direset ke default.",true));
                } else {
                    int i=selectedIndex;
                    exec("wm size "+W[i]+"x"+H[i]);
                    exec("wm density "+DPI[i]);
                    runOnUiThread(()->setStatus("Berhasil: "+NAMES[i]+" ("+W[i]+"x"+H[i]+" | DPI "+DPI[i]+")",true));
                }
            } catch (Exception e) {
                runOnUiThread(()->setStatus("Gagal: "+e.getMessage(),false));
            }
        }).start();
    }

    private void exec(String cmd) throws Exception {
        ShizukuRemoteProcess p=Shizuku.newProcess(new String[]{"sh","-c",cmd},null,null);
        p.waitFor(); p.destroy();
    }

    private void setStatus(String msg,boolean ok) {
        tvStatus.setText(msg);
        tvStatus.setTextColor(getResources().getColor(ok?R.color.green:R.color.red,null));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(this::onShizukuPermissionResult);
    }
  }
