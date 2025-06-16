package com.k.minidoodle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PaintView extends View {

    // 画笔类型枚举
    public enum BrushType {
        NORMAL,     // 普通画笔
        HIGHLIGHTER, // 荧光笔
        PEN,        // 钢笔
        BRUSH,      // 毛笔
        ERASER      // 橡皮擦
    }

    private Paint paint;
    private Path currentPath;
    private int currentColor;
    private float strokeWidth = 12f;
    private BrushType currentBrushType = BrushType.NORMAL;
    private boolean isEraserMode = false;

    // 存储路径和它们的属性，用于撤销功能
    private List<PathData> paths = new ArrayList<>();
    private Bitmap canvasBitmap;
    private Canvas drawCanvas;

    private static class PathData {
        Path path;
        Paint paint;
        BrushType brushType;

        PathData(Path path, Paint paint, BrushType brushType) {
            this.path = new Path(path);
            this.paint = new Paint(paint);
            this.brushType = brushType;
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
        setupPaint();
        currentColor = Color.BLACK;
        currentPath = new Path();
    }

    private void setupPaint() {
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(currentColor);
        updatePaintForBrushType();
    }

    private void updatePaintForBrushType() {
        switch (currentBrushType) {
            case NORMAL:
                paint.setXfermode(null);
                paint.setMaskFilter(null);
                paint.setAlpha(255);
                break;
            case HIGHLIGHTER:
                paint.setXfermode(null);
                paint.setMaskFilter(null);
                paint.setAlpha(128); // 半透明效果
                break;
            case PEN:
                paint.setXfermode(null);
                paint.setMaskFilter(null);
                paint.setAlpha(255);
                paint.setStrokeCap(Paint.Cap.SQUARE);
                break;
            case BRUSH:
                paint.setXfermode(null);
                paint.setMaskFilter(new BlurMaskFilter(2, BlurMaskFilter.Blur.NORMAL));
                paint.setAlpha(255);
                break;
            case ERASER:
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                paint.setMaskFilter(null);
                paint.setAlpha(255);
                break;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制背景
        canvas.drawColor(Color.WHITE);

        if (canvasBitmap != null) {
            canvas.drawBitmap(canvasBitmap, 0, 0, null);
        }

        // 绘制当前正在画的路径
        if (!currentPath.isEmpty()) {
            canvas.drawPath(currentPath, paint);
        }
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
                // 将完成的路径绘制到画布上
                if (drawCanvas != null) {
                    drawCanvas.drawPath(currentPath, paint);
                }
                // 保存路径数据用于撤销
                paths.add(new PathData(currentPath, paint, currentBrushType));
                currentPath.reset();
                break;
            default:
                return false;
        }

        invalidate();
        return true;
    }

    // 设置画笔颜色
    public void setColor(int color) {
        this.currentColor = color;
        paint.setColor(color);
        updatePaintForBrushType();
    }

    // 设置画笔粗细
    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
        paint.setStrokeWidth(width);
    }

    // 获取当前画笔粗细
    public float getStrokeWidth() {
        return strokeWidth;
    }

    // 设置画笔类型
    public void setBrushType(BrushType brushType) {
        this.currentBrushType = brushType;
        this.isEraserMode = (brushType == BrushType.ERASER);
        updatePaintForBrushType();
    }

    // 获取当前画笔类型
    public BrushType getBrushType() {
        return currentBrushType;
    }

    // 清除画布
    public void clear() {
        paths.clear();
        currentPath.reset();
        if (canvasBitmap != null) {
            canvasBitmap.eraseColor(Color.TRANSPARENT);
        }
        invalidate();
    }

    // 撤销操作
    public void undo() {
        if (paths.size() > 0) {
            paths.remove(paths.size() - 1);
            // 重新绘制所有路径
            redrawCanvas();
            invalidate();
        }
    }

    // 重新绘制画布
    private void redrawCanvas() {
        if (canvasBitmap != null && drawCanvas != null) {
            canvasBitmap.eraseColor(Color.TRANSPARENT);
            for (PathData pathData : paths) {
                drawCanvas.drawPath(pathData.path, pathData.paint);
            }
        }
    }

    // 获取画布位图用于保存
    public Bitmap getCanvasBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        draw(canvas);
        return bitmap;
    }
}