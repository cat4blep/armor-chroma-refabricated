package nukeduck.armorchroma;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ArmorChromaDebugCommand {

    private static final DynamicCommandExceptionType FAILED_ITEMLESS_EXCEPTION = new DynamicCommandExceptionType(
            entityName -> Component.translatableEscape("commands.enchant.failed.itemless", entityName)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("armorchroma").then(
                literal("setarmor").then(
                        argument("points", doubleArg())
                                .executes(ArmorChromaDebugCommand::executeSetArmor)
                ).then(
                        literal("reset")
                                .executes(ArmorChromaDebugCommand::executeResetArmor)
                )
        ).then(
                literal("setglint").then(
                        argument("glint", bool())
                                .executes(ArmorChromaDebugCommand::executeSetGlint)
                ).then(
                        literal("reset")
                                .executes(ArmorChromaDebugCommand::executeResetGlint)
                )
        ));
    }


    private static int executeSetArmor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getActiveStack(context);
        double points = getDouble(context, "points");
        ItemAttributeModifiers originalModifiersComponent = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        List<ItemAttributeModifiers.Entry> modifiers;

        if (originalModifiersComponent == null) {
            modifiers = new ArrayList<>(1);
        } else {
            modifiers = originalModifiersComponent.modifiers()
                    .stream()
                    .filter(entry -> entry.attribute() != Attributes.ARMOR)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        AttributeModifier attributeModifier = new AttributeModifier(Identifier.fromNamespaceAndPath(ArmorChroma.MODID, UUID.randomUUID().toString()), points, AttributeModifier.Operation.ADD_VALUE);
        modifiers.add(new ItemAttributeModifiers.Entry(Attributes.ARMOR, attributeModifier, EquipmentSlotGroup.ANY));
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(modifiers));

        return Command.SINGLE_SUCCESS;
    }

    private static int executeResetArmor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getActiveStack(context);
        ItemAttributeModifiers oldComponent = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);

        if (oldComponent != null) {
            List<ItemAttributeModifiers.Entry> modifiers = oldComponent.modifiers()
                    .stream()
                    .filter(entry -> entry.attribute() != Attributes.ARMOR || !entry.modifier().id().getNamespace().equals(ArmorChroma.MODID))
                    .toList();

            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(modifiers));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeSetGlint(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = getActiveStack(context);
        boolean glint = getBool(context, "glint");

        if (glint) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeResetGlint(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        getActiveStack(context).remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        return Command.SINGLE_SUCCESS;
    }


    private static ItemStack getActiveStack(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            throw FAILED_ITEMLESS_EXCEPTION.create(player.getName().getString());
        }

        return stack;
    }
}
