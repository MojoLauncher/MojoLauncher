package net.kdt.pojavlaunch.customcontrols;

import java.util.Iterator;
import java.util.List;

public class LayoutSanitizer {

    // Maybe add more conditions here later?
    private static boolean isValidFormula(String formula) {
        return !formula.contains("Infinity") && !formula.contains("NaN");
    }

    private static boolean isSaneData(ControlData controlData) {
        if(controlData.getWidth() == 0 || controlData.getHeight() == 0) return false;
        return isValidFormula(controlData.dynamicX) && isValidFormula(controlData.dynamicY);
    }

    private static boolean checkEntry(Object entry) {
        if(entry instanceof ControlData) {
            return isSaneData((ControlData) entry);
        }else if(entry instanceof ControlDrawerData) {
            ControlDrawerData drawerData = (ControlDrawerData) entry;
            if(!isSaneData(drawerData.properties)) return false;
            sanitizeList(drawerData.buttonProperties);
            return true;
        }else throw new RuntimeException("Unknown data entry "+entry.getClass().getName());
    }

    private static boolean sanitizeList(List<?> controlDataList) {
        boolean madeChanges = false;
        Iterator<?> iterator = controlDataList.iterator();
        while(iterator.hasNext()) {
            if(!checkEntry(iterator.next())) {
                madeChanges = true;
                iterator.remove();
            }
        }
        return madeChanges;
    }

    /**
     * Check all buttons in a control layout and ensure they're sane (contain values valid enough
     * to be displayed properly). Removes any buttons deemed not sane.
     * @param controls the original control layout.
     * @return whether the sanitization process made any changes to the layout
     */
    public static boolean sanitizeLayout(CustomControls controls) {
        boolean madeChanges = sanitizeList(controls.mControlDataList);
        if(sanitizeList(controls.mDrawerDataList)) madeChanges = true;
        if(sanitizeList(controls.mJoystickDataList)) madeChanges = true;
        return madeChanges;
    }
}
