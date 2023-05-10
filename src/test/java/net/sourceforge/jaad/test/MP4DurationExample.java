package net.sourceforge.jaad.test;

import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.MP4InputStream;
import net.sourceforge.jaad.mp4.api.Movie;

import java.io.IOException;
import java.io.RandomAccessFile;

public class MP4DurationExample {

    public static void main(String[] args) {
        try {
            MP4Container mp4 = new MP4Container(MP4InputStream.open(new RandomAccessFile("src/test/resources/fbodemo1_aac.m4a", "r")));
            Movie movie = mp4.getMovie();
            System.out.println("MP4 audio duration: " + movie.getDuration());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
