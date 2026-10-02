package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.animation.Animator;
import android.animation.AnimatorInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import git.artdeell.mojo.R;

/**
 * Cryonix AI Assist — an offline chatbot that guides users through
 * setting up Cryonix Launcher V2 (Java, profiles, controls, login,
 * mods and troubleshooting). No network access is used; answers come
 * from a built-in keyword knowledge base.
 */
public class AiAssistFragment extends Fragment {

    public static final String TAG = "AiAssistFragment";
    public static final String ARG_ERROR_TEXT = "error_text";

    private final List<AiMessage> messages = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ChatAdapter adapter;
    private EditText input;
    private RecyclerView messageList;
    private int typingPosition = -1;

    public AiAssistFragment() {
        super(R.layout.fragment_ai_assist);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        messageList = view.findViewById(R.id.ai_message_list);

        ImageButton backButton = view.findViewById(R.id.ai_assist_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                requireActivity().onBackPressed();
            });
        }

        RecyclerView list = view.findViewById(R.id.ai_message_list);
        LinearLayoutManager layoutManager = list != null ? (LinearLayoutManager) list.getLayoutManager() : null;
        if (list != null) {
            if (layoutManager == null) {
                layoutManager = new LinearLayoutManager(requireContext());
                layoutManager.setStackFromEnd(true);
                list.setLayoutManager(layoutManager);
            }
            adapter = new ChatAdapter();
            list.setAdapter(adapter);
        }
        this.input = view.findViewById(R.id.ai_input);

        View analyzeLogButton = view.findViewById(R.id.ai_analyze_log_button);
        if (analyzeLogButton != null) {
            analyzeLogButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                analyzeLatestLog();
            });
        }

        View clearButton = view.findViewById(R.id.ai_clear_button);
        if (clearButton != null) {
            clearButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                messages.clear();
                if (adapter != null) adapter.notifyDataSetChanged();
                addBotMessage("Ready. Paste a crash, error, or latest launcher log and I’ll break it into likely cause + exact next steps.");
            });
        }

        buildChips(view);

        ImageButton sendButton = view.findViewById(R.id.ai_send_button);
        if (sendButton != null) {
            sendButton.setOnClickListener(v -> {
                Tools.jellyClick(v);
                sendUserMessage();
            });
        }
        if (input != null) {
            input.setOnEditorActionListener((t, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                    sendUserMessage();
                    return true;
                }
                return false;
            });
        }

        String incomingError = getArguments() != null
                ? getArguments().getString(ARG_ERROR_TEXT) : null;
        if (incomingError != null && !incomingError.trim().isEmpty()) {
            if (input != null) input.setText(incomingError);
            addBotMessage(CryonixDiagnosticEngine.analyze(incomingError));
        } else {
            addBotMessage(CryonixBrain.greeting());
        }
    }

    private void buildChips(View view) {
        LinearLayout container = view.findViewById(R.id.ai_chip_container);
        if (container == null) {
            return;
        }
        for (String chip : CryonixBrain.chipTopics()) {
            final String query = chip;
            TextView tv = new TextView(requireContext());
            tv.setText(chip);
            tv.setBackgroundResource(R.drawable.cryonix_chat_chip);
            tv.setTextColor(0xFFA9C0EA);
            tv.setTextSize(13f);
            tv.setPadding(dp(14), dp(8), dp(14), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            tv.setLayoutParams(lp);
            tv.setOnClickListener(v -> {
                Tools.jellyClick(v);
                if ("Analyze latest log".equals(query)) {
                    analyzeLatestLog();
                } else {
                    input.setText(query);
                    sendUserMessage();
                }
            });
            container.addView(tv);
        }
    }

    private void sendUserMessage() {
        if (input == null) {
            return;
        }
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        input.setText("");
        addUserMessage(text);
        respond(text);
    }

    private void addUserMessage(String text) {
        int pos = messages.size();
        messages.add(new AiMessage(true, text));
        if (adapter != null) {
            adapter.notifyItemInserted(pos);
            scrollToEnd();
            animateRow(pos);
        }
    }

    private void addBotMessage(String text) {
        int pos = messages.size();
        messages.add(new AiMessage(false, text));
        if (adapter != null) {
            adapter.notifyItemInserted(pos);
            scrollToEnd();
            animateRow(pos);
        }
    }

    /** Bounce a newly added message bubble in with the jelly pop animation. */
    private void animateRow(int index) {
        final RecyclerView list = messageList;
        if (list == null) {
            return;
        }
        handler.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }
            View row = list.getLayoutManager() != null ? list.getLayoutManager().findViewByPosition(index) : null;
            if (row != null) {
                try {
                    Animator animator = AnimatorInflater.loadAnimator(row.getContext(), R.animator.cryonix_bubble_in);
                    animator.setTarget(row);
                    animator.start();
                } catch (Throwable ignored) {
                }
            }
        }, 40);
    }

    private void analyzeLatestLog() {
        addUserMessage("Analyze my latest launcher log");
        addBotMessage("Reading latestlog.txt…");
        final int statusIndex = messages.size() - 1;

        new Thread(() -> {
            String log = readLatestLog();
            String answer = CryonixDiagnosticEngine.analyze(log);
            handler.post(() -> {
                if (!isAdded()) return;
                if (statusIndex >= 0 && statusIndex < messages.size()) {
                    messages.set(statusIndex, new AiMessage(false, answer));
                    if (adapter != null) {
                        adapter.notifyItemChanged(statusIndex);
                        scrollToEnd();
                    }
                }
            });
        }, "CryonixLogAnalyzer").start();
    }

    private String readLatestLog() {
        try {
            File crash = new File(Tools.DIR_GAME_HOME, "latestcrash.txt");
            File log = new File(Tools.DIR_GAME_HOME, "latestlog.txt");
            File privateCrash = Tools.DIR_DATA != null
                    ? new File(Tools.DIR_DATA, "latestcrash.txt") : null;
            File file = crash.exists() && crash.isFile() ? crash
                    : privateCrash != null && privateCrash.isFile() ? privateCrash : log;
            if (!file.exists() || !file.isFile()) {
                return "No latestcrash.txt or latestlog.txt was found. Paste the crash text or log into the message box instead.";
            }

            long maxBytes = 512L * 1024L;
            try (FileInputStream in = new FileInputStream(file);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int read;
                while ((read = in.read(buffer)) != -1 && total < maxBytes) {
                    int allowed = (int) Math.min(read, maxBytes - total);
                    out.write(buffer, 0, allowed);
                    total += allowed;
                    if (allowed < read) break;
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (Throwable e) {
            return "I couldn't read latestlog.txt: " + e.getClass().getSimpleName()
                    + " — " + String.valueOf(e.getMessage());
        }
    }

    private boolean looksLikeCrash(String text) {
        String q = text.toLowerCase();
        return q.contains("exception") || q.contains("error:")
                || q.contains("caused by:") || q.contains("stacktrace")
                || q.contains("fatal") || q.contains("unsatisfiedlinkerror")
                || q.contains("crash-report") || q.contains("exit code");
    }

    private void respond(String userText) {
        // "Typing…" bubble that is replaced by the real answer shortly after,
        // so the bot feels alive.
        typingPosition = messages.size();
        messages.add(new AiMessage(false, "…"));
        if (adapter != null) {
            adapter.notifyItemInserted(typingPosition);
            scrollToEnd();
        }
        final int index = typingPosition;
        final String answer = looksLikeCrash(userText) ? CryonixDiagnosticEngine.analyze(userText) : CryonixBrain.respond(userText);
        long delay = 450 + Math.min(450, answer.length() / 4L);
        handler.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }
            if (index >= 0 && index < messages.size()) {
                messages.set(index, new AiMessage(false, answer));
                if (adapter != null) {
                    adapter.notifyItemChanged(index);
                    scrollToEnd();
                }
            }
        }, delay);
    }

    private void scrollToEnd() {
        if (adapter != null && messages.size() > 0) {
            RecyclerView list = getView() != null ? getView().findViewById(R.id.ai_message_list) : null;
            if (list != null) {
                list.smoothScrollToPosition(messages.size() - 1);
            }
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------------
    // Model + adapter
    // ------------------------------------------------------------------

    private static class AiMessage {
        final boolean fromUser;
        String text;

        AiMessage(boolean fromUser, String text) {
            this.fromUser = fromUser;
            this.text = text;
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = View.inflate(parent.getContext(), R.layout.item_ai_message, null);
            return new VH(v);
        }

        @NonNull
        public VH onCreateViewHolder(@NonNull RecyclerView parent, int viewType) {
            // Compatibility overload for newer RecyclerView (1.2+) versions.
            return onCreateViewHolder((ViewGroup) parent, viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            AiMessage message = messages.get(position);
            TextView bot = holder.itemView.findViewById(R.id.message_bot);
            TextView user = holder.itemView.findViewById(R.id.message_user);
            if (message.fromUser) {
                user.setVisibility(View.VISIBLE);
                user.setText(message.text);
                bot.setVisibility(View.GONE);
            } else {
                bot.setVisibility(View.VISIBLE);
                bot.setText(message.text);
                user.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class VH extends RecyclerView.ViewHolder {
            VH(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

    // ------------------------------------------------------------------
    // Knowledge base — Cryonix Launcher V2 setup guide (offline)
    // ------------------------------------------------------------------

    private static class CryonixBrain {

        static String greeting() {
            return "Hi! I'm the Cryonix AI Assist bot 🤖\n\n"
                    + "I can walk you through setting up Cryonix Launcher V2, step by step — "
                    + "Java runtime, profiles, controls, login, mods and troubleshooting.\n\n"
                    + "Tap a topic below or type your question. "
                    + "To see the complete setup guide, just ask: “setup”";
        }

        static String[] chipTopics() {
            return new String[]{
                    "Setup guide",
                    "Install Java",
                    "Login / Account",
                    "Controls",
                    "Mods & Modpacks",
                    "Troubleshooting",
                    "Analyze latest log"
            };
        }

        static String respond(String raw) {
            String q = raw.toLowerCase();

            if (q.contains("setup") || q.contains("start") || q.contains("begin") || q.contains("guide")
                    || q.contains("first") || q.contains("how do i") || q.contains("help me")) {
                return setupGuide();
            }
            if (q.contains("java") || q.contains("jdk") || q.contains("jre") || q.contains("runtime")
                    || q.contains("openjdk") || q.contains("crash on launch") && q.contains("java")) {
                return javaGuide();
            }
            if (q.contains("login") || q.contains("log in") || q.contains("account") || q.contains("sign")
                    || q.contains("microsoft") || q.contains("ms ") || q.contains("elyby") || q.contains("offline")) {
                return loginGuide();
            }
            if (q.contains("control") || q.contains("touch") || q.contains("button") || q.contains("gamepad")
                    || q.contains("remap") || q.contains("keyboard") || q.contains("sensitivity")) {
                return controlsGuide();
            }
            if (q.contains("mod") || q.contains("modpack") || q.contains("fabric") || q.contains("forge")
                    || q.contains("neoforge") || q.contains("quilt") || q.contains("curseforge") || q.contains("multiMC")) {
                return modsGuide();
            }
            if (q.contains("troubleshoot") || q.contains("problem") || q.contains("issue") || q.contains("fix")
                    || q.contains("not working") || q.contains("black screen") || q.contains("stuck")
                    || q.contains("download fail") || q.contains("corrupt") || q.contains("permission")) {
                return troubleshooting();
            }
            if (q.contains("profile") || q.contains("instance") || q.contains("create") || q.contains("add")) {
                return profileGuide();
            }
            if (q.contains("hello") || q.contains("hi") || q.contains("hey")) {
                return "Hello! 👋 Ready to help you set up Cryonix Launcher V2. "
                        + "Ask me “setup guide” for the full walkthrough, or tap a topic chip below.";
            }
            if (q.contains("thank")) {
                return "You're welcome! 💙 If anything breaks along the way, come back and ask — "
                        + "try “troubleshooting” for the quick fixes list.";
            }
            return fallback();
        }

        static String setupGuide() {
            return "Here's how to set up Cryonix Launcher V2 properly ✅\n\n"
                    + "1️⃣ Storage permission — if Android asks for storage (Android 13+: “All files access”), allow it. "
                    + "The launcher needs it to save game data.\n\n"
                    + "2️⃣ Java runtime — tap the Java icon in the left sidebar and download an OpenJDK runtime "
                    + "(8 or 17). Do this before your first launch — Minecraft cannot start without it.\n\n"
                    + "3️⃣ Account — tap the profile icon (top right) and choose how to log in: Microsoft, Elyby, "
                    + "or Local (offline) account.\n\n"
                    + "4️⃣ Create your first instance — on the home screen press “+ Add Instance”, choose Vanilla or "
                    + "Modded, pick a loader (Fabric / Forge / NeoForge / Quilt for modded), choose a game version, "
                    + "and wait for the download.\n\n"
                    + "5️⃣ Play — select the instance, tap the green ▶ launch button, and enjoy!\n\n"
                    + "6️⃣ Touch controls — if the on-screen buttons feel off, open the Controls screen from the "
                    + "sidebar and adjust positions, sizes and buttons.\n\n"
                    + "Ask me about any of these steps in more detail! 🚀";
        }

        static String javaGuide() {
            return "Installing the Java runtime ☕\n\n"
                    + "1. Go to the home screen and tap the Java icon in the left sidebar.\n"
                    + "2. Pick a runtime — OpenJDK 8 works for older Minecraft versions, OpenJDK 17 for 1.18+.\n"
                    + "3. Wait for the download and install to finish (check your internet first).\n"
                    + "4. Done — the launcher now uses it automatically for every instance.\n\n"
                    + "⚠️ If the game crashes on launch with a Java error, re-download the runtime from the same "
                    + "screen, then try launching again.";
        }

        static String profileGuide() {
            return "Creating an instance (profile) 🎮\n\n"
                    + "1. Home screen → “+ Add Instance”.\n"
                    + "2. Choose Vanilla (plain Minecraft) or Modded.\n"
                    + "3. If modded, pick your loader: Fabric, Forge, NeoForge or Quilt.\n"
                    + "4. Pick the game version and let the files download.\n\n"
                    + "Tip: each instance has its own world and mods — create a separate one for every "
                    + "modpack you try. Use the Edit screen to rename, copy or delete instances.";
        }

        static String controlsGuide() {
            return "Touch controls 🕹️\n\n"
                    + "1. Open the Controls screen (gamepad icon in the sidebar).\n"
                    + "2. Drag buttons where you like, pinch them to resize, and re-map actions to buttons.\n"
                    + "3. Save when you're happy — the layout is remembered for your device.\n\n"
                    + "If you connect a real controller, the launcher can remap its buttons from the same screen. "
                    + "For laggy touch response, close background apps and try a lower render distance in game.";
        }

        static String loginGuide() {
            return "Logging in 👤\n\n"
                    + "Tap the profile icon (top right) and pick one:\n\n"
                    + "• Microsoft — the official Mojang account. You need to be signed in on a PC once with a "
                    + "free Microsoft account.\n"
                    + "• Elyby — the Elytra-based helper for devices where Microsoft login is blocked. "
                    + "Follow the on-screen steps.\n"
                    + "• Local / Offline — no internet account needed; you play with a local skin name. "
                    + "Great for single-player.\n\n"
                    + "After the first login the account is saved — you can switch or remove it from the same screen.";
        }

        static String modsGuide() {
            return "Mods & Modpacks 📦\n\n"
                    + "Installing individual mods:\n"
                    + "1. Create a Modded instance (Fabric is easiest to start with).\n"
                    + "2. Open the file selector from the instance screen and copy .jar mods into the "
                    + "instance's mods folder.\n\n"
                    + "Installing a modpack:\n"
                    + "1. Download the modpack's .zip/.mrpack/.mmc from CurseForge / MultiMC on your PC.\n"
                    + "2. Copy it to the launcher's storage and import it in the launcher — it sets up the "
                    + "right loader and version for you.\n\n"
                    + "⚠️ Always match mod versions to the game version (Fabric API for Fabric, "
                    + "Forge version for Forge), or things will crash.";
        }

        static String troubleshooting() {
            return "Quick fixes 🛠️\n\n"
                    + "• Game doesn't start / crashes on launch → re-download the Java runtime (sidebar → Java).\n"
                    + "• Download stuck or failing → check Wi-Fi, wait, retry; big files take time on mobile.\n"
                    + "• Black screen after launch → try a different resolution / turn down render distance; "
                    + "some devices need an older game version.\n"
                    + "• “Permission denied” → grant storage (Android 13+: “All files access”) and restart.\n"
                    + "• Login fails → Microsoft login needs the account signed in on a PC; otherwise use Elyby "
                    + "or a Local account.\n"
                    + "• Controls feel laggy → close background apps, enable performance mode in Android settings.\n\n"
                    + "Still stuck? Tell me exactly what you see (error text / where it happens) and I'll point "
                    + "you the right way.";
        }

        static String fallback() {
            return "I'm a setup assistant, so I know these topics best: 📋\n\n"
                    + "• “setup guide” — the full step-by-step walkthrough\n"
                    + "• “install java” — get the Java runtime working\n"
                    + "• “create a profile” — make your first instance\n"
                    + "• “login” — Microsoft, Elyby or offline account\n"
                    + "• “controls” — touch controls and remapping\n"
                    + "• “mods & modpacks” — install Fabric / Forge packs\n"
                    + "• “troubleshooting” — quick fixes for common problems\n\n"
                    + "Tap a chip below or type one of these words!";
        }
    }
}
