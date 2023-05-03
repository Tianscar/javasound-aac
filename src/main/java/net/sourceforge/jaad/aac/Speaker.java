package net.sourceforge.jaad.aac;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 26.12.20
 * Time: 10:26
 */

/**
 * see: https://en.wikipedia.org/wiki/Surround_sound
 */

public enum Speaker {

    FL("Front Left"),
    FR("Front Right"),
    FC("Front Center"),
    LFE("Low Frequency"),
    BL("Back Left"),
    BR("Back Right"),
    FLC("Front Left of Center"),
    FRC("Front Right of Center"),
    BC("Back Center"),
    SL("Side Left"),
    SR("Side Right"),
    TC("Top Center"),
    TFL("Front Left Height"),
    TFC("Front Center Height"),
    TFR("Front Right Height"),
    TBL("Rear Left Height"),
    TBC("Rear Center Height"),
    TBR("Rear Right Height");

    final String name;

    Speaker(String name) {
        this.name = name;
    }
}
