package net.sourceforge.jaad.aac.ps;

import net.sourceforge.jaad.aac.tools.Utils;

import static net.sourceforge.jaad.aac.ps.Huffman.*;
import static net.sourceforge.jaad.aac.ps.PSTables.*;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 00:21
 */
public class IIDMode extends ICMode {

    private static final IIDTables DEFAULT_TABLES = new IIDTables(
            f_huff_iid_def, t_huff_iid_def,
            7, sf_iid_normal,
            cos_betas_normal, sin_betas_normal,
            sin_gammas_normal, cos_gammas_normal,
            sincos_alphas_B_normal);

    private static final IIDTables FINE_TABLES = new IIDTables(
            f_huff_iid_fine, t_huff_iid_fine,
            15, sf_iid_fine,
            cos_betas_fine, sin_betas_fine,
            sin_gammas_fine, cos_gammas_fine,
            sincos_alphas_B_fine);

    final IIDTables tables;

    public IIDMode(int id, int nr_par, IIDTables tables) {
        super(id, nr_par);
        this.tables = tables;
    }

    @Override
    IIDTables tables() {
        return tables;
    }

    @Override
    int clip(int idx) {
        int clip = tables.num_steps;
        return Utils.clip(idx, -clip, clip);
    }

    static private final IIDMode[] IID_MODES = {
            new IIDMode(0, 10, DEFAULT_TABLES),
            new IIDMode(1, 20, DEFAULT_TABLES),
            new IIDMode(2, 34, DEFAULT_TABLES),
            new IIDMode(3, 10, FINE_TABLES),
            new IIDMode(4, 20, FINE_TABLES),
            new IIDMode(5, 34, FINE_TABLES)
    };

    static IIDMode mode(int id) {
        return IID_MODES[id];
    }

    public static IIDData data() {
        return new IIDData();
    }
}
