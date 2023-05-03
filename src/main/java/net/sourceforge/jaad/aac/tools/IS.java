package net.sourceforge.jaad.aac.tools;

import net.sourceforge.jaad.aac.huffman.HCB;
import net.sourceforge.jaad.aac.syntax.CPE;
import net.sourceforge.jaad.aac.syntax.ICSInfo;
import net.sourceforge.jaad.aac.syntax.ICStream;

/**
 * Intensity stereo
 * @author in-somnia
 */
public final class IS implements ISScaleTable, HCB {

	private IS() {
	}

	public static void process(CPE cpe, float[] specL, float[] specR) {
		final ICStream ics = cpe.getRightChannel();
		final ICSInfo info = ics.getInfo();
		final int[] offsets = info.getSWBOffsets();
		final int windowGroups = info.getWindowGroupCount();
		final int maxSFB = info.getMaxSFB();
		final int[] sfbCB = ics.getSfbCB();
		final int[] sectEnd = ics.getSectEnd();
		final float[] scaleFactors = ics.getScaleFactors();

		int idx = 0, groupOff = 0;
		for(int g = 0; g<windowGroups; g++) {
			for(int i = 0; i<maxSFB;) {
				if(sfbCB[idx]==INTENSITY_HCB||sfbCB[idx]==INTENSITY_HCB2) {
					int end = sectEnd[idx];
					for(; i<end; i++, idx++) {
						int c = sfbCB[idx]==INTENSITY_HCB ? 1 : -1;
						if(cpe.isMSMaskPresent())
							c *= cpe.isMSUsed(idx) ? -1 : 1;
						float scale = c*scaleFactors[idx];
						for(int w = 0; w<info.getWindowGroupLength(g); w++) {
							int off = groupOff+w*128+offsets[i];
							for(int j = 0; j<offsets[i+1]-offsets[i]; j++) {
								specR[off+j] = specL[off+j]*scale;
							}
						}
					}
				}
				else {
					int end = sectEnd[idx];
					idx += end-i;
					i = end;
				}
			}
			groupOff += info.getWindowGroupLength(g)*128;
		}
	}
}
