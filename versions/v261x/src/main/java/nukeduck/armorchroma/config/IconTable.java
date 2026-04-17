package nukeduck.armorchroma.config;

import nukeduck.armorchroma.MaterialHelper;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class IconTable {
    public final Map<String, Integer> materials = new HashMap<>();
    public final Map<String, Integer> items = new HashMap<>();
    public final Map<String, Integer> special = new HashMap<>();

    public void putAll(IconTable other) {
        materials.putAll(other.materials);
        items.putAll(other.items);
        special.putAll(other.special);
    }

    public Integer getIconIndex(ItemStack stack) {
        if (stack == null) return null;

        Integer i = null;
        Item item = stack.getItem();
        String material = MaterialHelper.getMaterial(item);

        if (material != null) {
            i = Util.getGlob(materials, material);
        }

        if (i == null) {
            i = Util.getGlob(items, BuiltInRegistries.ITEM.getKey(item).getPath());
        }

        return i;
    }

    public Integer getSpecialIndex(String key) {
        return special.get(key);
    }
}
