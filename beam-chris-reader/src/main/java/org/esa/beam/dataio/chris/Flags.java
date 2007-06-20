package org.esa.beam.dataio.chris;

import java.awt.*;

/**
 * The enumeration type {@code Flags} is a representation of
 * the product flags used by CHRIS.
 *
 * @author Ralf Quast
 * @version $Revision: $ $Date: $
 */
public enum Flags {

    /**
     * Dropout flag.
     */
    DROPOUT(0x0001, "Dropout pixel", Color.red),
    /**
     * Saturation flag.
     */
    SATURATED(0x0002, "Saturated pixel", Color.orange),
    /**
     * Correction flag.
     */
    CORRECTED(0x0004, "Corrected pixel", Color.yellow);

    private int mask;
    private Color color;
    private float transparency;
    private String description;

    private Flags(final int mask, final String description, final Color color) {
        this(mask, description, color, 0.5f);
    }

    private Flags(final int mask, final String description, final Color color, final float transparency) {
        this.mask = mask;
        this.color = color;
        this.transparency = transparency;
        this.description = description;
    }

    /**
     * Returns the bit mask associated with the flag.
     *
     * @return the bit mask.
     */
    public final int getMask() {
        return mask;
    }

    /**
     * Returns the textual description of the flag.
     *
     * @return the textual description.
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Returns the color associated with this flag (useful for colored bit mask layers).
     *
     * @return the color.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the transparency associated with this flag (useful for colored bit mask layers).
     *
     * @return the transparency.
     */
    public final float getTransparency() {
        return transparency;
    }

    /**
     * Tests a bit pattern for the status of the flag.
     *
     * @param value the bit pattern.
     * @return true if the flag is set, false otherwise.
     */
    public final boolean isSet(final int value) {
        return (value & mask) != 0;
    }

    @Override
    public final String toString() {
        return name().toLowerCase();
    }

}
