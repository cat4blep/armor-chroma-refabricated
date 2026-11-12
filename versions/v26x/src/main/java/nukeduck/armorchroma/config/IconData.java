package nukeduck.armorchroma.config;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import nukeduck.armorchroma.ArmorChroma;

public class IconData extends SimplePreparableReloadListener<Map<String, IconTable>> {
    private Map<String, IconTable> mods = Map.of();

    private static final ArmorIcon FALLBACK_ICON = new ArmorIcon(0);
    private static final String DEFAULT = "default";
    private static final String MINECRAFT = Identifier.DEFAULT_NAMESPACE;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ArmorChroma.MODID, "icondata");

    /**
     * @return The armor icon corresponding to {@code stack}
     */
    public ArmorIcon getIcon(ItemStack stack) {
        String modId = Util.getModId(stack);
        IconTable mod = mods.get(modId);

        Integer i;

        if (mod != null) {
            i = mod.getIconIndex(stack);

            if (i != null) {
                return new ArmorIcon(modId, i, Util.getColor(stack));
            } else {
                return getSpecial(modId, SpecialIconKey.DEFAULT);
            }
        }

        return getSpecial(SpecialIconKey.DEFAULT);
    }

    public ArmorIcon getSpecial(SpecialIconKey key) {
        return getSpecial(MINECRAFT, key);
    }

    public ArmorIcon getSpecial(String modid, SpecialIconKey key) {
        ArmorIcon icon = getSpecialWithoutFallback(modid, key);

        if (icon == null && !modid.equals(MINECRAFT)) {
            icon = getSpecialWithoutFallback(MINECRAFT, key);
        }

        return icon != null ? icon : FALLBACK_ICON;
    }

    private ArmorIcon getSpecialWithoutFallback(String modid, SpecialIconKey key) {
        IconTable mod = mods.get(modid);

        if (mod != null) {
            Integer i = mod.getSpecialIndex(key.value);

            if (i != null) {
                return new ArmorIcon(modid, i);
            }
        }

        return null;
    }

    @Override
    protected Map<String, IconTable> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, IconTable> loaded = new HashMap<>();
        for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
            String modid = modContainer.getMetadata().getId();
            List<Resource> resources = manager.getResourceStack(Identifier.fromNamespaceAndPath(modid, "textures/gui/armor_chroma.json"));
            for (Resource resource : resources) {
                try (Reader reader = new BufferedReader(new InputStreamReader(resource.open()))) {
                    IconTable mod = new Gson().fromJson(reader, IconTable.class);
                    if (mod != null) {
                        loaded.merge(modid, mod, (a, b) -> {
                            a.putAll(b);
                            return a;
                        });
                    }
                } catch (JsonSyntaxException | JsonIOException | IOException e) {
                    ArmorChroma.LOGGER.error("[Armor Chroma] Error loading icons for {}", modid, e);
                }
            }
        }

        IconTable minecraft = loaded.get(MINECRAFT);
        if (minecraft == null || minecraft.getSpecialIndex(DEFAULT) == null
                || minecraft.getSpecialIndex("leadingMask") == null
                || minecraft.getSpecialIndex("trailingMask") == null) {
            throw new IllegalStateException("Missing fallback icons. The mod is damaged");
        }
        return loaded;
    }

    @Override
    protected void apply(Map<String, IconTable> loaded, ResourceManager manager, ProfilerFiller profiler) {
        mods = loaded;
        ArmorChroma.LOGGER.info("[Armor Chroma] Loaded icons for {} mods", mods.size() - 1);
    }

}
