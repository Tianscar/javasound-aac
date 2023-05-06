package net.sourceforge.jaad.test;

import net.sourceforge.jaad.MP4Info;
import net.sourceforge.jaad.Main;
import net.sourceforge.jaad.Play;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CLITest {

    @Test
    @DisplayName("MP4 Info")
    public void mp4info() {
        MP4Info.main(new String[] {"-b", "src/test/resources/fbodemo1.m4a"});
    }

    @Test
    @DisplayName("Decode MP4")
    public void decodeMP4() {
        Main.main(new String[] {"-mp4", "src/test/resources/fbodemo1.m4a", "fbodemo1.wav"});
    }

    @Test
    @DisplayName("Decode AAC")
    public void decodeAAC() {
        Main.main(new String[] {"src/test/resources/fbodemo1.aac", "fbodemo1.wav"});
    }

    @Test
    @DisplayName("Play MP4")
    public void playMP4() {
        Play.main(new String[] {"-mp4", "src/test/resources/fbodemo1.m4a"});
    }

    @Test
    @DisplayName("Play AAC")
    public void playAAC() {
        Play.main(new String[] {"src/test/resources/fbodemo1.aac"});
    }

}
