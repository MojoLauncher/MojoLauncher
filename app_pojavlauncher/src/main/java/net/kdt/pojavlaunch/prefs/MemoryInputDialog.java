package net.kdt.pojavlaunch.prefs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import git.artdeell.mojo.R;

public class MemoryInputDialog {
    public interface Callback {
        void setMemory(int value);
    }
    private final AlertDialog mDialog;
    private final View mInputView;
    private final EditText mEditField;
    public MemoryInputDialog(Context context, Callback onMemorySet){
        mInputView = LayoutInflater.from(context).inflate(R.layout.dialog_mem_input, null, false);
        mEditField = mInputView.findViewById(R.id.mem_input_field);
        mDialog = new AlertDialog.Builder(context)
                .setView(mInputView)
                .setTitle(R.string.dialog_mem_alloc_title)
                .setMessage(R.string.dialog_mem_alloc_message)
                .setPositiveButton(android.R.string.ok, (l,p ) -> onMemorySet.setMemory(this.getMemory()))
                .setOnDismissListener(dialog -> {})
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {})
                .create();
    }
    public void show(){
        mDialog.show();
    }
    private int getMemory(){
        String text = mEditField.getText().toString();
        mEditField.setText(null);
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            value = 0;
        }
        return value;
    }
}
