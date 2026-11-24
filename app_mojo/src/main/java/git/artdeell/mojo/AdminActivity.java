
package git.artdeell.mojo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_karabakh_admin);

        Button back = findViewById(R.id.admin_back);

        back.setOnClickListener(v -> finish());

        Button update = findViewById(R.id.admin_publish_update);
        Button announcement = findViewById(R.id.admin_publish_announcement);

        update.setOnClickListener(v ->
            Toast.makeText(this,
                "Yeniləmə sistemi hələ serverə qoşulmayıb",
                Toast.LENGTH_SHORT).show()
        );

        announcement.setOnClickListener(v ->
            Toast.makeText(this,
                "Elan sistemi hələ serverə qoşulmayıb",
                Toast.LENGTH_SHORT).show()
        );
    }
}
