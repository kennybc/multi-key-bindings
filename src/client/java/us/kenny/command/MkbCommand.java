package us.kenny.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import us.kenny.ProfileManager;
import us.kenny.core.profile.ProfileNameValidator;

import java.util.concurrent.CompletableFuture;

/**
 * Chat formatting palette used by all /mkb feedback for a consistent look.
 * kept as constants near the top so tweaks are one-stop.
 */

/**
 * Client-side /mkb subcommands for profile CRUD and switching. All
 * feedback goes to the local chat via sendFeedback / sendError.
 */
public final class MkbCommand {
    private static final ChatFormatting HEADER = ChatFormatting.WHITE;
    private static final ChatFormatting HIGHLIGHT = ChatFormatting.AQUA;
    private static final ChatFormatting HINT = ChatFormatting.GRAY;

    private MkbCommand() {
    }

    /**
     * Style a profile name so it stands out inside a translated message.
     */
    private static Component nameArg(String name) {
        return Component.literal(name).withStyle(HIGHLIGHT);
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
                ClientCommands.literal("mkb")
                        .executes(c -> status(c.getSource()))
                        .then(ClientCommands.literal("help")
                                .executes(c -> help(c.getSource())))
                        .then(ClientCommands.literal("list")
                                .executes(c -> listProfiles(c.getSource())))
                        .then(ClientCommands.literal("create")
                                .then(ClientCommands.argument("name", StringArgumentType.word())
                                        .executes(c -> create(c.getSource(),
                                                StringArgumentType.getString(c, "name")))))
                        .then(ClientCommands.literal("load")
                                .then(ClientCommands.argument("name", StringArgumentType.word())
                                        .suggests(MkbCommand::suggestProfiles)
                                        .executes(c -> load(c.getSource(),
                                                StringArgumentType.getString(c, "name")))))
                        .then(ClientCommands.literal("rename")
                                .then(ClientCommands.argument("old", StringArgumentType.word())
                                        .suggests(MkbCommand::suggestProfiles)
                                        .then(ClientCommands.argument("new", StringArgumentType.word())
                                                .executes(c -> rename(c.getSource(),
                                                        StringArgumentType.getString(c, "old"),
                                                        StringArgumentType.getString(c, "new"))))))
                        .then(ClientCommands.literal("duplicate")
                                .then(ClientCommands.argument("name", StringArgumentType.word())
                                        .suggests(MkbCommand::suggestProfiles)
                                        .executes(c -> duplicate(c.getSource(),
                                                StringArgumentType.getString(c, "name"), null))
                                        .then(ClientCommands.argument("new_name", StringArgumentType.word())
                                                .executes(c -> duplicate(c.getSource(),
                                                        StringArgumentType.getString(c, "name"),
                                                        StringArgumentType.getString(c, "new_name"))))))
                        .then(ClientCommands.literal("delete")
                                .then(ClientCommands.argument("name", StringArgumentType.word())
                                        .suggests(MkbCommand::suggestProfiles)
                                        .executes(c -> delete(c.getSource(),
                                                StringArgumentType.getString(c, "name")))))));
    }

    private static CompletableFuture<Suggestions> suggestProfiles(
            CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ProfileManager.listProfileNames(), builder);
    }

    private static int status(FabricClientCommandSource src) {
        String version = FabricLoader.getInstance().getModContainer("multi-key-bindings")
                .map(mc -> mc.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
        src.sendFeedback(Component.translatable("multi.profile.command.status.title")
                .copy().withStyle(HEADER, ChatFormatting.BOLD, ChatFormatting.UNDERLINE));
        src.sendFeedback(Component.translatable("multi.profile.command.status.version",
                Component.literal(version).withStyle(ChatFormatting.GOLD)));
        src.sendFeedback(Component.translatable("multi.profile.command.status.active",
                nameArg(ProfileManager.getActiveProfileName())));
        src.sendFeedback(Component.translatable("multi.profile.command.status.hint")
                .copy().withStyle(HINT, ChatFormatting.ITALIC));
        return 1;
    }

    private static int help(FabricClientCommandSource src) {
        src.sendFeedback(Component.translatable("multi.profile.command.help.header")
                .copy().withStyle(HEADER, ChatFormatting.BOLD, ChatFormatting.UNDERLINE));
        helpEntry(src, "/mkb", "multi.profile.command.help.status");
        helpEntry(src, "/mkb help", "multi.profile.command.help.help");
        helpEntry(src, "/mkb list", "multi.profile.command.help.list");
        helpEntry(src, "/mkb create <name>", "multi.profile.command.help.create");
        helpEntry(src, "/mkb load <name>", "multi.profile.command.help.load");
        helpEntry(src, "/mkb rename <old> <new>", "multi.profile.command.help.rename");
        helpEntry(src, "/mkb duplicate <name> [new_name]", "multi.profile.command.help.duplicate");
        helpEntry(src, "/mkb delete <name>", "multi.profile.command.help.delete");
        return 1;
    }

    private static void helpEntry(FabricClientCommandSource src, String syntax, String descKey) {
        src.sendFeedback(styleSyntax(syntax));
        src.sendFeedback(Component.literal("  ")
                .append(Component.translatable(descKey).withStyle(HINT, ChatFormatting.ITALIC)));
    }

    /**
     * Split a syntax string into a styled Component: literal parts white,
     * argument tokens (bounded by <> or []) in gold.
     */
    private static Component styleSyntax(String syntax) {
        var line = Component.literal("").withStyle(ChatFormatting.WHITE);
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < syntax.length()) {
            char c = syntax.charAt(i);
            if (c == '<' || c == '[') {
                if (buf.length() > 0) {
                    line.append(Component.literal(buf.toString()));
                    buf.setLength(0);
                }
                char close = c == '<' ? '>' : ']';
                int end = syntax.indexOf(close, i);
                if (end < 0) {
                    end = syntax.length() - 1;
                }
                line.append(Component.literal(syntax.substring(i, end + 1))
                        .withStyle(ChatFormatting.GOLD));
                i = end + 1;
            } else {
                buf.append(c);
                i++;
            }
        }
        if (buf.length() > 0) {
            line.append(Component.literal(buf.toString()));
        }
        return line;
    }

    private static int listProfiles(FabricClientCommandSource src) {
        src.sendFeedback(Component.translatable("multi.profile.command.list.header")
                .copy().withStyle(HEADER, ChatFormatting.BOLD, ChatFormatting.UNDERLINE));
        String active = ProfileManager.getActiveProfileName();
        for (String name : ProfileManager.listProfileNames()) {
            boolean isActive = name.equalsIgnoreCase(active);
            var line = Component.literal(isActive ? "* " + name : "  " + name)
                    .withStyle(isActive ? HIGHLIGHT : HINT);
            src.sendFeedback(line);
        }
        return 1;
    }

    private static int create(FabricClientCommandSource src, String name) {
        if (!ProfileNameValidator.isValid(name)) {
            src.sendError(Component.translatable("multi.profile.command.error.invalid_name"));
            return 0;
        }
        if (!ProfileManager.create(name)) {
            src.sendError(Component.translatable("multi.profile.command.error.exists", nameArg(name)));
            return 0;
        }
        src.sendFeedback(Component.translatable("multi.profile.command.created", nameArg(name))
                );
        return 1;
    }

    private static int load(FabricClientCommandSource src, String name) {
        if (!ProfileManager.load(name)) {
            src.sendError(Component.translatable("multi.profile.command.error.not_found", nameArg(name)));
            return 0;
        }
        src.sendFeedback(Component.translatable("multi.profile.command.loaded", nameArg(name))
                );
        return 1;
    }

    private static int rename(FabricClientCommandSource src, String oldName, String newName) {
        if (!ProfileNameValidator.isValid(newName)) {
            src.sendError(Component.translatable("multi.profile.command.error.invalid_name"));
            return 0;
        }
        if (!ProfileManager.rename(oldName, newName)) {
            src.sendError(Component.translatable("multi.profile.command.error.not_found", nameArg(oldName)));
            return 0;
        }
        src.sendFeedback(Component.translatable("multi.profile.command.renamed",
                nameArg(oldName), nameArg(newName)));
        return 1;
    }

    private static int duplicate(FabricClientCommandSource src, String name, String newName) {
        if (!ProfileManager.duplicate(name, newName)) {
            src.sendError(Component.translatable("multi.profile.command.error.not_found", nameArg(name)));
            return 0;
        }
        String resolved = newName == null ? name + "_copy" : newName;
        src.sendFeedback(Component.translatable("multi.profile.command.duplicated",
                nameArg(name), nameArg(resolved)));
        return 1;
    }

    private static int delete(FabricClientCommandSource src, String name) {
        if (name.equalsIgnoreCase(ProfileManager.getActiveProfileName())) {
            src.sendError(Component.translatable("multi.profile.command.error.delete_active"));
            return 0;
        }
        if (!ProfileManager.delete(name)) {
            src.sendError(Component.translatable("multi.profile.command.error.not_found", nameArg(name)));
            return 0;
        }
        src.sendFeedback(Component.translatable("multi.profile.command.deleted", nameArg(name))
                );
        return 1;
    }
}
