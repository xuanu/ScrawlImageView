package views.zeffect.cn.scrawlviewlib.panel;

import java.util.ArrayList;

/**
 * Created by zeffect on 2016/12/26.
 *
 * @author zzx
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
