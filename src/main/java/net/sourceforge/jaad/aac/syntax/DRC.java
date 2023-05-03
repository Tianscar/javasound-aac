package net.sourceforge.jaad.aac.syntax;

/**
 * Dynamic Range Control (4.5.2.7)
 *
 * This is currently unused.
 * The parameters are to be applied to all channels.
 */
public class DRC {

    private static final int MAX_NBR_BANDS = 7;
    private final boolean[] excludeMask;
    private final boolean[] additionalExcludedChannels;
    private boolean pceTagPresent;
    private int pceInstanceTag;
    private int tagReservedBits;
    private boolean excludedChannelsPresent;
    private boolean bandsPresent;
    private int bandsIncrement, interpolationScheme;
    private int[] bandTop;
    private boolean progRefLevelPresent;
    private int progRefLevel, progRefLevelReservedBits;
    private boolean[] dynRngSgn;
    private int[] dynRngCtl;

    public DRC() {
        excludeMask = new boolean[MAX_NBR_BANDS];
        additionalExcludedChannels = new boolean[MAX_NBR_BANDS];
    }
    
    public void decode(BitStream in) {

   		int bandCount = 1;
   
   		//pce tag
   		if(pceTagPresent = in.readBool()) {
   			pceInstanceTag = in.readBits(4);
   			tagReservedBits = in.readBits(4);
   		}
   
   		//excluded channels
   		if(excludedChannelsPresent = in.readBool()) {
   			decodeExcludedChannels(in);
   		}
   
   		//bands
   		if(bandsPresent = in.readBool()) {
   			bandsIncrement = in.readBits(4);
   			interpolationScheme = in.readBits(4);
   			bandCount += bandsIncrement;
   			bandTop = new int[bandCount];
   			for(int i = 0; i<bandCount; i++) {
   				bandTop[i] = in.readBits(8);
   			}
   		}
   
   		//prog ref level
   		if(progRefLevelPresent = in.readBool()) {
   			progRefLevel = in.readBits(7);
   			progRefLevelReservedBits = in.readBits(1);
   		}
   
   		dynRngSgn = new boolean[bandCount];
   		dynRngCtl = new int[bandCount];
   		for(int i = 0; i<bandCount; i++) {
   			dynRngSgn[i] = in.readBool();
   			dynRngCtl[i] = in.readBits(7);
   		}
   	}
   
   	private int decodeExcludedChannels(BitStream in) {
   		int exclChs = 0;
   
   		do {
   			for(int i = 0; i<7; i++) {
   				excludeMask[exclChs] = in.readBool();
   				exclChs++;
   			}
   		}
   		while(exclChs<57&&in.readBool());
   
   		return (exclChs/7)*8;
   	}
}
