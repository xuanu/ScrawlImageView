package views.zeffect.cn.scrawlviewlib.panel;

import android.content.Context;
import android.content.res.TypedArray;
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

import java.util.ArrayList;
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
            return true;
        }
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
            tPoint.x = width;
            if (height == 0f) {
                height = pBitmap.getHeight();
            }
            tPoint.y = height;
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
        for (Line pLine : pLines) {
            ISketchPadTool tempTool;
            if (pLine.type == LINE_TYPE_PEN_0) {
                int penSize = m_penSize;
                if (pLine.width != 0) {
                    penSize = pLine.width;
                }
                int penColor = m_strokeColor;
                if (pLine.color != 0) {
                    penColor = pLine.color;
                }
                tempTool = new SketchPadPen(penSize, penColor);
            } else if (pLine.type == LINE_TYPE_ERASER_1) {
                int penSize = m_eraserSize;
                if (pLine.width != 0) {
                    penSize = pLine.width;
                }
                tempTool = new SketchPadEraser(m_eraserColor, penSize);
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
     * 用来保存用户回答的问题，便于清除
     */
    private HashMap<Integer, String> mSaveAnswer = new HashMap<>();

    /**
     * 在图上画文字
     * <p>
     * 可能存在问题。两个问题之间太近就会存在问题。
     * 步骤：
     * 1.检测上一次有无作答：有，擦掉。
     * 2.画本次问题，text为空，画题号
     *
     * @param index 题号 从0开始默认+1
     * @param x     X为图片中的X
     * @param y     y为图片中的Y
     * @param pText 作答文字
     */
    public void drawText(int index, float x, float y, String pText) {
        if (!isCanDraw) {
            return;
        }
        x = toViewAxisX(x);
        y = toViewAxisY(y);
        float temp1Dp = 1;
        //有无历史答案，有的话要擦掉
        if (mSaveAnswer.containsKey(index)) {
            SketchPadEraser tempEraser = new SketchPadEraser(m_eraserColor, m_eraserSize);
            String tempText = mSaveAnswer.get(index);
            float textWidth = m_bitmapPaint.measureText(tempText);
            double left = x - textWidth / 2 - temp1Dp;
            double top = y + m_bitmapPaint.getFontMetrics().top - temp1Dp;
            double right = x + textWidth / 2 + temp1Dp;
            double bottom = y + temp1Dp + m_bitmapPaint.getFontMetrics().bottom;
            Rect tRectF = new Rect((int) Math.floor(left), (int) Math.floor(top), (int) Math.ceil(right), (int) Math.ceil(bottom));
            tempEraser.drawRect(m_canvas, tRectF);
            invalidate(tRectF);
        }
        //
        if (pText.length() >= mDrawTextCount) {
            pText = pText.substring(0, mDrawTextCount);
        }
        if (pText == null) {
            pText = "";
        }
        String drawText = String.valueOf(index + 1) + "." + pText;
        if (!TextUtils.isEmpty(drawText)) {//空白不画东西
            float textWidth = m_bitmapPaint.measureText(drawText);
            m_canvas.drawText(drawText, x - textWidth / 2, y, m_bitmapPaint);
        }
        //
        mSaveAnswer.put(index, drawText);
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
    public float toPhotoAxisY(float y) {
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
    public float toViewAxisX(float x) {
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
    public float toViewAxisY(float y) {
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
        m_bitmapPaint.setColor(Color.BLUE);
        m_bitmapPaint.setStyle(Paint.Style.FILL);
        m_bitmapPaint.setTextSize(sp2px(14));
        m_bitmapPaint.setStrokeWidth(1);
        setStrokeType(PenType.Pen);
        HALF_STROKE_WIDTH = m_penSize / 2;
        mPhotoSize.x = 1280;
        mPhotoSize.y = 800;
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

        public ArrayList<ViewPoint> getPoints() {
            return mPoints;
        }

        public void setPoints(ArrayList<ViewPoint> pPoints) {
            mPoints = pPoints;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int pColor) {
            color = pColor;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int pWidth) {
            width = pWidth;
        }

        public int getType() {
            return type;
        }

        public void setType(int pType) {
            type = pType;
        }
    }

    /**
     * 点
     */
    public class ViewPoint {
        /**
         * X坐标
         */
        private float x;
        /**
         * Y坐标
         */
        private float y;

        public float getX() {
            return x;
        }

        public void setX(float pX) {
            x = pX;
        }

        public float getY() {
            return y;
        }

        public void setY(float pY) {
            y = pY;
        }
    }
}
