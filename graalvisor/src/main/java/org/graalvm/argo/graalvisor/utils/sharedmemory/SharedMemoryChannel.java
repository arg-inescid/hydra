package org.graalvm.argo.graalvisor.utils.sharedmemory;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public abstract class SharedMemoryChannel {

    protected final FileChannel channel;
    protected final CharBuffer buffer;
    protected final char[] readBuffer = new char[4096];

    protected static final char readyToRead = '<';
    protected static final char readyToWrite = '>';

    public SharedMemoryChannel(Path path) throws IOException {
        this.channel = FileChannel.open(
                path,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE);
        this.buffer = channel.map(MapMode.READ_WRITE, 0, 4096).asCharBuffer();
    }

    public void close() throws IOException {
        this.channel.close();
    }
}
