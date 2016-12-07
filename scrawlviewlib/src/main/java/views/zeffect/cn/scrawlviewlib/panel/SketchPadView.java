package views.zeffect.cn.scrawlviewlib.panel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import views.zeffect.cn.scrawlviewlib.R;


/**
 * 增加笔迹路线
 */
public class SketchPadView extends ImageView {
    /**
     * Need to track this so the dirty region can accommodate the stroke.
     **/
    private float HALF_STROKE_WIDTH = 0;
    private int m_strokeColor = Color.BLACK;
    private int m_penSize;
    private int m_eraserSize;
    private PenType mPenType = PenType.Pen;

    public enum PenType {
        Pen, Eraser;
    }

    private Paint m_bitmapPaint = null;
    private Bitmap m_foreBitmap = null;
    private Canvas m_canvas = null;
    private ISketchPadTool m_curTool = null;
    /**
     * 就用这个来存储图片的宽高了
     */
    private ViewPoint mPhotoSize = new ViewPoint();

    public SketchPadView(Context context) {
        this(context, null);
    }

    public SketchPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public SketchPadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public void setStrokeType(PenType type) {
        if (type == PenType.Pen) {
            m_curTool = new SketchPadPen(m_penSize, m_strokeColor);
        } else if (type == PenType.Eraser) {
            m_curTool = new SketchPadEraser(m_eraserSize);
        }
        mPenType = type;
    }

    /***
     * 存储画过的所有线
     */
    private List<Line> mLines = new LinkedList<>();
    /**
     * 存储一条线
     */
    private Line mLine;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //
        float eventX = event.getX();
        float eventY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLine = new Line();
                mLine.color = m_curTool.getColor();
                mLine.width = (int) m_curTool.getSize();
                if (mPenType == PenType.Pen) {
                    mLine.type = 0;
                } else if (mPenType == PenType.Eraser) {
                    mLine.type = 1;
                }
                lastTouchX = eventX;
                lastTouchY = eventY;
                savePoint(eventX, eventY);
                setStrokeType(mPenType);
                m_curTool.touchDown(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                resetDirtyRect(eventX, eventY);
                int historySize = event.getHistorySize();
                for (int i = 0; i < historySize; i++) {//取当前点和下一点作贝塞乐曲线
                    float historicalX = event.getHistoricalX(i);
                    float historicalY = event.getHistoricalY(i);
                    savePoint(historicalX, historicalY);
                    expandDirtyRect(historicalX, historicalY);
                    m_curTool.touchMove(historicalX, historicalY);
                    if (mPenType == PenType.Eraser) {
                        m_curTool.draw(m_canvas);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                m_curTool.touchUp(event.getX(), event.getY());
                m_curTool.draw(m_canvas);
                savePoint(eventX, eventY);
                mLines.add(mLine);
                break;
        }
        invalidate(
                (int) (dirtyRect.left - HALF_STROKE_WIDTH),
                (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

        lastTouchX = eventX;
        lastTouchY = eventY;
        return true;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (null != m_foreBitmap) {
            canvas.drawBitmap(m_foreBitmap, 0, 0, m_bitmapPaint);
        }
        if (mPenType != PenType.Eraser) {
            m_curTool.draw(canvas);
        }
    }

    /**
     * 设置图片宽高
     *
     * @param pBitmap 图片
     */
    public void setViewBackground(Bitmap pBitmap) {
        if (pBitmap != null) {
            clear();
            ViewPoint tPoint = new ViewPoint();
            tPoint.x = pBitmap.getWidth();
            tPoint.y = pBitmap.getHeight();
            mPhotoSize = tPoint;
            this.setImageBitmap(pBitmap);
        }

    }

    /**
     * 清空
     */
    public void clear() {
        mLines.clear();
        m_curTool.cleanAll();
        SketchPadEraser tempEraser = new SketchPadEraser(m_eraserSize);
        tempEraser.drawRect(m_canvas, new Rect(0, 0, getWidth(), getHeight()));
        invalidate();
    }

    /**
     * 画线，根椐所有的线
     *
     * @param pLines
     */
    public void drawLines(List<Line> pLines) {
        if (pLines == null) {
            return;
        }
        for (Line pLine : pLines) {
            ISketchPadTool tempTool;
            if (pLine.type == 0) {
                int penSize = m_penSize;
                if (pLine.width != 0) {
                    penSize = pLine.width;
                }
                int penColor = m_strokeColor;
                if (pLine.color != 0) {
                    penColor = pLine.color;
                }
                tempTool = new SketchPadPen(penSize, penColor);
            } else if (pLine.type == 1) {
                int penSize = m_eraserSize;
                if (pLine.width != 0) {
                    penSize = pLine.width;
                }
                tempTool = new SketchPadEraser(penSize);
            } else {
                int penSize = m_penSize;
                if (pLine.width != 0) {
                    penSize = pLine.width;
                }
                int penColor = m_strokeColor;
                if (pLine.color != 0) {
                    penColor = pLine.color;
                }
                tempTool = new SketchPadPen(penSize, penColor);
            }
            Path tempPath = new Path();
            float mX = 0;
            float mY = 0;
            for (int i = 0; i < pLine.mPoints.size() - 1; i++) {
                ViewPoint tempPoint = pLine.mPoints.get(i);
                if (i == 0) {
                    mX = toViewAxisX(tempPoint.x);
                    mY = toViewAxisY(tempPoint.y);
                    tempPath.moveTo(mX, mY);
                } else {
                    float previousX = mX;
                    float previousY = mY;
                    ViewPoint nextPoint = pLine.mPoints.get(i + 1);
                    //设置贝塞尔曲线的操作点为起点和终点的一半
                    float cX = (mX + toViewAxisX(nextPoint.x)) / 2;
                    float cY = (mY + toViewAxisY(nextPoint.y)) / 2;
                    tempPath.quadTo(previousX, previousY, cX, cY);
                    mX = toViewAxisX(nextPoint.x);
                    mY = toViewAxisY(nextPoint.y);
                }
            }
            tempTool.drawPath(m_canvas, tempPath);
        }

    }

    /**
     * 返回当前图的所有笔画
     *
     * @return 笔画
     */
    public List<Line> getLines() {
        return mLines;
    }

    /**
     * 存储一个点
     *
     * @param eventX x
     * @param eventY y
     */
    private void savePoint(float eventX, float eventY) {
        ViewPoint tPoint = new ViewPoint();
        tPoint.x = toPhotoAxisX(eventX);
        tPoint.y = toPhotoAxisY(eventY);
        mLine.mPoints.add(tPoint);
    }


    /**
     * 转换为图片中的坐标
     *
     * @param x
     * @return
     */
    public float toPhotoAxisX(float x) {
        float vW = getWidth();
        float pW = mPhotoSize.x;
        return pW * x / vW;
    }

    /**
     * 转换为图片中的Y
     *
     * @param y
     * @return
     */
    private float toPhotoAxisY(float y) {
        float vH = getHeight();
        float pH = mPhotoSize.y;
        //
        return y * pH / vH;
    }


    /**
     * 转换成控件中的坐标
     *
     * @param x
     * @return
     */
    private float toViewAxisX(float x) {
        float vW = getWidth();
        float pW = mPhotoSize.x;
        //
        return x * vW / pW;
    }

    /**
     * 转换为控件中的Y
     *
     * @param y
     * @return
     */
    private float toViewAxisY(float y) {
        float vH = getHeight();
        float pH = mPhotoSize.y;
        //
        return y * vH / pH;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        createStrokeBitmap(w, h);
    }

    protected void initialize() {
        m_penSize = (int) getResources().getDimension(R.dimen.pen_size);
        m_eraserSize = 2 * m_penSize;
        m_canvas = new Canvas();
        m_bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setStrokeType(PenType.Pen);
        HALF_STROKE_WIDTH = m_penSize / 2;
        mPhotoSize.x = 1280;
        mPhotoSize.y = 800;
    }

    protected void createStrokeBitmap(int w, int h) {
        Bitmap bitmap = Bitmap.createBitmap(w, h,
                Bitmap.Config.ARGB_8888);
        if (null != bitmap) {
            m_foreBitmap = bitmap;
            m_canvas.setBitmap(m_foreBitmap);
        }
    }

    /**
     * Optimizes painting by invalidating the smallest possible area.
     */
    private float lastTouchX;
    private float lastTouchY;
    private final RectF dirtyRect = new RectF();

    /**
     * Called when replaying history to ensure the dirty region includes all
     * points.
     */
    private void expandDirtyRect(float historicalX, float historicalY) {
        if (historicalX < dirtyRect.left) {
            dirtyRect.left = historicalX;
        } else if (historicalX > dirtyRect.right) {
            dirtyRect.right = historicalX;
        }
        if (historicalY < dirtyRect.top) {
            dirtyRect.top = historicalY;
        } else if (historicalY > dirtyRect.bottom) {
            dirtyRect.bottom = historicalY;
        }
    }

    /**
     * Resets the dirty region when the motion event occurs.
     */
    private void resetDirtyRect(float eventX, float eventY) {
        dirtyRect.left = Math.min(lastTouchX, eventX);
        dirtyRect.right = Math.max(lastTouchX, eventX);
        dirtyRect.top = Math.min(lastTouchY, eventY);
        dirtyRect.bottom = Math.max(lastTouchY, eventY);
    }

    /**
     * 代表一条线
     */
    public class Line {
        /**
         * 点的集合
         */
        private ArrayList<ViewPoint> mPoints = new ArrayList<>();
        /**
         * 线的颜色
         */
        private int color;
        /**
         * 线的宽度
         */
        private int width;
        /**
         * 用来标记是笔还是橡皮
         * 0笔1橡皮
         */
        private int type;
    }

    /**
     * 点
     */
    private class ViewPoint {
        /**
         * X坐标
         */
        private float x;
        /**
         * Y坐标
         */
        private float y;
    }
}
