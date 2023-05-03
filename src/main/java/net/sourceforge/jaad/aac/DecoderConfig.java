package net.sourceforge.jaad.aac;

import net.sourceforge.jaad.aac.filterbank.FilterBank;
import net.sourceforge.jaad.aac.ps.PSImpl;
import net.sourceforge.jaad.aac.sbr.PS;
import net.sourceforge.jaad.aac.sbr.SBR;
import net.sourceforge.jaad.aac.syntax.BitStream;
import net.sourceforge.jaad.aac.syntax.PCE;

import static net.sourceforge.jaad.aac.SampleFrequency.SF_NONE;

/**
 * DecoderConfig that must be passed to the <code>Decoder</code> constructor.
 *
 * See 1.6.2.1 AudioSpecificConfig
 *
 * @author in-somnia
 */
public class DecoderConfig {

	private Profile profile = Profile.AAC_MAIN, extProfile = Profile.UNKNOWN;
	private SampleRate sampleFrequency = SF_NONE;
	private ChannelConfiguration channelConfiguration = ChannelConfiguration.CHANNEL_CONFIG_UNSUPPORTED;
	private ChannelConfiguration extChannelConfiguration = ChannelConfiguration.CHANNEL_CONFIG_UNSUPPORTED;
	private boolean frameLengthFlag=false;
	private boolean dependsOnCoreCoder=false;
	private int coreCoderDelay = 0;
	private boolean extensionFlag=false;
	//extension: SBR
	private final boolean sbrEnabled;
	private boolean sbrPresent=false;
	// in case of SBR this may be twice the SampleFrequency.
	// it remains null without SBR
	private SampleRate outputFrequency;

	private boolean psEnabled = true;
	private boolean psPresent = false;

	public PS openPS(SBR sbr) {
		psPresent = true;
		return new PSImpl(sbr.numTimeSlotsRate);
	}

	//extension: error resilience
	private boolean sectionDataResilience=false, scalefactorResilience=false, spectralDataResilience=false;

	DecoderConfig(boolean sbrEnabled) {
		this.sbrEnabled = sbrEnabled;
	}

	DecoderConfig() {
		this(true);
	}

	/* ========== gets/sets ========== */
	public ChannelConfiguration getChannelConfiguration() {
		return channelConfiguration;
	}

	public DecoderConfig setAudioDecoderInfo(AudioDecoderInfo info) {
		profile = info.getProfile();
		sampleFrequency = info.getSampleFrequency();
		channelConfiguration = info.getChannelConfiguration();
		return this;
	}

	public int getCoreCoderDelay() {
		return coreCoderDelay;
	}

	public boolean isDependsOnCoreCoder() {
		return dependsOnCoreCoder;
	}

	public Profile getExtObjectType() {
		return extProfile;
	}

	public int getFrameLength() {
		return frameLengthFlag ? FilterBank.WINDOW_SMALL_LEN_LONG : FilterBank.WINDOW_LEN_LONG;
	}

	public int getSampleLength() {
		int upsampled = outputFrequency!= null && sampleFrequency != outputFrequency ? 2 : 1;
		return upsampled * getFrameLength();
	}

	public boolean isSmallFrameUsed() {
		return frameLengthFlag;
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	public SampleRate getSampleFrequency() {
		return sampleFrequency;
	}

	public SampleRate getOutputFrequency() {
		return outputFrequency!=null ? outputFrequency : sampleFrequency;
	}

	public int getChannelCount() {

		// expect HE AAC v2 with PS
		if(sbrEnabled && channelConfiguration==ChannelConfiguration.MONO)
			return 2;

		return channelConfiguration.getChannelCount();
	}

	//=========== SBR =============

	/**
	 * Setup SBR and try to duplicate the output frequency if possible.
	 *
	 *  @return true if the frequency could be duplicated.
	 */
	public boolean setSBRPresent() {
		sbrPresent = true;

		if(outputFrequency==null) {
			SampleRate duplicated = sampleFrequency.duplicated();
			if (duplicated == SF_NONE)
				return false;
			outputFrequency = duplicated;
		}

		return isUpSampled();
	}

	boolean isUpSampled() {
		return outputFrequency!=null && outputFrequency!=sampleFrequency;
	}

	public boolean isSBREnabled() {
		return sbrEnabled;
	}

	public boolean isPSEnabled() {
		return psEnabled;
	}
	
	//=========== ER =============
	public boolean isScalefactorResilienceUsed() {
		return scalefactorResilience;
	}

	public boolean isSectionDataResilienceUsed() {
		return sectionDataResilience;
	}

	public boolean isSpectralDataResilienceUsed() {
		return spectralDataResilience;
	}

	/* ======== static builder ========= */

	public static DecoderConfig create(AudioDecoderInfo info) {
		return new DecoderConfig().setAudioDecoderInfo(info);
	}

	/**
	 * Parses the input arrays as a DecoderSpecificInfo, as used in MP4
	 * containers.
	 *
	 * see: 1.6.2.1 AudioSpecificConfig
	 * @return a DecoderConfig
	 */
	public DecoderConfig decode(BitStream in) {

		profile = readProfile(in);

		sampleFrequency = SampleRate.decode(in);
		outputFrequency = sampleFrequency;

		channelConfiguration = ChannelConfiguration.forInt(in.readBits(4));

		switch(profile) {
			case AAC_PS:
				psPresent = true;
			    // implies SBR
			case AAC_SBR:
				SampleRate frequency = SampleRate.decode(in);

				extProfile = profile;
				profile = readProfile(in);

				if(sbrEnabled) {
					outputFrequency = frequency;
				}

				break;

			case AAC_MAIN:
			case AAC_LC:
			case AAC_SSR:
			case AAC_LTP:
			case ER_AAC_LC:
			case ER_AAC_LTP:
			case ER_AAC_LD:
				//ga-specific info:
				frameLengthFlag = in.readBool();
				if(frameLengthFlag)
					throw new AACException("config uses 960-sample frames, not yet supported"); //TODO: are 960-frames working yet?

				dependsOnCoreCoder = in.readBool();

				if(dependsOnCoreCoder)
					coreCoderDelay = in.readBits(14);
				else
					coreCoderDelay = 0;

				extensionFlag = in.readBool();

				if(extensionFlag) {
					if(profile.isErrorResilientProfile()) {
						sectionDataResilience = in.readBool();
						scalefactorResilience = in.readBool();
						spectralDataResilience = in.readBool();
					}
					//extensionFlag3
					in.skipBit();
				}

				if(channelConfiguration==ChannelConfiguration.NONE) {
					//TODO: is this working correct? -> ISO 14496-3 part 1: 1.A.4.3
					//in.skipBits(3); //PCE
					PCE pce = PCE.read(in);
					setAudioDecoderInfo(pce);
				}

				if(sbrEnabled && in.getBitsLeft()>10)
					readSyncExtension(in);

				break;

			default:
				throw new AACException("profile not supported: "+profile.getIndex());
		}

		// expect implicit SBR for low frequencies
		// see 4.6.18.2.6
		//if(sbrEnabled && !sbrPresent && sampleFrequency.duplicated()!=SF_NONE) {
		//	setSBRPresent();
		//}

		return this;
	}

	private static Profile readProfile(BitStream in) {
		int i = in.readBits(5);
		if(i==31)
			i = 32+in.readBits(6);
		return Profile.forInt(i);
	}

	/**
	 * Read possible SBR and PS indication.
	 * See 1.6.6 Signaling of Parametric Stereo (PS)
	 * @param in input stream
	 */
	private void readSyncExtension(BitStream in) {
		int extensionType = in.readBits(11);
		if(extensionType==0x2B7) {
			extProfile = Profile.forInt(in.readBits(5));
			if (extProfile.equals(Profile.AAC_SBR) || extProfile.equals(Profile.ER_BSAC)) {
				sbrPresent = in.readBool();
				if (sbrPresent) {
					outputFrequency = SampleRate.decode(in);
				}
				if(extProfile.equals(Profile.AAC_SBR)) {
					// possible PS indication
					// see: 1.6.6 Signaling of Parametric Stereo (PS)
					if(in.getBitsLeft()>12) {
						extensionType = in.readBits(11);
						if(extensionType== 0x548)
							psPresent = in.readBool();
					}
				} else
				if(extProfile.equals(Profile.ER_BSAC)) {
					extChannelConfiguration = ChannelConfiguration.forInt(in.readBits(4));
				}
			}
		}
	}
}
