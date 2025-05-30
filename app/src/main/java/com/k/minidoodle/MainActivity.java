package com.k.minidoodle;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PaintView paintView;
    private int currentColor = Color.BLACK;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        paintView = findViewById(R.id.paintView);

        // 设置颜色选择按钮
        Button colorBtn = findViewById(R.id.colorBtn);
        colorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog();
            }
        });

        // 撤销按钮已移除

        // 设置清除按钮
        Button clearBtn = findViewById(R.id.clearBtn);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.clear();
            }
        });

        // 设置保存按钮
        Button saveBtn = findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    saveDrawing();
                } else {
                    requestPermission();
                }
            }
        });
    }

    private void showColorPickerDialog() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_color_picker);
        dialog.setCancelable(true);

        // 获取视图引用
        final View colorPreviewInDialog = dialog.findViewById(R.id.colorPreviewInDialog);
        final LinearLayout presetColorsLayout = dialog.findViewById(R.id.presetColorsLayout);

        Button selectBtn = dialog.findViewById(R.id.selectColorBtn);
        Button cancelBtn = dialog.findViewById(R.id.cancelBtn);

        // 设置预设颜色
        int[] presetColors = {
                Color.BLACK, Color.WHITE, Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.GRAY,
                Color.rgb(255, 165, 0), // 橙色
                Color.rgb(128, 0, 128), // 紫色
                Color.rgb(165, 42, 42), // 棕色
                Color.rgb(255, 192, 203) // 粉色
        };

        // 记录当前选中的颜色
        final int[] selectedColor = {currentColor};

        // 动态添加预设颜色按钮
        for (final int presetColor : presetColors) {
            final View colorView = new View(this);
            int sizePx = (int) (40 * getResources().getDisplayMetrics().density); // 40dp
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(10, 10, 10, 10);
            colorView.setLayoutParams(params);
            colorView.setBackgroundColor(presetColor);

            // 添加点击事件
            colorView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedColor[0] = presetColor;
                    colorPreviewInDialog.setBackgroundColor(presetColor);
                }
            });

            presetColorsLayout.addView(colorView);
        }

        // 初始化颜色预览为当前颜色
        colorPreviewInDialog.setBackgroundColor(currentColor);

        // 设置确定按钮
        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentColor = selectedColor[0];
                paintView.setPathColor(currentColor);

                // 更新颜色按钮的背景色，以显示当前选择的颜色
                Button colorBtn = findViewById(R.id.colorBtn);
                colorBtn.setBackgroundColor(currentColor);

                // 如果颜色较深，则使用白色文本
                int red = Color.red(currentColor);
                int green = Color.green(currentColor);
                int blue = Color.blue(currentColor);
                if (red + green + blue < 384) { // 384 = 3*128
                    colorBtn.setTextColor(Color.WHITE);
                } else {
                    colorBtn.setTextColor(Color.BLACK);
                }

                dialog.dismiss();
            }
        });

        // 设置取消按钮
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // 保存绘图
    private void saveDrawing() {
        Bitmap bitmap = Bitmap.createBitmap(paintView.getWidth(), paintView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        paintView.draw(canvas);

        // 生成文件名
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = "MiniDoodle_" + sdf.format(new Date()) + ".png";

        // 保存图片
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用MediaStore
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MiniDoodle");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    Toast.makeText(MainActivity.this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // Android 9及以下使用传统文件存储
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File miniDoodleDir = new File(picturesDir, "MiniDoodle");
            if (!miniDoodleDir.exists()) {
                miniDoodleDir.mkdirs();
            }

            File file = new File(miniDoodleDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                Toast.makeText(MainActivity.this, "图片已保存到相册", Toast.LENGTH_SHORT).show();

                // 通知图库更新
                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), fileName, null);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 检查存储权限
    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用分区存储，无需特殊权限
            return true;
        } else {
            // 检查外部存储写入权限
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    // 请求存储权限
    private void requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveDrawing();
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
            }
        }
    }
}