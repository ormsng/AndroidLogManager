package com.example.logviewer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<LogEntry> logs;
    private VisualizationType visualizationType = VisualizationType.PLAIN_TEXT;

    private static final int COLOR_DEBUG = Color.rgb(0, 0, 255);    // Blue
    private static final int COLOR_INFO = Color.rgb(0, 255, 0);     // Green
    private static final int COLOR_WARNING = Color.rgb(255, 165, 0); // Orange
    private static final int COLOR_ERROR = Color.rgb(255, 0, 0);    // Red
    private static final int COLOR_VERBOSE = Color.rgb(128, 0, 128); // Purple

    public LogAdapter(List<LogEntry> logs) {
        this.logs = logs;
    }

    @Override
    public int getItemViewType(int position) {
        return visualizationType.ordinal();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (VisualizationType.values()[viewType]) {
            case PIE_CHART:
                View pieChartView = inflater.inflate(R.layout.pie_chart_item, parent, false);
                return new PieChartViewHolder(pieChartView);
            case BAR_GRAPH:
                View barGraphView = inflater.inflate(R.layout.bar_graph_item, parent, false);
                return new BarGraphViewHolder(barGraphView);
            case PLAIN_TEXT:
            default:
                View textView = inflater.inflate(R.layout.log_item, parent, false);
                return new LogViewHolder(textView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (visualizationType) {
            case PIE_CHART:
                ((PieChartViewHolder) holder).bind(logs);
                break;
            case BAR_GRAPH:
                ((BarGraphViewHolder) holder).bind(logs);
                break;
            case PLAIN_TEXT:
            default:
                ((LogViewHolder) holder).bind(logs.get(position));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return visualizationType == VisualizationType.PLAIN_TEXT ? logs.size() : 1;
    }

    public void setVisualizationType(VisualizationType type) {
        this.visualizationType = type;
        notifyDataSetChanged();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView logTextView;

        LogViewHolder(View itemView) {
            super(itemView);
            logTextView = itemView.findViewById(R.id.logTextView);
        }

        void bind(LogEntry log) {
            logTextView.setText(log.getMessage());
            logTextView.setTextColor(getColorForLogType(log.getType()));
        }

        private int getColorForLogType(LogType type) {
            switch (type) {
                case DEBUG: return COLOR_DEBUG;
                case INFO: return COLOR_INFO;
                case WARNING: return COLOR_WARNING;
                case ERROR: return COLOR_ERROR;
                case VERBOSE: return COLOR_VERBOSE;
                default: return Color.BLACK;
            }
        }
    }

    static class PieChartViewHolder extends RecyclerView.ViewHolder {
        PieChart pieChart;

        PieChartViewHolder(View itemView) {
            super(itemView);
            pieChart = itemView.findViewById(R.id.pieChart);
        }

        void bind(List<LogEntry> logs) {
            Map<LogType, Integer> logTypeCounts = new HashMap<>();
            for (LogEntry log : logs) {
                logTypeCounts.put(log.getType(), logTypeCounts.getOrDefault(log.getType(), 0) + 1);
            }

            List<PieEntry> entries = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            for (Map.Entry<LogType, Integer> entry : logTypeCounts.entrySet()) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey().name()));
                colors.add(getColorForLogType(entry.getKey()));
            }

            PieDataSet dataSet = new PieDataSet(entries, "Log Types");
            dataSet.setColors(colors);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);
            PieData data = new PieData(dataSet);

            pieChart.setData(data);
            pieChart.getDescription().setEnabled(false);
            pieChart.setUsePercentValues(true);
            pieChart.setExtraOffsets(5, 10, 5, 5);
            pieChart.setDragDecelerationFrictionCoef(0.95f);
            pieChart.setDrawHoleEnabled(true);
            pieChart.setHoleColor(Color.WHITE);
            pieChart.setTransparentCircleRadius(61f);
            pieChart.setDrawCenterText(true);
            pieChart.setCenterText("Log Types");
            pieChart.setCenterTextSize(16f);
            pieChart.animateY(1000);

            pieChart.invalidate();
        }

        private int getColorForLogType(LogType type) {
            switch (type) {
                case DEBUG: return COLOR_DEBUG;
                case INFO: return COLOR_INFO;
                case WARNING: return COLOR_WARNING;
                case ERROR: return COLOR_ERROR;
                case VERBOSE: return COLOR_VERBOSE;
                default: return Color.BLACK;
            }
        }
    }

    static class BarGraphViewHolder extends RecyclerView.ViewHolder {
        BarChart barChart;

        BarGraphViewHolder(View itemView) {
            super(itemView);
            barChart = itemView.findViewById(R.id.barChart);
        }

        void bind(List<LogEntry> logs) {
            Map<LogType, Integer> logTypeCounts = new HashMap<>();
            for (LogEntry log : logs) {
                logTypeCounts.put(log.getType(), logTypeCounts.getOrDefault(log.getType(), 0) + 1);
            }

            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            int index = 0;
            for (Map.Entry<LogType, Integer> entry : logTypeCounts.entrySet()) {
                entries.add(new BarEntry(index, entry.getValue()));
                labels.add(entry.getKey().name());
                colors.add(getColorForLogType(entry.getKey()));
                index++;
            }

            BarDataSet dataSet = new BarDataSet(entries, "Log Types");
            dataSet.setColors(colors);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.BLACK);
            BarData data = new BarData(dataSet);
            data.setBarWidth(0.9f);

            barChart.setData(data);
            barChart.getDescription().setEnabled(false);
            barChart.setDrawValueAboveBar(true);
            barChart.setMaxVisibleValueCount(5);
            barChart.setPinchZoom(false);
            barChart.setDrawGridBackground(false);

            XAxis xAxis = barChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);
            xAxis.setLabelCount(labels.size());
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

            YAxis leftAxis = barChart.getAxisLeft();
            leftAxis.setLabelCount(8, false);
            leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
            leftAxis.setSpaceTop(15f);
            leftAxis.setAxisMinimum(0f);

            barChart.getAxisRight().setEnabled(false);
            barChart.getLegend().setEnabled(false);
            barChart.animateY(1000);

            barChart.invalidate();
        }

        private int getColorForLogType(LogType type) {
            switch (type) {
                case DEBUG: return COLOR_DEBUG;
                case INFO: return COLOR_INFO;
                case WARNING: return COLOR_WARNING;
                case ERROR: return COLOR_ERROR;
                case VERBOSE: return COLOR_VERBOSE;
                default: return Color.BLACK;
            }
        }
    }
}