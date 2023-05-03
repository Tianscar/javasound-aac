package net.sourceforge.jaad.aac.sbr;

import net.sourceforge.jaad.aac.syntax.BitStream;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 13.05.21
 * Time: 18:57
 */
class Header {
    boolean bs_amp_res = true;
    int bs_start_freq = 5;
    int bs_stop_freq;
    int bs_xover_band;
    int bs_freq_scale = 2;
    boolean bs_alter_scale = true;
    int bs_noise_bands = 2;
    int bs_limiter_bands = 2;
    int bs_limiter_gains = 2;
    boolean bs_interpol_freq;
    boolean bs_smoothing_mode;

    public void decode(BitStream ld) {

        this.bs_amp_res = ld.readBool();

        /* bs_start_freq and bs_stop_freq must define a frequency band that does
         not exceed 48 channels */
        this.bs_start_freq = ld.readBits(4);
        this.bs_stop_freq = ld.readBits(4);
        this.bs_xover_band = ld.readBits(3);

        ld.readBits(2); //reserved

        boolean bs_header_extra_1 = ld.readBool();
        boolean bs_header_extra_2 = ld.readBool();

        if (bs_header_extra_1) {
            this.bs_freq_scale = ld.readBits(2);
            this.bs_alter_scale = ld.readBool();
            this.bs_noise_bands = ld.readBits(2);
        } else {
            /* Default values */
            this.bs_freq_scale = 2;
            this.bs_alter_scale = true;
            this.bs_noise_bands = 2;
        }

        if (bs_header_extra_2) {
            this.bs_limiter_bands = ld.readBits(2);
            this.bs_limiter_gains = ld.readBits(2);
            this.bs_interpol_freq = ld.readBool();
            this.bs_smoothing_mode = ld.readBool();
        } else {
            /* Default values */
            this.bs_limiter_bands = 2;
            this.bs_limiter_gains = 2;
            this.bs_interpol_freq = true;
            this.bs_smoothing_mode = true;
        }
    }

    /**
     * If ay of these parameters differs the system must is reset.
     *
     * @param prev header to compare
     * @return if any relevant parameter differs or the previous header was null.
     */
    public boolean differs(Header prev) {
        return prev == null
                || this.bs_start_freq != prev.bs_start_freq
                || this.bs_stop_freq != prev.bs_stop_freq
                || this.bs_freq_scale != prev.bs_freq_scale
                || this.bs_alter_scale != prev.bs_alter_scale
                || this.bs_xover_band != prev.bs_xover_band
                || this.bs_noise_bands != prev.bs_noise_bands;
    }
}
