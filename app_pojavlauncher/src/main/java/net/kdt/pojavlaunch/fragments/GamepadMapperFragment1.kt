package net.kdt.pojavlaunch.fragments

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnGenericMotionListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.spse.gamepad_remapper.RemapperManager
import fr.spse.gamepad_remapper.RemapperView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.customcontrols.gamepad.Gamepad
import net.kdt.pojavlaunch.customcontrols.gamepad.GamepadMapperAdapter

class GamepadMapperFragment : Fragment(R.layout.fragment_controller_remapper), View.OnKeyListener,
    OnGenericMotionListener, AdapterView.OnItemSelectedListener {
    private val mRemapperViewBuilder: RemapperView.Builder = RemapperView.Builder(null)
        .remapA(true)
        .remapB(true)
        .remapX(true)
        .remapY(true)
        .remapLeftJoystick(true)
        .remapRightJoystick(true)
        .remapStart(true)
        .remapSelect(true)
        .remapLeftShoulder(true)
        .remapRightShoulder(true)
        .remapLeftTrigger(true)
        .remapRightTrigger(true)
        .remapDpad(true)
    private val mExitHandler = Handler(Looper.getMainLooper())
    private val mExitRunnable = Runnable {
        val activity: Activity? = activity
        if (activity == null) return@Runnable
        activity.onBackPressed()
    }
    private var mInputManager: RemapperManager? = null
    private var mMapperAdapter: GamepadMapperAdapter? = null
    private var mGamepad: Gamepad? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val buttonRecyclerView = view.findViewById<RecyclerView>(R.id.gamepad_remapper_recycler)
        mMapperAdapter = GamepadMapperAdapter(requireContext())
        buttonRecyclerView.layoutManager = LinearLayoutManager(view.context)
        buttonRecyclerView.adapter = mMapperAdapter
        buttonRecyclerView.setOnKeyListener(this)
        buttonRecyclerView.setOnGenericMotionListener(this)
        buttonRecyclerView.requestFocus()
        mInputManager = RemapperManager(view.context, mRemapperViewBuilder)
        val grabStateSpinner = view.findViewById<Spinner>(R.id.gamepad_remapper_mode_spinner)
        val mGrabStateAdapter =
            ArrayAdapter<String>(view.context, R.layout.support_simple_spinner_dropdown_item)
        mGrabStateAdapter.addAll(
            getString(R.string.customctrl_visibility_in_menus),
            getString(R.string.customctrl_visibility_ingame)
        )
        grabStateSpinner.adapter = mGrabStateAdapter
        grabStateSpinner.setSelection(0)
        grabStateSpinner.onItemSelectedListener = this
    }

    private fun createGamepad(mainView: View, inputDevice: InputDevice?) {
        val adapter = mMapperAdapter ?: return
        mGamepad = object : Gamepad(mainView, inputDevice, adapter, false) {
            override fun handleGamepadInput(keycode: Int, value: Float) {
                if (keycode == KeyEvent.KEYCODE_BUTTON_SELECT) {
                    handleExitButton(value > 0.5)
                }
                super.handleGamepadInput(keycode, value)
            }
        }
    }

    private fun handleExitButton(isPressed: Boolean) {
        if (isPressed) mExitHandler.postDelayed(mExitRunnable, 400)
        else mExitHandler.removeCallbacks(mExitRunnable)
    }

    override fun onKey(view: View?, i: Int, keyEvent: KeyEvent): Boolean {
        val mainView = view ?: return false
        if (!Gamepad.isGamepadEvent(keyEvent)) return false
        if (mGamepad == null) createGamepad(mainView, keyEvent.device)
        mInputManager?.handleKeyEventInput(mainView.context, keyEvent, mGamepad)
        return true
    }

    override fun onGenericMotion(view: View?, motionEvent: MotionEvent): Boolean {
        val mainView = view ?: return false
        if (!Gamepad.isGamepadEvent(motionEvent)) return false
        if (mGamepad == null) createGamepad(mainView, motionEvent.device)
        mInputManager?.handleMotionEventInput(mainView.context, motionEvent, mGamepad)
        return true
    }

    override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        val grab = i == 1
        mMapperAdapter?.setGrabState(grab)
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {
    }

    companion object {
        const val TAG: String = "GamepadMapperFragment"
    }
}
