package nukeduck.armorchroma;

import nukeduck.armorchroma.config.ArmorIcon;
import nukeduck.armorchroma.config.SpecialIconKey;
import nukeduck.armorchroma.config.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import static nukeduck.armorchroma.ArmorChroma.TEXTURE_SIZE;

/**
 * Renders the armor bar in the HUD
 */
public class GuiArmor {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(ArmorChroma.MODID, "textures/gui/background.png");
    private static final Identifier ENCHANTED_GLINT_ITEM = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png");

    /**
     * The colors used for the border of the bar at different levels
     * @see #drawBackground(GuiGraphicsExtractor, int, int, int)
     */
    private static final int[] BG_COLORS = {0xff3acaff, 0xff3be55a, 0xffffff00, 0xffff9d00, 0xffed3200, 0xff7130c1};

    /**
     * The vertical distance between the top of each row
     */
    private static final int ROW_SPACING = 5;

    /**
     * The number of armor points per row in the armor bar
     */
    private static final int ARMOR_PER_ROW = 20;

    /**
     * Fallback attributes required when getting the player's armor
     */
    private static final AttributeSupplier FALLBACK_ATTRIBUTES = AttributeSupplier.builder()
            .add(Attributes.ARMOR).build();

    private final Minecraft client = Minecraft.getInstance();

    /**
     * Render the bar as a full replacement for vanilla
     */
    public void draw(GuiGraphicsExtractor context, int x, int y) {
        List<ArmorBarSegment> segments = new ArrayList<>(EquipmentSlot.VALUES.size());
        int totalPoints = buildArmorSegments(client.player, segments);
        if (totalPoints <= 0) return;

        int barPoints = 0;
        int compressedRows = ArmorChroma.config.compressBar() ? compressRows(segments, totalPoints) : 0;

        drawBackground(context, x, y, compressedRows);

        for (ArmorBarSegment segment : segments) {
            drawSegment(context, x, y, barPoints, segment);
            barPoints += segment.getArmorPoints();
        }
    }

    /**
     * Draws the armor bar background and, if {@code level > 0}, with a border
     * @param level The colored border level where a level is one full row
     * ({@link #ARMOR_PER_ROW} armor points)
     */
    private void drawBackground(GuiGraphicsExtractor context, int x, int y, int level) {
        boolean drawBorder = level > 0;

        // Plain background
        if (ArmorChroma.config.renderBackground() || drawBorder) {
            context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0, 0, 81, 9, TEXTURE_SIZE, TEXTURE_SIZE);

            // Colored border
            if (drawBorder) {
                int color = level <= BG_COLORS.length ? BG_COLORS[level - 1] : BG_COLORS[BG_COLORS.length - 1];
                context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x - 1, y - 1, 81, 0, 83, 11, TEXTURE_SIZE, TEXTURE_SIZE, color);
            }
        }

    }

    /**
     * Draws all the rows needed for a single piece of armor
     * @param barPoints The number of points in the bar before this piece
     */
    private void drawSegment(GuiGraphicsExtractor context, int x, int y, int barPoints, ArmorBarSegment segment) {
        int space;
        y -= (barPoints / ARMOR_PER_ROW) * ROW_SPACING; // Offset to account for full bars
        int stackPoints = segment.getArmorPoints();

        // Repeatedly fill rows when possible
        while ((space = ARMOR_PER_ROW - (barPoints % ARMOR_PER_ROW)) <= stackPoints) {
            drawPartialRow(context, x, y, ARMOR_PER_ROW - space, space, segment);
            addZOffset(context, -3); // Move out of range of glint offset

            // Move up a row
            y -= ROW_SPACING;
            barPoints += space;
            stackPoints -= space;
        }

        // Whatever's left over (doesn't fill the whole row)
        if (stackPoints > 0) {
            drawPartialRow(context, x, y, ARMOR_PER_ROW - space, stackPoints, segment);
            addZOffset(context, -1);
        }
    }

    /**
     * Renders a partial row of icons, {@code stackPoints} wide
     * @param barPoints The points already in the bar
     */
    private void drawPartialRow(GuiGraphicsExtractor context, int left, int top, int barPoints, int stackPoints, ArmorBarSegment segment) {
        ArmorIcon icon = segment.getIcon();

        if (segment.hasGlint()) {
            addZOffset(context, 2); // Glint rows should appear on top of normal rows
        }

        int i = barPoints & 1;
        int x = left + barPoints * 4;

        // Drawing icons starts here

        if (i == 1) { // leading half icon
            drawMaskedIcon(context, x - 4, top, icon, segment.getLeadingMask());
            x += 4;
        }

        for (; i < stackPoints - 1; i += 2, x += 8) { // Main body icons
            icon.draw(context, x, top);
        }

        if (i < stackPoints) { // Trailing half icon
            drawMaskedIcon(context, x, top, icon, segment.getTrailingMask());
        }

        if (segment.hasGlint()) { // Draw one glint quad for the whole row
            drawTexturedGlintRect(context, left + barPoints * 4, top, left, top, stackPoints * 4 + 1, ArmorIcon.ICON_SIZE);
            addZOffset(context, -2);
        }
    }

    /**
     * Finds all items in the player's equipment slots that provide armor
     * @param player The player holding the items
     * @param segments The segments making up the armor bar
     * @return The total number of armor points the player has
     */
    private int buildArmorSegments(LocalPlayer player, List<ArmorBarSegment> segments) {
        AttributeMap attributes = new AttributeMap(FALLBACK_ATTRIBUTES);
        AttributeInstance armor = attributes.getInstance(Attributes.ARMOR);
        if (armor == null) return 0;

        int displayedArmorCap = ArmorChroma.config.getDisplayedArmorCap();
        int totalPoints = (int) ((EntityAttributeInstanceAccess) armor).armorChroma_getUnclampedValue();

        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack stack = player.getItemBySlot(slot);
            stack.forEachModifier(slot, (attribute, modifier) -> {
                if (attribute == Attributes.ARMOR && !armor.hasModifier(modifier.id())) {
                    armor.addTransientModifier(modifier);
                }
            });

            int nextTotalPoints = Math.min(displayedArmorCap, (int) ((EntityAttributeInstanceAccess) armor).armorChroma_getUnclampedValue());
            int pointDelta = nextTotalPoints - totalPoints;
            totalPoints = nextTotalPoints;

            if (pointDelta > 0) {
                ArmorIcon icon = ArmorChroma.ICON_DATA.getIcon(stack);
                boolean hasGlint = ArmorChroma.config.renderGlint() && stack.hasFoil();
                String modId = Util.getModId(stack);
                ArmorIcon leadingMask = ArmorChroma.ICON_DATA.getSpecial(modId, SpecialIconKey.LEADING_MASK);
                ArmorIcon trailingMask = ArmorChroma.ICON_DATA.getSpecial(modId, SpecialIconKey.TRAILING_MASK);
                ArmorBarSegment segment = new ArmorBarSegment(pointDelta, icon, leadingMask, trailingMask, hasGlint);

                if (!segments.isEmpty() && segment.canMergeWith(segments.getLast())) {
                    segments.getLast().addArmorPoints(segment.getArmorPoints());
                } else {
                    segments.add(segment);
                }
            }
        }

        if (ArmorChroma.config.reverse()) {
            Collections.reverse(segments);
        }

        return totalPoints;
    }

    /**
     * Removes leading full rows from the points map
     * @param segments The segments making up the armor bar
     * @return The number of compressed rows
     */
    private int compressRows(List<ArmorBarSegment> segments, int totalPoints) {
        int compressedRows = (totalPoints - 1) / ARMOR_PER_ROW;
        int compressedPoints = compressedRows * ARMOR_PER_ROW;
        int segmentsToRemove = 0;
        int pointsSoFar = 0;

        for (ArmorBarSegment segment : segments) {
            pointsSoFar += segment.getArmorPoints();

            if (pointsSoFar <= compressedPoints) {
                segmentsToRemove++;
            } else {
                segment.setArmorPoints(pointsSoFar - compressedPoints);
                break;
            }
        }

        segments.subList(0, segmentsToRemove).clear();
        return compressedRows;
    }

    private void drawMaskedIcon(GuiGraphicsExtractor context, int x, int y, ArmorIcon icon, ArmorIcon mask) {
        mask.draw(context, x, y);
        icon.drawMasked(context, x, y);
    }

    /**
     * Render an item glint over the specified quad, blending with equal depth
     */
    @SuppressWarnings("SameParameterValue")
    private void drawTexturedGlintRect(GuiGraphicsExtractor context, int x, int y, float u, float v, int width, int height) {
        float intensity = client.options.glintStrength().get().floatValue()
                * ArmorChroma.config.glintIntensity();
        int color = ARGB.white(Mth.clamp(intensity, 0, 1));
        context.blit(RenderPipelines.GLINT, ENCHANTED_GLINT_ITEM, x, y, u, v, width, height, TEXTURE_SIZE, TEXTURE_SIZE, color);
    }

    private void addZOffset(GuiGraphicsExtractor context, int z) {
        // GuiGraphicsExtractor uses stratums instead of z translation, and draw order is sufficient here.
    }
}
