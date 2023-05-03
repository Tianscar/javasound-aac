import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SPIReadMP4Test {

    public static void main(String[] args) {
        try {
            AudioInputStream in = AudioSystem.getAudioInputStream(new File("src/test/resources/fbodemo1.m4a"));
            if (in != null) {
                AudioFormat baseFormat = in.getFormat();

                AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(),
                        16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);

                AudioInputStream dataIn = AudioSystem.getAudioInputStream(targetFormat, in);

                byte[] buffer = new byte[4096];

                // get a line from a mixer in the system with the wanted format
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

                if (line != null) {
                    line.open();

                    line.start();
                    int nBytesRead = 0;
                    while (nBytesRead != -1) {
                        nBytesRead = dataIn.read(buffer, 0, buffer.length);
                        if (nBytesRead != -1) {
                            line.write(buffer, 0, nBytesRead);
                        }
                    }

                    line.drain();
                    line.stop();
                    line.close();

                    dataIn.close();
                }

                in.close();
                // playback finished
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

}
