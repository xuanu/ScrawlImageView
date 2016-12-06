package views.zeffect.cn.scrawlimageview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.LinkedList;
import java.util.List;

import views.zeffect.cn.scrawlviewlib.panel.SketchPadView;

public class MainActivity extends AppCompatActivity {
    SketchPadView mSketchPadView;
    List<SketchPadView.Line> tempLines = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSketchPadView = (SketchPadView) findViewById(R.id.scrawlview);
        Button tButton1 = (Button) findViewById(R.id.pen);
        Button tButton2 = (Button) findViewById(R.id.eraser);
        tButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSketchPadView.setStrokeType(SketchPadView.PenType.Pen);
            }
        });
        tButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSketchPadView.setStrokeType(SketchPadView.PenType.Eraser);
            }
        });
        final Button tempButton3 = (Button) findViewById(R.id.scale);
        tempButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mSketchPadView.setStrokeType(SketchPadView.PenType.Scale);
                tempLines.addAll(mSketchPadView.getLines());
                mSketchPadView.clear();
                mSketchPadView.drawLines(tempLines);
                tempLines.clear();
            }
        });
    }
}
