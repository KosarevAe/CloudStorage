package ru.geekbrains.netty.common;

public class DownloadMessage extends AbstractMessage {
    private String filename;

    public DownloadMessage(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}