package com.alertalinkedin;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alertalinkedin.db.AppDatabase;
import com.alertalinkedin.db.KeywordEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIF_PERMISSION_CODE = 1001;

    private static final String[] INTERVAL_LABELS = {"1 minuto", "5 minutos", "10 minutos", "15 minutos", "30 minutos", "1 hora"};
    private static final long[]   INTERVAL_VALUES  = {1, 5, 10, 15, 30, 60};

    private KeywordAdapter adapter;
    private SwitchMaterial monitorSwitch;
    private TextView statusText, statusSubText;
    private RecyclerView recyclerView;
    private View emptyView;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        db = AppDatabase.getInstance(this);
        JobNotifier.createChannel(this);

        recyclerView  = findViewById(R.id.recyclerKeywords);
        emptyView     = findViewById(R.id.emptyView);
        monitorSwitch = findViewById(R.id.monitorSwitch);
        statusText    = findViewById(R.id.statusText);
        statusSubText = findViewById(R.id.statusSubText);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        adapter = new KeywordAdapter(keyword ->
            new Thread(() -> db.keywordDao().delete(keyword)).start()
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        db.keywordDao().getAll().observe(this, keywords -> {
            adapter.setKeywords(keywords);
            boolean empty = keywords.isEmpty();
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        boolean active = PrefsManager.isMonitoringActive(this);
        monitorSwitch.setChecked(active);
        updateStatusCard(active);

        // Retoma o serviço se estava ativo antes de fechar o app
        if (active) MonitoringService.start(this);

        monitorSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            PrefsManager.setMonitoringActive(this, isChecked);
            if (isChecked) {
                requestNotificationPermission();
                MonitoringService.start(this);
                Toast.makeText(this, "Monitoramento ativado", Toast.LENGTH_SHORT).show();
            } else {
                MonitoringService.stop(this);
                Toast.makeText(this, "Monitoramento desativado", Toast.LENGTH_SHORT).show();
            }
            updateStatusCard(isChecked);
        });

        fabAdd.setOnClickListener(v -> showAddKeywordDialog());
    }

    private void showAddKeywordDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_keyword, null);
        EditText input = view.findViewById(R.id.keywordInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Adicionar palavra-chave")
            .setView(view)
            .setPositiveButton("Adicionar", (d, w) -> {
                String kw = input.getText().toString().trim();
                if (!kw.isEmpty()) {
                    new Thread(() -> db.keywordDao().insert(new KeywordEntity(kw))).start();
                }
            })
            .setNegativeButton("Cancelar", null)
            .create();

        dialog.show();
        input.post(() -> {
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void showSettingsDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        Spinner countrySpinner   = view.findViewById(R.id.countrySpinner);
        Spinner timeRangeSpinner = view.findViewById(R.id.timeRangeSpinner);
        Spinner intervalSpinner  = view.findViewById(R.id.intervalSpinner);

        String[] countries        = getResources().getStringArray(R.array.countries);
        String[] timeRangeLabels  = getResources().getStringArray(R.array.time_range_labels);
        String[] timeRangeValues  = getResources().getStringArray(R.array.time_range_values);

        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, countries);
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(countryAdapter);

        String savedLocation = PrefsManager.getLocation(this);
        for (int i = 0; i < countries.length; i++) {
            if (countries[i].equalsIgnoreCase(savedLocation)) {
                countrySpinner.setSelection(i);
                break;
            }
        }

        ArrayAdapter<String> timeRangeAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, timeRangeLabels);
        timeRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeRangeSpinner.setAdapter(timeRangeAdapter);

        String savedRange = PrefsManager.getTimeRange(this);
        for (int i = 0; i < timeRangeValues.length; i++) {
            if (timeRangeValues[i].equals(savedRange)) {
                timeRangeSpinner.setSelection(i);
                break;
            }
        }

        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, INTERVAL_LABELS);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalSpinner.setAdapter(intervalAdapter);

        long current = PrefsManager.getIntervalMinutes(this);
        for (int i = 0; i < INTERVAL_VALUES.length; i++) {
            if (INTERVAL_VALUES[i] == current) { intervalSpinner.setSelection(i); break; }
        }

        new AlertDialog.Builder(this)
            .setTitle("Configurações")
            .setView(view)
            .setPositiveButton("Salvar", (d, w) -> {
                String selectedCountry = countries[countrySpinner.getSelectedItemPosition()];
                PrefsManager.setLocation(this, selectedCountry);

                String selectedRange = timeRangeValues[timeRangeSpinner.getSelectedItemPosition()];
                PrefsManager.setTimeRange(this, selectedRange);

                int idx = intervalSpinner.getSelectedItemPosition();
                PrefsManager.setIntervalMinutes(this, INTERVAL_VALUES[idx]);

                if (PrefsManager.isMonitoringActive(this)) {
                    MonitoringService.stop(this);
                    MonitoringService.start(this);
                }
                updateStatusCard(PrefsManager.isMonitoringActive(this));
                Toast.makeText(this, "Configurações salvas", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void updateStatusCard(boolean active) {
        if (active) {
            statusText.setText("Monitoramento ativo");
            long interval = PrefsManager.getIntervalMinutes(this);
            String label;
            if (interval == 1) label = "1 minuto";
            else if (interval == 60) label = "1 hora";
            else label = interval + " minutos";

            statusSubText.setText("Verificando a cada " + label);
        } else {
            statusText.setText("Monitoramento desativado");
            statusSubText.setText("Ative para receber alertas de vagas");
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIF_PERMISSION_CODE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
