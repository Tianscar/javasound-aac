package net.sourceforge.jaad.aac.ps;

import net.sourceforge.jaad.aac.syntax.BitStream;

import java.util.Arrays;

import static net.sourceforge.jaad.aac.ps.PSConstants.MAX_PS_ENVELOPES;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 02:13
 */
abstract public class EnvData<Mode extends EnvMode> {

    final int[] first;
    final Envelope[] envs;

    Mode mode = null;

    EnvData(int len) {
        first = new int[len];
        envs = new Envelope[MAX_PS_ENVELOPES];

        /**
         * Each Envelope refers to its predecessor.
         * The first envelop refers to prev.
         */
        int[] prev = first;
        for(int l=0; l<envs.length; ++l) {
            Envelope e = new Envelope(prev);
            envs[l] = e;
            prev = e.index;
        }
    }

    Mode mode() {
        return mode == null ? mode(0) : mode;
    }

    abstract protected Mode mode(int id);

    void readData(BitStream ld, int num_env) {
        if(mode!=null) {
            for(int n = 0; n<num_env; n++)
                envs[n].read(ld, mode.tables(), mode.nr_par);
        }
    }

    void decode(int num_env) {
        /* handle error case and restore or reset envs[0]*/
        if(num_env==0) {
            if(mode!=null)
                envs[0].restore();
            else
                envs[0].reset();

        } else {
            for (int env = 0; env < num_env; env++)
                envs[env].decode(mode);
        }
    }

    void update(int num_env) {
        if(num_env==0)
            Arrays.fill(first, 0);
        else
            System.arraycopy(envs[num_env-1].index, 0, first, 0, first.length);
    }

    void restore(int num_env) {
        envs[num_env].restore();
    }

    void mapTo34(int num_env) {
        if(mode!=null && (mode.id%3)!=2) {
            for (int env = 0; env < num_env; env++)
                envs[env].map20To34();
        }
    }
}
