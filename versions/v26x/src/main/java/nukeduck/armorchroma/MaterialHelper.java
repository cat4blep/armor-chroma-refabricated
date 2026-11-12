package nukeduck.armorchroma;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

public class MaterialHelper {

    private static final String TURTLE_MATERIAL = "turtle";

    @Nullable
    public static String getMaterial(Item item) {
        Equippable equippableComponent = item.components().get(DataComponents.EQUIPPABLE);

        if (equippableComponent != null) {
            // Armor materials don't have IDs anymore since 1.21.2, and models
            // are the closest thing to materials (with some exceptions such
            // as turtle scutes)
            Optional<ResourceKey<EquipmentAsset>> optionalAsset = equippableComponent.assetId();

            if (optionalAsset.isPresent()) {
                ResourceKey<EquipmentAsset> asset = optionalAsset.get();

                if (asset.equals(EquipmentAssets.TURTLE_SCUTE)) {
                    return TURTLE_MATERIAL;
                } else {
                    return asset.identifier().getPath();
                }
            }
        }

        return null;
    }

}
