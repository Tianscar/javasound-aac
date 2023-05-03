package net.sourceforge.jaad.aac.ps;

import net.sourceforge.jaad.aac.syntax.BitStream;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 09:35
 */
class Envelope {

    boolean dt = false;
    final int[] prev;
    final int[] index;

    Envelope(int[] prev) {
        this.prev = prev;
        this.index = new int[prev.length];
    }

    void read(BitStream ld, EnvTables tables, int nr_par) {
        dt = ld.readBool();
        Huffman.Table huff = tables.table(dt);
        for(int n = 0; n<nr_par; n++) {
            index[n] = huff.read(ld);
        }
    }

    void reset() {
        dt = false;
        Arrays.fill(index, 0);
    }

    /**
     * Copy back previous indexes
     */
    void restore() {
        System.arraycopy(prev, 0, index, 0, prev.length);
    }

    void decode(EnvMode mode) {
        if(mode==null)
            reset();
        else {
            final int stride = mode.stride();
            final int nr_par = mode.nr_par;

            if(dt) {
                for(int i = 0; i<nr_par; i++) {
                    int p = prev[i*stride];
                    index[i] = mode.clip(p+index[i]);
                }
            } else {
                int p = index[0];
                for(int i = 1; i<nr_par; i++) {
                    p = mode.clip(p+index[i]);
                    index[i] = p;
                }
            }

            /**
             * coarse values for stride==2:
             * {0,1,2,3,4} -> {0,0,1,1,2,2,3,3,4,4}
             */
            if(stride>1) {
                for(int i = stride * nr_par-1; i>0; --i) {
                    index[i] = index[i/stride];
                }
            }
        }
    }

    /**
     * Map indexes.
     * Multiple indices are placed on higher bytes.
     * Index 0 can only be used at first place.
     *
     * Table 8.45 of Subpart 8
     */
    private static final int[] MAP20TO34 = {
            0,0|1<<8,1,2,2|3<<8,3,4,4,5,5,6,7,8,8,9,9,10,
            11,12,13,14,14,15,15,16,16,17,17,18,18,18,19,19,19};
    
    void map20To34() {

        // l==0 does not matter
        for(int l=index.length-1; l>0; --l) {
            int m = MAP20TO34[l];
            int i = index[m&0xff];
            m >>= 8;

            // find further indexes to add
            int n=1;
            while(m!=0) {
                i += index[m&0xff];
                m >>= 8;
                ++n;
            }
            i /= n;

            index[l] = i;
        }
    }
}
