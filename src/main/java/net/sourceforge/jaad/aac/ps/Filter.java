package net.sourceforge.jaad.aac.ps;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 10.09.21
 * Time: 19:04
 */
public interface Filter {

    static float sind(float deg) {
        return (float) Math.sin(Math.toRadians(deg));
    }
    static float sqrt(float x) {
        return (float) Math.sqrt(Math.toRadians(x));
    }

    float SIN_75 = sind(75);
    float SIN_675 = sind(67.5f);
    float SIN_60 = sind(60);
    float SIN_30 = 0.5f;
    float SIN_45 = sqrt(0.5f);
    float SIN_15 = sind(15);

    float S2P = (float) Math.sqrt(1 + SIN_45);
    float S2M = (float) Math.sqrt(1 - SIN_45);

    int filter(int frame_len, float[][] buffer, float[][][] result);

    int resolution();
}
