public class ModrinthActivity extends AppCompatActivity {
    WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modrinth);

        web = findViewById(R.id.webview);
        web.getSettings().setJavaScriptEnabled(true);

        web.loadUrl("https://modrinth.com/mods");
    }
}
