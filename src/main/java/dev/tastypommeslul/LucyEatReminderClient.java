package dev.tastypommeslul;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.moulberry.lattice.Lattice;
import com.moulberry.lattice.element.LatticeElements;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LucyEatReminderClient implements ClientModInitializer {
    public static LatticeElements elements;
    public static final String MOD_ID = "lucyeatreminder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Config config;

    public static final LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommands.literal("fer")
            .then(ClientCommands.literal("enabled").executes(_ -> {
                config.enabled = !config.enabled;
                saveConfig();
                return Command.SINGLE_SUCCESS;
            }).then(ClientCommands.argument("value", BoolArgumentType.bool()).executes(context -> {
                config.enabled = context.getArgument("value", Boolean.class);
                saveConfig();
                return Command.SINGLE_SUCCESS;
            })))
            .then(ClientCommands.literal("debug").executes(_ -> {
                config.debug = !config.debug;
                saveConfig();
                return Command.SINGLE_SUCCESS;
            }).then(ClientCommands.argument("value", BoolArgumentType.bool()).executes(context -> {
                config.debug = context.getArgument("value", Boolean.class);
                saveConfig();
                return Command.SINGLE_SUCCESS;
            })))
            .then(ClientCommands.literal("amount")
                    .then(ClientCommands.argument("amount", IntegerArgumentType.integer(0, 10)).executes(context -> {
                        config.amount = context.getArgument("amount", Integer.class);
                        saveConfig();
                        return Command.SINGLE_SUCCESS;
                    })))
            .then(ClientCommands.literal("config").executes(_ -> {
                Minecraft.getInstance().schedule(() -> Minecraft.getInstance().setScreen(LucyEatReminderClient.configScreen(null)));
                return Command.SINGLE_SUCCESS;
            }));
    @Override
    public void onInitializeClient() {
        config = new Config();
        loadConfig();
        try {
            elements = LatticeElements.fromAnnotations(Component.literal("Lucy Eat Reminder Config"), config);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Lattice config: {}", e.getMessage());
        }
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> dispatcher.register(command));
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Identifier.fromNamespaceAndPath(MOD_ID, "eat_texture"), (guiGraphics, _) -> {
            Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/eat_texture-128x128.png");
            Minecraft client = Minecraft.getInstance();
            assert client.player != null;
            if (!config.enabled) return;
            int baseImageSize = 24;

            int imageHeight = 0;
            int imageWidth = 0;

            if (((float) client.player.getFoodData().getFoodLevel()) / 2 <= config.amount) {
                imageHeight = getImageScale(baseImageSize, client.player.getFoodData().getFoodLevel(), 1);
                imageWidth = getImageScale((int) (baseImageSize * 1.35), client.player.getFoodData().getFoodLevel(), 1);
            }

            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();

            int x = (screenWidth / 2) - (imageWidth / 2);
            int y = (screenHeight / 2) - (imageHeight / 2);

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
            if (config.debug) {
                guiGraphics.text(client.font, "Debug Info: ", 10, 10, 0xFFFFFFFF, false);
                guiGraphics.text(client.font, "     Food Level: " + client.player.getFoodData().getFoodLevel(), 10, 25, 0xFFFFFFFF, false);
                guiGraphics.text(client.font, "     Calculated Width: " + imageWidth, 10, 40, 0xFFFFFFFF, false);
                guiGraphics.text(client.font, "     Calculated Height: " + imageHeight, 10, 55, 0xFFFFFFFF, false);
            }
        });
    }

    public static int getImageScale(int imageSize, int foodLevel, int scalingSlope) {
        if (foodLevel == 0) {
            foodLevel = 1;
        }
        return imageSize * (imageSize / (foodLevel * scalingSlope));
    }

    public static Screen configScreen(Screen parent) {
        if (elements == null) {
            System.err.println("Lattice elements not initialized!");
            return null;
        }
        return Lattice.createConfigScreen(elements, LucyEatReminderClient::saveConfig, parent);
    }

    private static final Path FILE = Paths.get("config", MOD_ID + ".json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void loadConfig() {
        try {
            if (Files.notExists(FILE)) {
                saveConfig();
                return;
            }
            try (Reader r = Files.newBufferedReader(FILE)) {
                config = GSON.fromJson(r, Config.class);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config: {}", e.getMessage());
        }
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(config, w);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }
}
