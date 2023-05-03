package net.sourceforge.jaad.aac.ps;

import static net.sourceforge.jaad.aac.ps.Huffman.*;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 01:38
 */
public class PDMode extends EnvMode {

    final EnvTables tables;

    public PDMode(int id, int nr_par, EnvTables tables) {
        super(id, nr_par);
        this.tables = tables;
    }

    @Override
    EnvTables tables() {
        return tables;
    }

    @Override
    int stride() {
        return 1;
    }

    @Override
    /**
     * modulo clip on 7
     */
    int clip(int idx) {
        return idx & 7;
    }

    static private final PDMode[] IPD_MODES = modes(f_huff_ipd, t_huff_ipd);

    static private final PDMode[] OPD_MODES = modes(f_huff_opd, t_huff_opd);

    public static PDData IpdData() {
        return new PDData(IPD_MODES);
    }

    public static PDData OpdData() {
        return new PDData(OPD_MODES);
    }

    private static PDMode[] modes(int[][] f, int[][] t) {
        EnvTables tables = new EnvTables(f, t);

        return new PDMode[] {
                new PDMode(0,  5, tables),
                new PDMode(1,  11, tables),
                new PDMode(2,  17, tables),
                new PDMode(3,  5, tables),
                new PDMode(4,  11, tables),
                new PDMode(5,  17, tables)
        };
    }
}
