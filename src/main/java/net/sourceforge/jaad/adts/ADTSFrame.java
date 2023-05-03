package net.sourceforge.jaad.adts;

import net.sourceforge.jaad.aac.AudioDecoderInfo;
import net.sourceforge.jaad.aac.ChannelConfiguration;
import net.sourceforge.jaad.aac.Profile;
import net.sourceforge.jaad.aac.SampleFrequency;

import java.io.DataInputStream;
import java.io.IOException;

class ADTSFrame implements AudioDecoderInfo {

	//fixed
	private boolean id, protectionAbsent, privateBit, copy, home;
	private int layer, profile, sampleFrequency, channelConfiguration;
	//variable
	private boolean copyrightIDBit, copyrightIDStart;
	private int frameLength, adtsBufferFullness, rawDataBlockCount;
	//error check
	private int[] rawDataBlockPosition;
	private int crcCheck;
	//decoder specific info
	private byte[] info;

	ADTSFrame(DataInputStream in) throws IOException {
		readHeader(in);

		if(!protectionAbsent)
			crcCheck = in.readUnsignedShort();

		if(rawDataBlockCount==0) {
			//raw_data_block();
		}
		else {
			//header error check
			if(!protectionAbsent) {
				rawDataBlockPosition = new int[rawDataBlockCount];
				for(int i = 0; i<rawDataBlockCount; i++) {
					rawDataBlockPosition[i] = in.readUnsignedShort();
				}
				crcCheck = in.readUnsignedShort();
			}
			//raw data blocks
			for(int i = 0; i<rawDataBlockCount; i++) {
				//raw_data_block();
				if(!protectionAbsent)
					crcCheck = in.readUnsignedShort();
			}
		}
	}

	private void readHeader(DataInputStream in) throws IOException {
		//fixed header:
		//1 bit ID, 2 bits layer, 1 bit protection absent
		int i = in.read();
		id = ((i>>3)&0x1)==1;
		layer = (i>>1)&0x3;
		protectionAbsent = (i&0x1)==1;

		//2 bits profile, 4 bits sample frequency, 1 bit private bit
		i = in.read();
		profile = ((i>>6)&0x3)+1;
		sampleFrequency = (i>>2)&0xF;
		privateBit = ((i>>1)&0x1)==1;

		//3 bits channel configuration, 1 bit copy, 1 bit home
		i = (i<<8)|in.read();
		channelConfiguration = ((i>>6)&0x7);
		copy = ((i>>5)&0x1)==1;
		home = ((i>>4)&0x1)==1;
		//int emphasis = in.readBits(2);

		//variable header:
		//1 bit copyrightIDBit, 1 bit copyrightIDStart, 13 bits frame length,
		//11 bits adtsBufferFullness, 2 bits rawDataBlockCount
		copyrightIDBit = ((i>>3)&0x1)==1;
		copyrightIDStart = ((i>>2)&0x1)==1;
		i = (i<<16)|in.readUnsignedShort();
		frameLength = (i>>5)&0x1FFF;
		i = (i<<8)|in.read();
		adtsBufferFullness = (i>>2)&0x7FF;
		rawDataBlockCount = i&0x3;
	}

	int getFrameLength() {
		return frameLength-(protectionAbsent ? 7 : 9);
	}

	public Profile getProfile() {
		return Profile.forInt(profile);
	}

	public SampleFrequency getSampleFrequency() {
		return SampleFrequency.forInt(sampleFrequency);
	}

	public ChannelConfiguration getChannelConfiguration() {
		return ChannelConfiguration.forInt(channelConfiguration);
	}
}
