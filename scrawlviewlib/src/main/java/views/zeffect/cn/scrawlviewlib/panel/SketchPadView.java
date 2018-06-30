package views.zeffect.cn.scrawlviewlib.panel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.HashMap;
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
    private int m_strokeColor = Color.BLUE;
    private int m_eraserColor = Color.WHITE;
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
    /***
     * 能否画线或文字
     */
    private boolean isCanDraw = true;

    /**
     * 手指动作
     */
    private OnFingerAction mFingerAction;

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
            m_curTool = new SketchPadEraser(m_eraserColor, m_eraserSize);
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
        if (!isCanDraw) {
            return false;
        }
        mIsTouchUp = false;
        //
        float eventX = event.getX();
        float eventY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLine = new Line();
                mLine.setColor(m_curTool.getColor());
                mLine.setWidth((int) m_curTool.getSize());
                HALF_STROKE_WIDTH = m_curTool.getSize() / 2;
                if (mPenType == PenType.Pen) {
                    mLine.setType(0);
                } else if (mPenType == PenType.Eraser) {
                    mLine.setType(1);
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
                }
                m_curTool.touchMove(eventX, eventY);
                break;
            case MotionEvent.ACTION_UP:
                mIsTouchUp = true;
                m_curTool.touchUp(event.getX(), event.getY());
                m_curTool.draw(m_canvas);
                savePoint(eventX, eventY);
                mLines.add(mLine);
                if (mFingerAction != null) {
                    mFingerAction.onFingerUp(mLine);
                }
                break;
        }
        invalidate(
                (int) (dirtyRect.left - HALF_STROKE_WIDTH),
                (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));
//        invalidate();
        lastTouchX = eventX;
        lastTouchY = eventY;
        return true;
    }

    /**
     * 手指是否抬起来
     */
    private boolean mIsTouchUp = false;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mPenType == PenType.Eraser) {
            if (!mIsTouchUp) {
                m_curTool.drawToastCircle(canvas, lastTouchX, lastTouchY);
                m_curTool.draw(m_canvas);
            }
        }
        if (null != m_foreBitmap) {
            canvas.drawBitmap(m_foreBitmap, 0, 0, m_bitmapPaint);
        }
        if (mPenType != PenType.Eraser) {
            if (!mIsTouchUp) {
                m_curTool.draw(canvas);
            }
        }
    }

    /**
     * 设置事件
     *
     * @param pAction
     */
    public void setFingerAction(OnFingerAction pAction) {
        this.mFingerAction = pAction;
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
            tPoint.setX(pBitmap.getWidth());
            tPoint.setY(pBitmap.getHeight());
            mPhotoSize = tPoint;
            this.setImageBitmap(pBitmap);
        }

    }

    /**
     * 设置图片宽高,可自定义
     *
     * @param pBitmap 图片
     * @param width   自定义宽度
     * @param height  自定义高度
     */
    public void setViewBackground(Bitmap pBitmap, float width, float height) {
        if (pBitmap != null) {
            clear();
            ViewPoint tPoint = new ViewPoint();
            if (width == 0f) {
                width = pBitmap.getWidth();
            }
            tPoint.setX(width);
            if (height == 0f) {
                height = pBitmap.getHeight();
            }
            tPoint.setY(height);
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
        SketchPadEraser tempEraser = new SketchPadEraser(m_eraserColor, m_eraserSize);
        tempEraser.drawRect(m_canvas, new Rect(0, 0, getWidth(), getHeight()));
        invalidate();
    }

    /**
     * 线的格式0代表笔1代表橡皮
     */
    public static final int LINE_TYPE_PEN_0 = 0, LINE_TYPE_ERASER_1 = 1;

    /**
     * 画线，根椐所有的线
     *
     * @param pLines
     */
    public void drawLines(List<Line> pLines) {
        if (pLines == null) {
            return;
        }
        mLines.addAll(pLines);
        for (Line pLine : pLines) {
            ISketchPadTool tempTool;
            if (pLine.getType() == LINE_TYPE_PEN_0) {
                int penSize = m_penSize;
                if (pLine.getWidth() != 0) {
                    penSize = pLine.getWidth();
                }
                int penColor = m_strokeColor;
                if (pLine.getColor() != 0) {
                    penColor = pLine.getColor();
                }
                tempTool = new SketchPadPen(penSize, penColor);
            } else if (pLine.getType() == LINE_TYPE_ERASER_1) {
                int penSize = m_eraserSize;
                if (pLine.getWidth() != 0) {
                    penSize = pLine.getWidth();
                }
                tempTool = new SketchPadEraser(m_eraserColor, penSize);
            } else {
                int penSize = m_penSize;
                if (pLine.getWidth() != 0) {
                    penSize = pLine.getWidth();
                }
                int penColor = m_strokeColor;
                if (pLine.getColor() != 0) {
                    penColor = pLine.getColor();
                }
                tempTool = new SketchPadPen(penSize, penColor);
            }
            Path tempPath = new Path();
            float mX = 0;
            float mY = 0;
            for (int i = 0; i < pLine.getPoints().size() - 1; i++) {
                ViewPoint tempPoint = pLine.getPoints().get(i);
                if (i == 0) {
                    mX = toViewAxisX(tempPoint.getX());
                    mY = toViewAxisY(tempPoint.getY());
                    tempPath.moveTo(mX, mY);
                } else {
                    float previousX = mX;
                    float previousY = mY;
                    ViewPoint nextPoint = pLine.getPoints().get(i + 1);
                    //设置贝塞尔曲线的操作点为起点和终点的一半
                    float cX = (mX + toViewAxisX(nextPoint.getX())) / 2;
                    float cY = (mY + toViewAxisY(nextPoint.getY())) / 2;
                    tempPath.quadTo(previousX, previousY, cX, cY);
                    mX = toViewAxisX(nextPoint.getX());
                    mY = toViewAxisY(nextPoint.getY());
                }
            }
            tempTool.drawPath(m_canvas, tempPath);
        }

    }

    /***
     * 设置是否能画线或或者设置文字
     *
     * @param pCanDraw true能画线false不能画
     */
    public void setCanDraw(boolean pCanDraw) {
        isCanDraw = pCanDraw;
    }

    /**
     * 设置文字颜色，画图片上的。
     *
     * @param pColor 色值，没有对错是不是色值
     */
    public void setTextColor(int pColor) {
        m_bitmapPaint.setColor(pColor);
    }

    /**
     * 设置文字大小
     *
     * @param pTextSize 大小
     */
    public void setTextSize(int pTextSize) {
        m_bitmapPaint.setTextSize(pTextSize);
    }

    /***
     * 返回当前控件截图，大小为控件大小
     *
     * @return bitmap
     */
    public Bitmap getScreenShot() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    /**
     * 图上显示文字的个数
     */
    private int mDrawTextCount = 3;

    /**
     * 设置画文字的个数，最长有几个字会画上去
     *
     * @param pCount
     */
    public void setDrawTextCount(int pCount) {
        if (pCount == 0) {
            pCount = 3;
        }
        this.mDrawTextCount = pCount;
    }

    /**
     * 在图上画文字
     * <p>
     * <p>
     * 步骤：
     *
     * @param x     X为图片中的X
     * @param y     y为图片中的Y
     * @param pText 作答文字
     */
    public void drawText(float x, float y, String pText) {
        drawText(x, y, pText, Color.BLUE);
    }

    public void drawText(float x, float y, String pText, int pColor) {
        drawText(x, y, pText, Color.BLUE, sp2px(14));
    }

    public void drawText(float x, float y, String pText, int pColor, int pTextSize) {
        if (!isCanDraw) {
            return;
        }
        x = toViewAxisX(x);
        y = toViewAxisY(y);
        //
        if (pText.length() >= mDrawTextCount) {
            pText = pText.substring(0, mDrawTextCount);
        }
        if (pText == null) {
            pText = "";
        }
        if (!TextUtils.isEmpty(pText)) {//空白不画东西
            float textWidth = m_bitmapPaint.measureText(pText);
            m_bitmapPaint.setColor(pColor);
            m_bitmapPaint.setTextSize(pTextSize);
            m_canvas.drawText(pText, x - textWidth / 2, y, m_bitmapPaint);
        }
        //
        invalidate();
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
        tPoint.setX(toPhotoAxisX(eventX));
        tPoint.setY(toPhotoAxisY(eventY));
        mLine.getPoints().add(tPoint);
    }


    /**
     * 转换为图片中的坐标
     *
     * @param x
     * @return
     */
    public float toPhotoAxisX(float x) {
        float vW = getWidth();
        float pW = mPhotoSize.getX();
        return pW * x / vW;
    }

    /**
     * 转换为图片中的Y
     *
     * @param y
     * @return
     */
    public float toPhotoAxisY(float y) {
        float vH = getHeight();
        float pH = mPhotoSize.getY();
        //
        return y * pH / vH;
    }


    /**
     * 转换成控件中的坐标
     *
     * @param x
     * @return
     */
    public float toViewAxisX(float x) {
        float vW = getWidth();
        float pW = mPhotoSize.getX();
        //
        return x * vW / pW;
    }

    /**
     * 转换为控件中的Y
     *
     * @param y
     * @return
     */
    public float toViewAxisY(float y) {
        float vH = getHeight();
        float pH = mPhotoSize.getY();
        //
        return y * vH / pH;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        createStrokeBitmap(w, h);
    }

    protected void initialize() {
        m_penSize = 2;//(int) getResources().getDimension(R.dimen.pen_size);
        m_eraserSize = 5 * m_penSize;
        m_canvas = new Canvas();
        m_bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        m_bitmapPaint.setColor(Color.BLUE);
        m_bitmapPaint.setStyle(Paint.Style.FILL);
        m_bitmapPaint.setTextSize(sp2px(14));
        m_bitmapPaint.setStrokeWidth(1);
        setStrokeType(PenType.Pen);
        HALF_STROKE_WIDTH = m_penSize / 2;
        mPhotoSize.setX(640);
        mPhotoSize.setY(480);
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }


    public PenType getPenType() {
        return mPenType;
    }

    public void setPenType(PenType pPenType) {
        mPenType = pPenType;
    }

    /**
     * 设置画笔颜色
     *
     * @param pColor 色值
     */
    public void setPenColor(int pColor) {
        this.m_strokeColor = pColor;
        if (mPenType == PenType.Pen) {
            m_curTool = new SketchPadPen(m_penSize, m_strokeColor);
        }
    }

    /**
     * 设置画笔大小
     *
     * @param size 大小
     */
    public void setPenSize(int size) {
        this.m_penSize = size;
        if (mPenType == PenType.Pen) {
            m_curTool = new SketchPadPen(m_penSize, m_strokeColor);
        }
    }

    /**
     * 设置橡皮大小
     *
     * @param pSize
     */
    public void setEraserSize(int pSize) {
        this.m_eraserSize = pSize;
        if (mPenType == PenType.Eraser) {
            m_curTool = new SketchPadEraser(m_eraserColor, m_eraserSize);
        }
    }

    /**
     * 设置画笔颜色
     *
     * @param pColor 色值
     */
    public void setEraserColor(int pColor) {
        this.m_strokeColor = pColor;
        if (mPenType == PenType.Eraser) {
            m_curTool = new SketchPadEraser(m_eraserColor, m_eraserSize);
        }
    }


    private int sp2px(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getContext().getResources().getDisplayMetrics());
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
     * 手指事件
     */
    public interface OnFingerAction {
        /**
         * 手指抬起
         *
         * @param pLine
         */
        public void onFingerUp(Line pLine);
    }
}
