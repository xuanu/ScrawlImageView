package views.zeffect.cn.scrawlviewlib.panel;


import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;

public interface ISketchPadTool {
    public void draw(Canvas canvas);

    public void cleanAll();

    public void touchDown(float x, float y);

    public void touchMove(float x, float y);

    public void touchUp(float x, float y);

    public void drawRect(Canvas pCanvas, Rect pRect);

    public int getColor();

    public float getSize();

    public void drawPath(Canvas pCanvas, Path pPath);

    public void drawToastCircle(Canvas pCanvas,float x,float y);
}
