package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.filterbank.FilterBank;
import net.sourceforge.jaad.aac.sbr.SBR;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 16.04.19
 * Time: 16:55
 */
abstract public class ChannelElement implements Element {

	abstract static class ChannelTag extends InstanceTag {

		protected ChannelTag(int id) {
			super(id);
		}

		abstract public boolean isChannelPair();

		@Override
		abstract public ChannelElement newElement(DecoderConfig config);
	}

	protected final DecoderConfig config;

	protected final ChannelTag tag;

	protected ChannelElement(DecoderConfig config, ChannelTag tag) {
		this.config = config;
		this.tag = tag;
	}

	public ChannelTag getElementInstanceTag() {
		return tag;
	}

	/**
	 * @return if this element represents a channel pair.
	 */
	abstract public boolean isChannelPair();

	/**
	 * A single channel may produce stereo using parametric stereo.
	 * @return if this stereo.
	 */
	abstract public boolean isStereo();

	protected SBR sbr;

	public void decode(BitStream in) {
		if(sbr!=null)
			sbr.invalidate();
	}

	abstract protected SBR openSBR();

	void decodeSBR(BitStream in, boolean crc) {

    	if(!config.isSBREnabled()) {
			return;
		}

   		if(sbr==null)
   			sbr = openSBR();

	    if(sbr!=null)
		   	sbr.decode(in, crc);
   	}

   	boolean isSBRPresent() {
   		return sbr!=null && sbr.isValid();
   	}

   	SBR getSBR() {
   		return sbr;
   	}

    private float[] dataL, dataR;

    private float[] newData() {
    	int len = config.getSampleLength();
    	return new float[len];
	}

    public float[] getDataL() {
    	if(dataL==null)
			dataL = newData();
    	return dataL;
	}

	public float[] getDataR() {
		if(dataR==null)
			dataR = newData();
		return dataR;
    }

	abstract public void process(FilterBank filterBank, List<CCE> cces, Consumer<float[]> target);

	void processDependentCoupling(List<CCE> cces, int couplingPoint, float[] dataL, float[] dataR) {

		final int elementID = getElementInstanceTag().getId();

		for (CCE cce : cces) {
			int index = 0;
			if(cce!=null&&cce.getCouplingPoint()==couplingPoint) {
				for(int c = 0; c<=cce.getCoupledCount(); c++) {
					int chSelect = cce.getCHSelect(c);
					if(cce.isChannelPair(c)==isChannelPair()&&cce.getIDSelect(c)==elementID) {
						if(chSelect!=1) {
							cce.applyDependentCoupling(index, dataL);
							if(chSelect!=0)
								index++;
						}
						if(chSelect!=2) {
							cce.applyDependentCoupling(index, dataR);
							index++;
						}
					}
					else
						index += 1+((chSelect==3) ? 1 : 0);
				}
			}
		}
	}

	void processIndependentCoupling(List<CCE> cces, float[] dataL, float[] dataR) {

		final int elementID = getElementInstanceTag().getId();

		for (CCE cce : cces) {
			int index = 0;
			if (cce != null && cce.getCouplingPoint() == CCE.AFTER_IMDCT) {
				for (int c = 0; c <= cce.getCoupledCount(); c++) {
					int chSelect = cce.getCHSelect(c);
					if (cce.isChannelPair(c) == isChannelPair() && cce.getIDSelect(c) == elementID) {
						if (chSelect != 1) {
							cce.applyIndependentCoupling(index, dataL);
							if (chSelect != 0)
								index++;
						}
						if (chSelect != 2) {
							cce.applyIndependentCoupling(index, dataR);
							index++;
						}
					} else
						index += 1 + ((chSelect == 3) ? 1 : 0);
				}
			}
		}
	}
}
