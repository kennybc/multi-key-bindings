package us.kenny;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiKeyBindingClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("multi-key-bindings");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Starting multi-key-bindings!");
    }
}