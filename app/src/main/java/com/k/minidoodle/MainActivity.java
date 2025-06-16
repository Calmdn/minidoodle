package com.k.minidoodle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PaintView paintView;
    private ScrollView sidePanel;
    private ImageButton menuButton;
    private View overlay;
    private boolean isPanelOpen = false;

    // 工具控件
    private SeekBar brushSizeSeekBar;
    private TextView brushSizeText;
    private LinearLayout brushTypeContainer;
    private GridLayout colorPalette;

    // 当前设置
    private int currentColor = Color.BLACK;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏标题栏和状态栏，创建沉浸式全屏体验
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
        setupColorPalette();
        setupBrushTypes();
    }

    private void initViews() {
        paintView = findViewById(R.id.paintView);
        sidePanel = findViewById(R.id.sidePanel);
        menuButton = findViewById(R.id.menuButton);
        overlay = findViewById(R.id.overlay);
        brushSizeSeekBar = findViewById(R.id.brushSizeSeekBar);
        brushSizeText = findViewById(R.id.brushSizeText);
        brushTypeContainer = findViewById(R.id.brushTypeContainer);
        colorPalette = findViewById(R.id.colorPalette);

        // 初始化画笔大小
        updateBrushSizeDisplay();

        // 初始化侧边栏位置
        sidePanel.setTranslationX(-dpToPx(350));

        // 确保ScrollView能够正常滚动
        sidePanel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 让ScrollView处理触摸事件
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
    }

    private void setupListeners() {
        // 菜单按钮点击事件
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSidePanel();
            }
        });

        // 覆盖层点击事件 - 点击画布区域关闭侧边栏
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPanelOpen) {
                    closeSidePanel();
                }
            }
        });

        // 防止覆盖层拦截侧边栏的触摸事件
        overlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                // 如果触摸在侧边栏区域内，不处理事件
                if (isPanelOpen && x < dpToPx(340)) {
                    return false;
                }
                // 否则关闭侧边栏
                if (isPanelOpen) {
                    closeSidePanel();
                    return true;
                }
                return false;
            }
        });

        // 画笔大小调节
        brushSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float brushSize = progress + 1; // 1-100
                    paintView.setStrokeWidth(brushSize);
                    updateBrushSizeDisplay();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 工具按钮点击事件
        findViewById(R.id.undoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.undo();
                showToast("已撤销");
            }
        });

        findViewById(R.id.clearButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.clear();
                showToast("画布已清空");
            }
        });

        findViewById(R.id.saveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDrawing();
            }
        });

        // 高级颜色选择器按钮
        findViewById(R.id.advancedColorButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdvancedColorPicker();
            }
        });
    }

    private void setupColorPalette() {
        int[] colors = {
                Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.GRAY,
                Color.rgb(255, 165, 0), // 橙色
                Color.rgb(128, 0, 128), // 紫色
                Color.rgb(255, 192, 203), // 粉色
                Color.rgb(139, 69, 19), // 棕色
                Color.rgb(255, 105, 180), // 热粉色
                Color.rgb(0, 191, 255), // 深天蓝
                Color.rgb(50, 205, 50), // 酸橙绿
                Color.rgb(255, 69, 0) // 橙红色
        };

        for (int color : colors) {
            View colorView = createColorView(color);
            colorPalette.addView(colorView);
        }
    }

    private View createColorView(final int color) {
        View colorView = new View(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dpToPx(34);
        params.height = dpToPx(34);
        params.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
        colorView.setLayoutParams(params);

        // 创建圆形背景
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(dpToPx(2), Color.WHITE);
        colorView.setBackground(drawable);

        colorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectColor(color);
                showToast("颜色已选择");
            }
        });

        return colorView;
    }

    private void setupBrushTypes() {
        String[] brushNames = {"画笔", "荧光笔", "钢笔", "毛笔", "橡皮擦"};
        PaintView.BrushType[] brushTypes = {
                PaintView.BrushType.NORMAL,
                PaintView.BrushType.HIGHLIGHTER,
                PaintView.BrushType.PEN,
                PaintView.BrushType.BRUSH,
                PaintView.BrushType.ERASER
        };

        for (int i = 0; i < brushNames.length; i++) {
            Button brushButton = createBrushTypeButton(brushNames[i], brushTypes[i]);
            brushTypeContainer.addView(brushButton);
        }
    }

    private Button createBrushTypeButton(String name, final PaintView.BrushType brushType) {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(40)
        );
        params.setMargins(0, dpToPx(2), 0, dpToPx(2));
        button.setLayoutParams(params);

        button.setText(name);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));

        // 创建按钮背景
        updateButtonBackground(button, false);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectBrushType(brushType, (Button) v);
                showToast("已选择 " + ((Button) v).getText());
            }
        });

        // 默认选中画笔
        if (brushType == PaintView.BrushType.NORMAL) {
            updateButtonBackground(button, true);
        }

        return button;
    }

    private void updateButtonBackground(Button button, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(5));
        if (selected) {
            drawable.setColor(Color.parseColor("#4CAF50"));
        } else {
            drawable.setColor(Color.parseColor("#546E7A"));
        }
        button.setBackground(drawable);
    }

    private void selectColor(int color) {
        currentColor = color;
        paintView.setColor(color);

        // 更新颜色选择视觉反馈
        for (int i = 0; i < colorPalette.getChildCount(); i++) {
            View child = colorPalette.getChildAt(i);
            child.setScaleX(1.0f);
            child.setScaleY(1.0f);
        }
    }

    private void selectBrushType(PaintView.BrushType brushType, Button selectedButton) {
        paintView.setBrushType(brushType);

        // 更新按钮选中状态
        for (int i = 0; i < brushTypeContainer.getChildCount(); i++) {
            View child = brushTypeContainer.getChildAt(i);
            if (child instanceof Button) {
                updateButtonBackground((Button) child, false);
            }
        }
        updateButtonBackground(selectedButton, true);
    }

    private void showAdvancedColorPicker() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_color_picker);
        dialog.setCancelable(true);

        // 获取视图引用
        final View colorPreview = dialog.findViewById(R.id.colorPreview);
        final SeekBar redSeekBar = dialog.findViewById(R.id.redSeekBar);
        final SeekBar greenSeekBar = dialog.findViewById(R.id.greenSeekBar);
        final SeekBar blueSeekBar = dialog.findViewById(R.id.blueSeekBar);
        final EditText hexEditText = dialog.findViewById(R.id.hexEditText);
        Button selectBtn = dialog.findViewById(R.id.selectColorBtn);
        Button cancelBtn = dialog.findViewById(R.id.cancelBtn);

        // 初始化当前颜色
        redSeekBar.setProgress(Color.red(currentColor));
        greenSeekBar.setProgress(Color.green(currentColor));
        blueSeekBar.setProgress(Color.blue(currentColor));
        updateColorPreview(colorPreview, redSeekBar, greenSeekBar, blueSeekBar, hexEditText);

        // SeekBar监听器
        SeekBar.OnSeekBarChangeListener colorChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateColorPreview(colorPreview, redSeekBar, greenSeekBar, blueSeekBar, hexEditText);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        redSeekBar.setOnSeekBarChangeListener(colorChangeListener);
        greenSeekBar.setOnSeekBarChangeListener(colorChangeListener);
        blueSeekBar.setOnSeekBarChangeListener(colorChangeListener);

        // 十六进制输入监听
        hexEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 6) {
                    try {
                        int color = Color.parseColor("#" + s.toString());
                        redSeekBar.setProgress(Color.red(color));
                        greenSeekBar.setProgress(Color.green(color));
                        blueSeekBar.setProgress(Color.blue(color));
                        colorPreview.setBackgroundColor(color);
                    } catch (IllegalArgumentException e) {
                        // 无效颜色代码
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedColor = Color.rgb(
                        redSeekBar.getProgress(),
                        greenSeekBar.getProgress(),
                        blueSeekBar.getProgress()
                );
                selectColor(selectedColor);
                showToast("自定义颜色已选择");
                dialog.dismiss();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void updateColorPreview(View colorPreview, SeekBar red, SeekBar green, SeekBar blue, EditText hexEditText) {
        int color = Color.rgb(red.getProgress(), green.getProgress(), blue.getProgress());
        colorPreview.setBackgroundColor(color);
        String hex = String.format("%06X", (0xFFFFFF & color));
        hexEditText.setText(hex);
    }

    private void toggleSidePanel() {
        if (isPanelOpen) {
            closeSidePanel();
        } else {
            openSidePanel();
        }
    }

    private void openSidePanel() {
        sidePanel.setVisibility(View.VISIBLE);
        overlay.setVisibility(View.VISIBLE);

        // 平滑滑出动画
        ObjectAnimator panelAnimator = ObjectAnimator.ofFloat(sidePanel, "translationX",
                -dpToPx(350), 0);
        panelAnimator.setDuration(350);
        panelAnimator.setInterpolator(new DecelerateInterpolator());
        panelAnimator.start();

        // 覆盖层淡入
        ObjectAnimator overlayAnimator = ObjectAnimator.ofFloat(overlay, "alpha", 0f, 0.5f);
        overlayAnimator.setDuration(350);
        overlayAnimator.start();

        // 菜单按钮旋转
        menuButton.animate().rotation(90).setDuration(350).start();

        isPanelOpen = true;
    }

    private void closeSidePanel() {
        // 平滑滑入动画
        ObjectAnimator panelAnimator = ObjectAnimator.ofFloat(sidePanel, "translationX",
                0, -dpToPx(350));
        panelAnimator.setDuration(350);
        panelAnimator.setInterpolator(new DecelerateInterpolator());
        panelAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                sidePanel.setVisibility(View.GONE);
            }
        });
        panelAnimator.start();

        // 覆盖层淡出
        ObjectAnimator overlayAnimator = ObjectAnimator.ofFloat(overlay, "alpha", 0.5f, 0f);
        overlayAnimator.setDuration(350);
        overlayAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlay.setVisibility(View.GONE);
            }
        });
        overlayAnimator.start();

        // 菜单按钮还原
        menuButton.animate().rotation(0).setDuration(350).start();

        isPanelOpen = false;
    }

    private void updateBrushSizeDisplay() {
        int size = (int) paintView.getStrokeWidth();
        brushSizeText.setText(size + "px");
        brushSizeSeekBar.setProgress(size - 1);
    }

    private void saveDrawing() {
        if (checkPermission()) {
            performSave();
        } else {
            requestPermission();
        }
    }

    private void performSave() {
        try {
            Bitmap bitmap = paintView.getCanvasBitmap();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Doodle_" + timeStamp + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MiniDoodle");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
                showToast("作品已保存到相册");
            }
        } catch (Exception e) {
            showToast("保存失败：" + e.getMessage());
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performSave();
            } else {
                showToast("需要存储权限才能保存作品");
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (isPanelOpen) {
            closeSidePanel();
        } else {
            super.onBackPressed();
        }
    }
}