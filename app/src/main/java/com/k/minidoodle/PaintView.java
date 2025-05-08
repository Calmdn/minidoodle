package com.k.minidoodle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PaintView extends View {

    private static final float STROKE_WIDTH = 12f;
    private Paint paint;
    private Path currentPath;
    private int currentColor;

    // 存储路径和它们的颜色，用于撤销功能
    private List<PathWithColor> paths = new ArrayList<>();

    private static class PathWithColor {
        Path path;
        int color;

        PathWithColor(Path path, int color) {
            this.path = path;
            this.color = color;
        }
    }

    public PaintView(Context context) {
        super(context);
        init();
    }

    public PaintView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PaintView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_WIDTH);
        currentColor = Color.BLACK;
        paint.setColor(currentColor);

        currentPath = new Path();
    }

    public void setPathColor(int color) {
        this.currentColor = color;
        paint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制所有存储的路径及其各自的颜色
        for (PathWithColor pathWithColor : paths) {
            paint.setColor(pathWithColor.color);
            canvas.drawPath(pathWithColor.path, paint);
        }

        // 用当前颜色绘制当前路径
        paint.setColor(currentColor);
        canvas.drawPath(currentPath, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                currentPath.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                currentPath.lineTo(x, y);
                // 存储完成的路径及其颜色
                paths.add(new PathWithColor(new Path(currentPath), currentColor));
                currentPath.reset();
                break;
            default:
                return false;
        }

        invalidate();
        return true;
    }

    public void clear() {
        paths.clear();
        currentPath.reset();
        invalidate();
    }

    public void undo() {
        if (paths.size() > 0) {
            paths.remove(paths.size() - 1);
            invalidate();
        }
    }
}