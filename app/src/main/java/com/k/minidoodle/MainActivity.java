package com.k.minidoodle; // 确认这里的包名与您的项目包名一致

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {

    private PaintView paintView;
    private int currentColor = Color.BLACK;
    private ImageButton colorPickerBtn;
    private View colorPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        paintView = findViewById(R.id.paintView);
        colorPickerBtn = findViewById(R.id.colorPickerBtn);
        colorPreview = findViewById(R.id.colorPreview);

        // 设置初始颜色预览
        colorPreview.setBackgroundColor(currentColor);

        // 设置颜色选择按钮
        colorPickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog();
            }
        });

        // 设置清除按钮
        Button clearBtn = findViewById(R.id.clearBtn);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.clear();
            }
        });

        // 设置撤销按钮
        Button undoBtn = findViewById(R.id.undoBtn);
        undoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paintView.undo();
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
        final SeekBar redSeekBar = dialog.findViewById(R.id.redSeekBar);
        final SeekBar greenSeekBar = dialog.findViewById(R.id.greenSeekBar);
        final SeekBar blueSeekBar = dialog.findViewById(R.id.blueSeekBar);
        final EditText redEditText = dialog.findViewById(R.id.redEditText);
        final EditText greenEditText = dialog.findViewById(R.id.greenEditText);
        final EditText blueEditText = dialog.findViewById(R.id.blueEditText);
        final EditText hexEditText = dialog.findViewById(R.id.hexEditText);
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
                    updateColorControls(presetColor, redSeekBar, greenSeekBar, blueSeekBar,
                            redEditText, greenEditText, blueEditText, hexEditText, colorPreviewInDialog);
                }
            });

            presetColorsLayout.addView(colorView);
        }

        // 初始化为当前颜色
        updateColorControls(currentColor, redSeekBar, greenSeekBar, blueSeekBar,
                redEditText, greenEditText, blueEditText, hexEditText, colorPreviewInDialog);

        // 设置SeekBar监听器
        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int red = redSeekBar.getProgress();
                    int green = greenSeekBar.getProgress();
                    int blue = blueSeekBar.getProgress();

                    // 更新EditText
                    if (seekBar == redSeekBar) {
                        redEditText.setText(String.valueOf(red));
                    } else if (seekBar == greenSeekBar) {
                        greenEditText.setText(String.valueOf(green));
                    } else if (seekBar == blueSeekBar) {
                        blueEditText.setText(String.valueOf(blue));
                    }

                    // 更新十六进制
                    String hex = String.format("%02X%02X%02X", red, green, blue);
                    hexEditText.setText(hex);

                    // 更新颜色预览
                    int color = Color.rgb(red, green, blue);
                    colorPreviewInDialog.setBackgroundColor(color);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        redSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        greenSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        blueSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        // 设置EditText监听器 - RGB
        TextWatcher rgbTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int red = getValidColorValue(redEditText.getText().toString());
                    int green = getValidColorValue(greenEditText.getText().toString());
                    int blue = getValidColorValue(blueEditText.getText().toString());

                    // 更新SeekBar（避免无限循环）
                    redSeekBar.setProgress(red);
                    greenSeekBar.setProgress(green);
                    blueSeekBar.setProgress(blue);

                    // 更新十六进制
                    String hex = String.format("%02X%02X%02X", red, green, blue);
                    hexEditText.setText(hex);

                    // 更新颜色预览
                    int color = Color.rgb(red, green, blue);
                    colorPreviewInDialog.setBackgroundColor(color);
                } catch (NumberFormatException e) {
                    // 处理无效输入
                }
            }
        };

        redEditText.addTextChangedListener(rgbTextWatcher);
        greenEditText.addTextChangedListener(rgbTextWatcher);
        blueEditText.addTextChangedListener(rgbTextWatcher);

        // 设置EditText监听器 - 十六进制
        hexEditText.addTextChangedListener(new TextWatcher() {
            boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;

                isUpdating = true;
                String hexValue = s.toString();

                try {
                    // 如果不足6位，补足为黑色的相应位
                    while (hexValue.length() < 6) {
                        hexValue += "0";
                    }

                    // 解析十六进制颜色
                    int color = Color.parseColor("#" + hexValue);
                    colorPreviewInDialog.setBackgroundColor(color);

                    // 更新RGB值
                    int red = Color.red(color);
                    int green = Color.green(color);
                    int blue = Color.blue(color);

                    // 更新RGB编辑框和滑块
                    redEditText.setText(String.valueOf(red));
                    greenEditText.setText(String.valueOf(green));
                    blueEditText.setText(String.valueOf(blue));
                    redSeekBar.setProgress(red);
                    greenSeekBar.setProgress(green);
                    blueSeekBar.setProgress(blue);
                } catch (Exception e) {
                    // 无效的十六进制值，不做任何处理
                }

                isUpdating = false;
            }
        });

        // 设置确定按钮
        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int red = redSeekBar.getProgress();
                int green = greenSeekBar.getProgress();
                int blue = blueSeekBar.getProgress();

                currentColor = Color.rgb(red, green, blue);
                paintView.setPathColor(currentColor);
                colorPreview.setBackgroundColor(currentColor);
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

    // 辅助方法：更新所有颜色控件
    private void updateColorControls(int color, SeekBar redSeekBar, SeekBar greenSeekBar,
                                     SeekBar blueSeekBar, EditText redEditText, EditText greenEditText,
                                     EditText blueEditText, EditText hexEditText, View colorPreview) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        // 更新SeekBar
        redSeekBar.setProgress(red);
        greenSeekBar.setProgress(green);
        blueSeekBar.setProgress(blue);

        // 更新EditText
        redEditText.setText(String.valueOf(red));
        greenEditText.setText(String.valueOf(green));
        blueEditText.setText(String.valueOf(blue));

        // 更新十六进制
        String hex = String.format("%02X%02X%02X", red, green, blue);
        hexEditText.setText(hex);

        // 更新颜色预览
        colorPreview.setBackgroundColor(color);
    }

    // 辅助方法：获取有效的颜色值（0-255）
    private int getValidColorValue(String input) {
        try {
            int value = Integer.parseInt(input);
            if (value < 0) return 0;
            if (value > 255) return 255;
            return value;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}