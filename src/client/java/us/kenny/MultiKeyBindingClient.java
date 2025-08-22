package us.kenny;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.kenny.mixin.KeyBindingAccessor;

import java.util.*;

public class MultiKeyBindingClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("multi-key-bindings");

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfigFile();
    }
}