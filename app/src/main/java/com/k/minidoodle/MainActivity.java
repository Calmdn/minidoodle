package com.k.minidoodle;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private DoodleView doodleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        doodleView = findViewById(R.id.doodleView);
        Button btnColorPicker = findViewById(R.id.btnColorPicker);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnSave = findViewById(R.id.btnSave);

        // Color button: open a simple color picker dialog with preset colors
        btnColorPicker.setOnClickListener(v -> openColorPicker());

        // Clear button: clear the drawing area
        btnClear.setOnClickListener(v -> doodleView.clear());

        // Save button: save the drawing to the device's Pictures directory using MediaStore API
        btnSave.setOnClickListener(v -> saveDrawing());
    }

    // A simple color picker using an AlertDialog with preset color choices
    private void openColorPicker() {
        final String[] colorNames = {"黑色", "红色", "蓝色", "绿色", "橙色", "紫色"};
        final int[] colorValues = {
                Color.BLACK,
                Color.RED,
                Color.BLUE,
                Color.GREEN,
                Color.parseColor("#FFA500"), // Orange
                Color.MAGENTA
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请选择颜色");
        builder.setItems(colorNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Set the selected color on the DoodleView
                doodleView.setPaintColor(colorValues[which]);
            }
        });
        builder.show();
    }

    // Save the current drawing using MediaStore API
    private void saveDrawing() {
        // Enable the drawing cache so we can obtain the bitmap
        doodleView.setDrawingCacheEnabled(true);
        doodleView.buildDrawingCache();
        Bitmap bitmap = doodleView.getDrawingCache();
        if (bitmap == null) {
            Toast.makeText(this, "没有图像可保存", Toast.LENGTH_SHORT).show();
            return;
        }

        long timeStamp = System.currentTimeMillis();
        String fileName = "doodle_" + timeStamp + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        // Save to the public Pictures directory
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    Toast.makeText(this, "保存成功: " + fileName, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "保存失败：无法获取输出流", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "保存失败：无法创建媒体记录", Toast.LENGTH_SHORT).show();
        }

        // Disable and destroy the drawing cache
        doodleView.destroyDrawingCache();
    }
}