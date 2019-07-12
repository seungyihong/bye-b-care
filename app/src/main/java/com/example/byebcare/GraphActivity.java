package com.example.byebcare;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.util.Log;
import android.provider.ContactsContract;

import butterknife.BindView;
import butterknife.ButterKnife;
import org.json.JSONException;
import org.json.JSONObject;

import com.anychart.APIlib;
import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Column;
import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.Anchor;
import com.anychart.enums.MarkerType;
import com.anychart.enums.Orientation;
import com.anychart.enums.Position;
import com.anychart.enums.TooltipPositionMode;
import com.anychart.graphics.vector.Stroke;
import com.anychart.graphics.vector.StrokeLineCap;
import com.anychart.graphics.vector.StrokeLineJoin;
import com.anychart.scales.Linear;
import com.anychart.core.cartesian.series.Base;
import com.github.mikephil.charting.renderer.scatter.ChevronUpShapeRenderer;


import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

import com.example.byebcare.BioDataContract.BioDataDbHelper;
import com.example.byebcare.BioDataContract.BioDataEntry;


public class GraphActivity extends AppCompatActivity {

    private JSONObject jsonObject;
    private double tempAmb;
    private double tempB;
    private int bpm;
    private BioDataDbHelper bioDataDbHelper;
    static int currnettime = 0 ;

    @BindView(R.id.any_chart_view) AnyChartView anyChartView;

    public GraphActivity(){

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        ButterKnife.bind(this);
        anyChartView.setProgressBar(findViewById(R.id.progress_bar));
        bioDataDbHelper = BioDataDbHelper.getInstance(this);

        SQLiteDatabase bioDB = bioDataDbHelper.getWritableDatabase();

        Cursor c = bioDB.query(
                BioDataEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                BioDataEntry.COLUMN_NAME_TIME+" DESC",
                "10");

        Cartesian cartesian = AnyChart.line();
       //Cartesian cartesian2 = AnyChart.column();

        cartesian.animation(true);

        cartesian.padding(5d, 10d, 5d, 10d);

        cartesian.crosshair().enabled(true);
        cartesian.crosshair()
                .yLabel(true)
                // TODO ystroke
                .yStroke((Stroke) null, null, null, (String) null, (String) null);

        cartesian.tooltip().positionMode(TooltipPositionMode.POINT);
        cartesian.title("<Record of your baby condition>");
        cartesian.yAxis(0).title("Temperature(celsius)");

        cartesian.lineMarker(0)    //체온 라인
                .value(80d)
                .axis(cartesian.yAxis(0))
                .stroke("#A5B3B3",1d,"5 2", StrokeLineJoin.ROUND, StrokeLineCap.ROUND);
        cartesian.lineMarker(1)    // 맥박 라인
                .value(20d)
                .axis(cartesian.yAxis(1))
                .stroke("#A5B3B3",1d,"5 2", StrokeLineJoin.ROUND, StrokeLineCap.ROUND);

        Linear scalesLinear = Linear.instantiate();   //체온 수치
        scalesLinear.minimum(0d);
        scalesLinear.maximum(50d);
        scalesLinear.ticks("{interval:5}");

        Linear scalesLinear2 = Linear.instantiate();  //맥박 수치
        scalesLinear2.minimum(40d);
        scalesLinear2.maximum(220d);
        scalesLinear2.ticks("{interval:10}");
        
        cartesian.yAxis(0).scale(scalesLinear);

        com.anychart.core.axes.Linear extraYAxis = cartesian.yAxis(1);
        cartesian.yAxis(1).title("Pulse(number/minute)");
        extraYAxis.orientation(Orientation.RIGHT)
                .scale(scalesLinear2);
        extraYAxis.labels()
                .padding(0d,0d,0d,5d);

        cartesian.xAxis(0).labels().padding(5d, 5d, 5d, 5d);


        List<DataEntry> seriesData = new ArrayList<>();


        while(!c.isAfterLast()) {

            tempAmb= c.getType(c.getColumnIndex(BioDataEntry.COLUMN_NAME_AMBIENT_TEMPERATURE));
            tempB = c.getType(c.getColumnIndex(BioDataEntry.COLUMN_NAME_BABY_TEMPERATURE));
            bpm=c.getType(c.getColumnIndex(BioDataEntry.COLUMN_NAME_BPM));

            seriesData.add(new ValueDataEntry("tempAmb", tempAmb));
            seriesData.add(new ValueDataEntry("tempB", tempB));
            seriesData.add(new ValueDataEntry("bpm", bpm));

            c.moveToNext();
        }

        Set set = Set.instantiate();
        set.data(seriesData);
        Mapping tempAmbData = set.mapAs("{ time1: 'time1', value: 'tempAmb' }");
        Mapping tempBData = set.mapAs("{ time1: 'time1', value: 'tempB' }");
        Mapping PulseData = set.mapAs("{ time1: 'time1', value: 'bpm' }" );


        Line seriesA = cartesian.line(tempAmbData);
        seriesA.yScale(scalesLinear);
        seriesA.name("Ambient.t");
        seriesA.hovered().markers().enabled(true);
        seriesA.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(5d);
        seriesA.tooltip()
                .position("right")
                .anchor(Anchor.CENTER)
                .offsetX(5d)
                .offsetY(5d);

        Line seriesB = cartesian.line(tempBData);
        seriesB.yScale(scalesLinear);
        seriesB.name("Baby.t");
        seriesB.hovered().markers().enabled(true);
        seriesB.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(5d);
        seriesB.tooltip()
                .position("left")
                .anchor(Anchor.CENTER)
                .offsetX(5d)
                .offsetY(5d);

        Column seriesP = cartesian.column(PulseData);
        seriesP.yScale(scalesLinear2);
        seriesP.name("BPM");
        seriesP.hovered().markers().enabled(true);
        seriesP.hovered().markers()
                .type(MarkerType.CIRCLE)
                .size(5d);
        seriesP.tooltip()
                .position(Position.CENTER_BOTTOM)
                .anchor(Anchor.CENTER_BOTTOM)
                .offsetX(0d)
                .offsetY(5d)
                .format("{Value}{groupsSeparator:}");

        cartesian.legend().enabled(true);
        cartesian.legend().fontSize(15d);
        cartesian.legend().padding(0d, 0d, 15d, 0d);

        anyChartView.setChart(cartesian);

    }



    }

    /*private class CustomDataEntry extends ValueDataEntry {

        CustomDataEntry(String time1,Number tempAmb, Number tempB, Number bpm) {
            super(time1, tempAmb);
            setValue("tempB", tempB);
            setValue("bpm", bpm);
        }

    }*/



