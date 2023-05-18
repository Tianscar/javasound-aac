# Java Advanced Audio Decoder
This is a fork of [DV8FromTheWorld/JAADec](https://github.com/DV8FromTheWorld/JAADec/), containing bug fixes mainly from [these forks](https://github.com/DV8FromTheWorld/JAADec/forks).

This library is an AAC decoder and MP4 demultiplexer library written completely in Java. It uses no native libraries, is platform-independent and portable. It can read MP4 container from almost every input-stream (files, network sockets etc.) and decode AAC-LC (Low Complexity) and HE-AAC (High Efficiency/AAC+).

## Add the library to your project (gradle)
1. Add the Maven Central repository (if not exist) to your build file:
```groovy
repositories {
    ...
    mavenCentral()
}
```

2. Add the dependency:
```groovy
dependencies {
    ...
    implementation 'com.tianscar.javasound:javasound-aac:0.9.7'
}
```

## Usage
[Tests and Examples](/src/test/java/net/sourceforge/jaad/test/)  
[Command-line interfaces](/src/test/java/net/sourceforge/jaad/)

Note you need to download test audios [here](https://github.com/Tianscar/fbodemo1) and put them to /src/test/resources to run the test code properly!

## License
[Public Domain](/LICENSE)  

### Dependencies
| Library                                                                    | License | Comptime | Runtime |
|----------------------------------------------------------------------------|---------|----------|---------|
| [JavaSound ResLoader SPI](https://github.com/Tianscar/javasound-resloader) | MIT     | Yes      | Yes     |
