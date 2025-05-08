
package com.k.minidoodle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * DoodleView 自定义绘图视图
 * 修改后使用 Path 绘制连续的线条，而不是单个圆点。
 */
public class DoodleView extends View {

    // 存储绘制内容的位图
    private Bitmap bitmap;
    // 位图对应的 Canvas
    private Canvas bitmapCanvas;
    // 绘制用的画笔
    private Paint paint;
    // 用于绘制连续线条的 Path 对象
    private Path drawPath;

    /**
     * 构造方法
     * @param context 上下文
     * @param attrs 自定义属性
     */
    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * 初始化方法，设置画笔和 Path 的默认属性
     */
    private void init() {
        // 初始化画笔，默认颜色为黑色，线宽为10
        paint = new Paint();
        paint.setColor(0xFF000000); // 黑色
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);

        // 初始化 Path 对象
        drawPath = new Path();
    }

    /**
     * 当视图尺寸改变时创建用于绘制的位图和 Canvas，并填充白色背景
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmapCanvas.drawColor(0xFFFFFFFF);  // 白色背景
    }

    /**
     * 绘制方法，将位图显示在屏幕上，并绘制当前正在绘制的 Path
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // 先绘制已有的位图
        canvas.drawBitmap(bitmap, 0, 0, null);
        // 再绘制当前 Path（不提交到位图缓存中，等待抬起后提交）
        canvas.drawPath(drawPath, paint);
    }

    /**
     * 设置画笔颜色
     * @param color 新的画笔颜色
     */
    public void setPaintColor(int color) {
        paint.setColor(color);
    }

    /**
     * 清空画布：清除位图中的内容并重置 Path
     */
    public void clear() {
        if (bitmapCanvas != null) {
            bitmapCanvas.drawColor(0xFFFFFFFF);  // 重置为白色背景
            drawPath.reset();
            invalidate();
        }
    }

    /**
     * 处理触摸事件，实现连续线条绘制
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 手指按下时重置 Path 并移动到起始位置
                drawPath.reset();
                drawPath.moveTo(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                // 手指移动时把线条连接到当前点
                drawPath.lineTo(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                // 手指抬起时把当前 Path 绘制到位图上，然后重置 Path
                bitmapCanvas.drawPath(drawPath, paint);
                drawPath.reset();
                invalidate();
                return true;
            default:
                return false;
        }
    }
}