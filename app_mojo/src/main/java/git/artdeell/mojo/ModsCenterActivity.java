
package git.artdeell.mojo;

import android.os.Bundle;
import android.widget.*;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModsCenterActivity extends AppCompatActivity {

    EditText search;
    Spinner source, version, loader, category;
    LinearLayout results;
    TextView empty;
    ProgressBar loading;
    Button searchButton, back;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    String[] sources = {"Modrinth", "CurseForge"};
    String[] versions = {"1.21.11", "1.21.10", "1.21.1", "1.20.1"};
    String[] loaders = {"Fabric", "Forge", "NeoForge", "Quilt"};
    String[] categories = {"Hamısı", "Adventure", "Technology",
            "Magic", "Optimization", "Library", "Utility"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mods_center);

        search = findViewById(R.id.mod_search);
        source = findViewById(R.id.mod_source);
        version = findViewById(R.id.mod_version);
        loader = findViewById(R.id.mod_loader);
        category = findViewById(R.id.mod_category);
        results = findViewById(R.id.mod_results);
        empty = findViewById(R.id.mod_empty);
        loading = findViewById(R.id.mod_loading);
        searchButton = findViewById(R.id.mod_search_button);
        back = findViewById(R.id.mod_back);

        setupSpinner(source, sources);
        setupSpinner(version, versions);
        setupSpinner(loader, loaders);
        setupSpinner(category, categories);

        searchButton.setOnClickListener(v -> searchMods());
        back.setOnClickListener(v -> finish());
    }

    private void setupSpinner(Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void searchMods() {
        String query = search.getText().toString().trim();

        if (query.isEmpty()) {
            search.setError("Mod adını yaz");
            return;
        }

        if (source.getSelectedItemPosition() != 0) {
            Toast.makeText(this,
                    "CurseForge üçün server bağlantısı lazımdır",
                    Toast.LENGTH_LONG).show();
            return;
        }

        loading.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        results.removeAllViews();
        searchButton.setEnabled(false);

        String mcVersion = version.getSelectedItem().toString();
        String modLoader = loader.getSelectedItem().toString()
                .toLowerCase();

        String selectedCategory =
                category.getSelectedItem().toString();

        executor.execute(() -> {
            try {
                String urlText =
                    "https://api.modrinth.com/v2/search?query="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&facets="
                    + URLEncoder.encode(
                        "[[\"versions:" + mcVersion
                        + "\"],[\"categories:" + modLoader + "\"]]",
                        "UTF-8")
                    + "&limit=20";

                URL url = new URL(urlText);
                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty(
                        "User-Agent", "KarabakhLauncher/1.0");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream()));

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                JSONObject data =
                        new JSONObject(response.toString());

                JSONArray hits = data.getJSONArray("hits");

                runOnUiThread(() -> {
                    loading.setVisibility(View.GONE);
                    searchButton.setEnabled(true);

                    if (hits.length() == 0) {
                        empty.setText("Mod tapılmadı.");
                        empty.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (int i = 0; i < hits.length(); i++) {
                        try {
                            JSONObject mod = hits.getJSONObject(i);

                            String title = mod.optString(
                                    "title", "Naməlum mod");

                            String author = mod.optString(
                                    "author", "Naməlum müəllif");

                            String description = mod.optString(
                                    "description", "");

                            String downloads = mod.optString(
                                    "downloads", "0");

                            TextView item = new TextView(this);

                            item.setText(
                                    title + "\n"
                                    + author + "\n"
                                    + description + "\n"
                                    + "Yüklənmə: " + downloads
                            );

                            item.setTextColor(0xFFFFFFFF);
                            item.setTextSize(14);
                            item.setPadding(20, 20, 20, 20);

                            item.setBackgroundColor(0xFF21152F);

                            LinearLayout.LayoutParams params =
                                    new LinearLayout.LayoutParams(
                                            -1, -2);

                            params.setMargins(0, 8, 0, 8);
                            item.setLayoutParams(params);

                            results.addView(item);

                        } catch (Exception ignored) {
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    loading.setVisibility(View.GONE);
                    searchButton.setEnabled(true);
                    empty.setText(
                            "Bağlantı xətası. İnterneti yoxla.");
                    empty.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
          }
