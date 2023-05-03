package net.sourceforge.jaad.aac.ps;

import net.sourceforge.jaad.aac.syntax.BitStream;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 25.09.21
 * Time: 12:51
 */
public class Extension {

    final IIDData parent;

    boolean enabled  = false;

    ExtData data = null;

    public Extension(IIDData parent) {
        this.parent = parent;
    }

    ExtData data() {
        if(enabled && data==null)
            data = new ExtData();

        return data;
    }

    boolean readMode(BitStream ld) {
        enabled = ld.readBool();
        ExtData data = data();

        if(data!=null)
            data.setMode(enabled ? parent.mode : null);

        return enabled;
    }

    void readData(BitStream ld, int num_env) {
        if(enabled) {
            int cnt = ld.readBits(4);
 			if(cnt==15) {
 				cnt += ld.readBits(8);
 			}

 			// open a new sub stream
 			ld = ld.readSubStream(8*cnt);

 			while(ld.getBitsLeft()>7) {
                int ps_extension_id = ld.readBits(2);
                if(ps_extension_id==0) {
                    ExtData data = data();
                    if(data!=null)
                       data.readData(ld, num_env);
                }
 			}
        }
    }

    void decode(int num_env) {
        if(enabled && data!=null)
            data.decode(num_env);
    }

    void update(int num_env) {
        if(enabled && data!=null)
            data.update(num_env);
    }

    void restore(int num_env) {
        if(enabled && data!=null)
            data.restore(num_env);
    }

    public void mapTo34(int num_env) {
        if(enabled && data!=null)
            data.mapTo34(num_env);
    }

    int nr_par() {
        if(enabled && data!=null)
            return data.nr_par();
        else
            return 0;
    }
}
