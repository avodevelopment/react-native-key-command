package com.expensify.reactnativekeycommand;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import android.view.KeyEvent;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

@ReactModule(name = KeyCommandModule.NAME)
public class KeyCommandModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext reactContext = null;
    private DeviceEventManagerModule.RCTDeviceEventEmitter mJSModule = null;
    private static KeyCommandModule instance = null;

    public static final String NAME = "KeyCommand";

    private static final Set<ReadableMap> commandsArray = new HashSet<ReadableMap>();

    public KeyCommandModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    public static KeyCommandModule init(ReactApplicationContext reactContext) {
        instance = new KeyCommandModule(reactContext);
        return instance;
    }

    public static KeyCommandModule getInstance() {
        return instance;
    }

    public void onKeyDownEvent(int keyCode, KeyEvent keyEvent) {
        if (!reactContext.hasActiveCatalystInstance()) {
            return;
        }

        if (mJSModule == null) {
            mJSModule = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        }

        WritableMap params = getJsEventParams(keyCode, keyEvent, null);

        if (matchesInput(params)) {
            mJSModule.emit("onKeyCommand", params);
        }
    };

    private boolean matchesInput(ReadableMap command) {
        /**
         * command object may have following schema:
         * - {input: 'f', modifierFlags: 123}
         * - {input: 'f'}
         * - {input: '123'}
         */
        if(commandsArray.isEmpty()){
            return true;
        }
        for (ReadableMap registeredCommand : commandsArray) {
            int registeredCommandModifierFlags = registeredCommand.hasKey("modifierFlags") ? registeredCommand.getInt("modifierFlags") : 0;
            int commandModifierFlags = command.hasKey("modifierFlags") ? command.getInt("modifierFlags") : 0;
            
            boolean matchInput = registeredCommand.getString("input").equals(command.getString("input"));
            boolean matchModifierFlags = registeredCommandModifierFlags == commandModifierFlags;

            if (matchInput && matchModifierFlags) {
                return true;
            }
        }

        return false;
    }

    private WritableMap getJsEventParams(int keyCode, KeyEvent keyEvent, Integer repeatCount) {
        WritableMap params = new WritableNativeMap();

        /**
         * Keyboard mostly has multiple modifier keys (e.g. CTRL Left and Right)
         * using isPressed method makes comparision by modifier mask
         */
        int modifierFlags = 0;
        if (keyEvent.isCtrlPressed() && keyEvent.isShiftPressed()) {
            modifierFlags = KeyEvent.META_CTRL_MASK | KeyEvent.META_SHIFT_MASK;
        }
        else if (keyEvent.isCtrlPressed()) {
            modifierFlags = KeyEvent.META_CTRL_MASK;
        }
        else if (keyEvent.isAltPressed()) {
            modifierFlags = KeyEvent.META_ALT_MASK;
        }
        else if (keyEvent.isShiftPressed()) {
            modifierFlags = KeyEvent.META_SHIFT_MASK;
        }

        int inputKeyCode = 0;
        if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
            inputKeyCode = KeyEvent.KEYCODE_ESCAPE;
        }
        else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
            inputKeyCode = KeyEvent.KEYCODE_DPAD_UP;
        }
        else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
            inputKeyCode = KeyEvent.KEYCODE_DPAD_DOWN;
        }
        else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
            inputKeyCode = KeyEvent.KEYCODE_DPAD_LEFT;
        }
        else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
            inputKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
        }
        else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            inputKeyCode = KeyEvent.KEYCODE_ENTER;
        }

        /**
         * Handle an event where an input (e.g. ESC) is pressed that has no display label
         */
        String input = String.valueOf(inputKeyCode);
        String displayLabel = String.valueOf(keyEvent.getDisplayLabel())
            .replaceAll("[^A-Za-z0-9]", "")
            .toLowerCase();
        if (!displayLabel.isEmpty()) {
            input = displayLabel;
        }
        if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.isCtrlPressed()) {
            input = Integer.toString(KeyEvent.KEYCODE_ENTER);
            modifierFlags = KeyEvent.META_CTRL_MASK;
        }

        params.putInt("keyCode",keyCode);
        params.putInt("modifierFlags", modifierFlags);
        params.putString("input", input);

        return params;
    }


    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        constants.put("keyModifierShift", KeyEvent.META_SHIFT_MASK);
        constants.put("keyModifierControl", KeyEvent.META_CTRL_MASK);
        constants.put("keyModifierShiftCommand", KeyEvent.META_CTRL_MASK | KeyEvent.META_SHIFT_MASK);
        constants.put("keyModifierShiftControl", KeyEvent.META_CTRL_MASK | KeyEvent.META_SHIFT_MASK);
        constants.put("keyModifierAlternate", KeyEvent.META_ALT_MASK);
        constants.put("keyModifierCommand", KeyEvent.META_CTRL_MASK);
        constants.put("keyModifierNumericPad", KeyEvent.KEYCODE_NUM);
        constants.put("keyInputUpArrow", KeyEvent.KEYCODE_DPAD_UP);
        constants.put("keyInputDownArrow", KeyEvent.KEYCODE_DPAD_DOWN);
        constants.put("keyInputLeftArrow", KeyEvent.KEYCODE_DPAD_LEFT);
        constants.put("keyInputRightArrow", KeyEvent.KEYCODE_DPAD_RIGHT);
        constants.put("keyInputEscape", KeyEvent.KEYCODE_ESCAPE);
        constants.put("keyInputEnter", KeyEvent.KEYCODE_ENTER);

        return constants;
    }

    @ReactMethod
    public void registerKeyCommands(ReadableArray json, Promise promise) {
        for (int i = 0; i < json.size(); i++) {
            commandsArray.add(json.getMap(i));
        }

        promise.resolve(null);
    }

    @ReactMethod
    public void unregisterKeyCommands(ReadableArray json, Promise promise) {
        for (int i = 0; i < json.size(); i++) {
            commandsArray.remove(json.getMap(i));
        }

        promise.resolve(null);
    }

    @ReactMethod
    public void addListener(String eventName) {}

    @ReactMethod
    public void removeListeners(Integer count) {}
}
