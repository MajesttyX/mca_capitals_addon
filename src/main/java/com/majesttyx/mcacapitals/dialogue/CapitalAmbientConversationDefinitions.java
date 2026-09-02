package com.majesttyx.mcacapitals.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.majesttyx.mcacapitals.MCACapitals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class CapitalAmbientConversationDefinitions {

    enum Context {
        EVENING("evening");

        private final String path;

        Context(String path) {
            this.path = path;
        }

        String path() {
            return path;
        }
    }

    enum Reference {
        SPEAKER,
        SOVEREIGN,
        CONSORT,
        DOWAGER,
        HEIR,
        HAND,
        COMMANDER,
        HERALD,
        GRAND_MAESTER,
        MASTER_OF_LAWS,
        AMBASSADOR
    }

    enum ForeignCapital {
        NONE,
        ANY,
        POSITIVE,
        FRIENDLY
    }

    record Definition(
            String id,
            Set<Reference> callReferences,
            Set<Reference> responseReferences,
            ForeignCapital foreignCapital,
            boolean requiresMourning
    ) {
        Definition {
            callReferences = Set.copyOf(callReferences);
            responseReferences = Set.copyOf(responseReferences);
            foreignCapital = foreignCapital == null ? ForeignCapital.NONE : foreignCapital;
        }
    }

    private static ResourceManager cachedResourceManager;
    private static Map<Context, List<Definition>> cachedDefinitions = Map.of();

    private CapitalAmbientConversationDefinitions() {
    }

    static synchronized List<Definition> get(ServerLevel level, Context context) {
        if (level == null || context == null || level.getServer() == null) {
            return List.of();
        }

        ResourceManager resourceManager = level.getServer().getResourceManager();
        if (resourceManager != cachedResourceManager) {
            cachedResourceManager = resourceManager;
            cachedDefinitions = loadAll(resourceManager);
        }

        return cachedDefinitions.getOrDefault(context, List.of());
    }

    private static Map<Context, List<Definition>> loadAll(ResourceManager resourceManager) {
        Map<Context, List<Definition>> definitions = new EnumMap<>(Context.class);
        for (Context context : Context.values()) {
            definitions.put(context, load(resourceManager, context));
        }
        return Map.copyOf(definitions);
    }

    private static List<Definition> load(ResourceManager resourceManager, Context context) {
        ResourceLocation location = new ResourceLocation(
                MCACapitals.MODID,
                "talk_of_the_town/" + context.path() + ".json"
        );

        Optional<Resource> resource = resourceManager.getResource(location);
        if (resource.isEmpty()) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Missing Talk of the Town conversation definitions at {}",
                    location
            );
            return List.of();
        }

        try (Reader reader = resource.get().openAsReader()) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                return List.of();
            }

            JsonObject root = rootElement.getAsJsonObject();
            JsonArray conversations = root.getAsJsonArray("conversations");
            if (conversations == null || conversations.isEmpty()) {
                return List.of();
            }

            List<Definition> result = new ArrayList<>();
            for (JsonElement element : conversations) {
                Definition definition = parseDefinition(element);
                if (definition != null) {
                    result.add(definition);
                }
            }

            return List.copyOf(result);
        } catch (Exception ex) {
            MCACapitals.LOGGER.warn(
                    "[MCACapitals] Failed to load Talk of the Town conversation definitions at {}",
                    location,
                    ex
            );
            return List.of();
        }
    }

    private static Definition parseDefinition(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        if (!object.has("id")) {
            return null;
        }

        String id = object.get("id").getAsString();
        if (id == null || !id.matches("\\d{2,3}")) {
            return null;
        }

        return new Definition(
                id,
                parseReferences(object.getAsJsonArray("call_references")),
                parseReferences(object.getAsJsonArray("response_references")),
                parseForeignCapital(object),
                object.has("requires_mourning")
                        && object.get("requires_mourning").getAsBoolean()
        );
    }

    private static ForeignCapital parseForeignCapital(JsonObject object) {
        if (object == null || !object.has("foreign_capital")) {
            return ForeignCapital.NONE;
        }

        String value = object.get("foreign_capital").getAsString();
        if (value == null || value.isBlank()) {
            return ForeignCapital.NONE;
        }

        try {
            return ForeignCapital.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ForeignCapital.NONE;
        }
    }

    private static Set<Reference> parseReferences(JsonArray array) {
        if (array == null || array.isEmpty()) {
            return Set.of();
        }

        EnumSet<Reference> references = EnumSet.noneOf(Reference.class);
        for (JsonElement element : array) {
            if (element == null || !element.isJsonPrimitive()) {
                continue;
            }

            try {
                references.add(
                        Reference.valueOf(
                                element.getAsString().trim().toUpperCase(Locale.ROOT)
                        )
                );
            } catch (IllegalArgumentException ignored) {
            }
        }

        return Set.copyOf(references);
    }

    static synchronized void clearCache() {
        cachedResourceManager = null;
        cachedDefinitions = Map.of();
    }

}
