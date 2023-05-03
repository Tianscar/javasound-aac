package net.sourceforge.jaad.aac;

/**
 * Created by IntelliJ IDEA.
 * User: stueken
 * Date: 24.03.18
 * Time: 15:21
 */
public interface AudioDecoderInfo {

    Profile getProfile();

    SampleFrequency getSampleFrequency();

    ChannelConfiguration getChannelConfiguration();
}
