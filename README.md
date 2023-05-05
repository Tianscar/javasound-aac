# Java AAC Decoder
This is a fork of [DV8FromTheWorld/JAADec](https://github.com/DV8FromTheWorld/JAADec/), containing bug fixes mainly from [these forks](https://github.com/DV8FromTheWorld/JAADec/forks).

JAAD is an AAC decoder and MP4 demultiplexer library written completely in Java. It uses no native libraries, is platform-independent and portable. It can read MP4 container from almost every input-stream (files, network sockets etc.) and decode AAC-LC (Low Complexity) and HE-AAC (High Efficiency/AAC+).

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
    implementation 'com.tianscar.javasound:jaad:0.8.9'
}
```

## Usage
[Examples](/src/test/java/net/sourceforge/jaad/test/)

## License
[Public Domain](/LICENSE)  
[audios for test](/src/test/resources) originally created by [ProHonor](https://github.com/Aislandz), authorized [me](https://github.com/Tianscar) to use. 2023 (c) ProHonor, all rights reserved.