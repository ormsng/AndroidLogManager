package com.example.logviewer;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private List<LogEntry> logs = new ArrayList<>();
    private List<LogEntry> filteredLogs = new ArrayList<>();
    private String currentSearchQuery = "";
    private TextInputEditText searchEditText;
    private Set<LogType> activeLogTypes = new HashSet<>(Arrays.asList(LogType.values()));
    private Calendar selectedDateTime = Calendar.getInstance();

    private LinearLayout checkboxContainer;
    private CheckBox checkBoxAll;
    private Map<LogType, CheckBox> logTypeCheckBoxes = new HashMap<>();
    private Button btnDatePicker, btnTimePicker, btnExportCsv, btnClearLogs;
    private MaterialButtonToggleGroup visualizationToggleGroup;
    private TextView tvSelectedDateTime, tvLogCounts;

    private VisualizationType currentVisualization = VisualizationType.PLAIN_TEXT;

    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private BroadcastReceiver logUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LogService.ACTION_LOG_UPDATED.equals(intent.getAction())) {
                LogEntry logEntry = (LogEntry) intent.getSerializableExtra(LogService.EXTRA_LOG);
                logs.add(logEntry);
                filterLogs();
                updateLogCounts();
                Log.d("MainActivity", "Log entry added: " + logEntry.getMessage());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();
        setupRecyclerView();
        setupListeners();
        setupBroadcastReceiver();

        updateSelectedDateTimeText();
        filterLogs();
        updateLogCounts();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        checkboxContainer = findViewById(R.id.checkboxContainer);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        btnTimePicker = findViewById(R.id.btnTimePicker);
        tvSelectedDateTime = findViewById(R.id.tvSelectedDateTime);
        btnExportCsv = findViewById(R.id.btnExportCsv);
        btnClearLogs = findViewById(R.id.btnClearLogs);
        visualizationToggleGroup = findViewById(R.id.visualizationToggleGroup);
        tvLogCounts = findViewById(R.id.tvLogCounts);

        setupCheckboxes();
    }

    private void setupCheckboxes() {
        checkBoxAll = new CheckBox(this);
        checkBoxAll.setText("All");
        checkBoxAll.setChecked(true);
        checkboxContainer.addView(checkBoxAll);

        LinearLayout row1 = new LinearLayout(this);
        LinearLayout row2 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        checkboxContainer.addView(row1);
        checkboxContainer.addView(row2);

        LogType[] logTypes = LogType.values();
        for (int i = 0; i < logTypes.length; i++) {
            LogType logType = logTypes[i];
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(logType.name());
            checkBox.setChecked(true);
            logTypeCheckBoxes.put(logType, checkBox);

            if (i < 4) {
                row1.addView(checkBox);
            } else {
                row2.addView(checkBox);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new LogAdapter(filteredLogs);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        setupCheckBoxListeners();
        setupSearchListener();
        setupDateTimeListeners();
        setupExportButton();
        setupClearLogsButton();
        setupVisualizationToggle();
    }

    private void setupCheckBoxListeners() {
        checkBoxAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (CheckBox checkBox : logTypeCheckBoxes.values()) {
                checkBox.setChecked(isChecked);
                checkBox.setEnabled(!isChecked);
            }
            updateActiveLogTypes();
        });

        for (CheckBox checkBox : logTypeCheckBoxes.values()) {
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateActiveLogTypes();
                checkBoxAll.setChecked(areAllTypeCheckBoxesChecked());
            });
        }
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                filterLogs();
            }
        });
    }

    private void setupDateTimeListeners() {
        btnDatePicker.setOnClickListener(v -> showDatePicker());
        btnTimePicker.setOnClickListener(v -> showTimePicker());
    }

    private void setupExportButton() {
        btnExportCsv.setOnClickListener(v -> exportLogsToCSV());
    }

    private void setupClearLogsButton() {
        btnClearLogs.setOnClickListener(v -> clearLogs());
    }

    private void setupVisualizationToggle() {
        visualizationToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnPlainText) {
                    currentVisualization = VisualizationType.PLAIN_TEXT;
                } else if (checkedId == R.id.btnPieChart) {
                    currentVisualization = VisualizationType.PIE_CHART;
                } else if (checkedId == R.id.btnBarGraph) {
                    currentVisualization = VisualizationType.BAR_GRAPH;
                }
                adapter.setVisualizationType(currentVisualization);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void setupBroadcastReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(logUpdateReceiver,
                new IntentFilter(LogService.ACTION_LOG_UPDATED));
        Intent intent = new Intent(this, LogService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logUpdateReceiver);
        Intent intent = new Intent(this, LogService.class);
        stopService(intent);
    }

    private void updateActiveLogTypes() {
        activeLogTypes.clear();
        if (checkBoxAll.isChecked()) {
            activeLogTypes.addAll(Arrays.asList(LogType.values()));
        } else {
            for (Map.Entry<LogType, CheckBox> entry : logTypeCheckBoxes.entrySet()) {
                if (entry.getValue().isChecked()) {
                    activeLogTypes.add(entry.getKey());
                }
            }
        }
        filterLogs();
    }

    private boolean areAllTypeCheckBoxesChecked() {
        for (CheckBox checkBox : logTypeCheckBoxes.values()) {
            if (!checkBox.isChecked()) {
                return false;
            }
        }
        return true;
    }

    private void filterLogs() {
        filteredLogs.clear();
        for (LogEntry log : logs) {
            if (activeLogTypes.contains(log.getType()) &&
                    (currentSearchQuery.isEmpty() || log.getMessage().toLowerCase().contains(currentSearchQuery)) &&
                    isLogWithinSelectedDateTime(log)) {
                filteredLogs.add(log);
            }
        }
        adapter.notifyDataSetChanged();
        updateLogCounts();
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateSelectedDateTimeText();
                    filterLogs();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    updateSelectedDateTimeText();
                    filterLogs();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void updateSelectedDateTimeText() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        tvSelectedDateTime.setText("Selected: " + sdf.format(selectedDateTime.getTime()));
    }

    private boolean isLogWithinSelectedDateTime(LogEntry log) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        try {
            Date logDate = sdf.parse(log.getTimestamp());
            return !logDate.before(selectedDateTime.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void exportLogsToCSV() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            return;
        }

        File exportDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LogExports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "logs_export_" + System.currentTimeMillis() + ".csv");

        try {
            FileWriter fw = new FileWriter(file);
            fw.append("Timestamp,Type,Message\n");

            for (LogEntry log : filteredLogs) {
                fw.append(log.getTimestamp())
                        .append(",")
                        .append(log.getType().name())
                        .append(",")
                        .append(log.getMessage().replace(",", ";")) // Escape commas in the message
                        .append("\n");
            }

            fw.flush();
            fw.close();

            Snackbar.make(recyclerView, "Logs exported to Downloads/LogExports", Snackbar.LENGTH_LONG)
                    .setAction("Open", v -> {
                        // Implement file opening logic here
                    })
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting logs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportLogsToCSV();
            } else {
                Toast.makeText(this, "Permission denied. Cannot export logs.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void clearLogs() {
        logs.clear();
        filterLogs();
        Snackbar.make(recyclerView, "All logs cleared", Snackbar.LENGTH_SHORT).show();
    }

    private void updateLogCounts() {
        Map<LogType, Integer> counts = new HashMap<>();
        for (LogEntry log : filteredLogs) {
            counts.put(log.getType(), counts.getOrDefault(log.getType(), 0) + 1);
        }

        StringBuilder sb = new StringBuilder("Log Counts: ");
        for (LogType type : LogType.values()) {
            sb.append(type.name()).append(": ").append(counts.getOrDefault(type, 0)).append(" ");
        }
        tvLogCounts.setText(sb.toString());
    }
}