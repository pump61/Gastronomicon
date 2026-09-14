package io.github.schntgaispock.gastronomicon.core.setup;

import io.github.schntgaispock.gastronomicon.core.listeners.CowInAJarListener;
import io.github.schntgaispock.gastronomicon.core.listeners.FridgeStructureListener;
import io.github.schntgaispock.gastronomicon.core.listeners.SeedListener;
import io.github.schntgaispock.gastronomicon.core.listeners.WaterRefillListener;
import io.github.schntgaispock.gastronomicon.core.listeners.WildHarvestListener;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ListenerSetup {

    public static void setup() {
        SeedListener.setup();
        WildHarvestListener.setup();
        WaterRefillListener.setup();
        FridgeStructureListener.setup();
        CowInAJarListener.setup();
    }

}
