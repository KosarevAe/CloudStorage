package ru.geekbrains.netty.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import ru.geekbrains.netty.common.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;


public class ServerMainHandler extends ChannelInboundHandlerAdapter {


    private String nick;

    ServerMainHandler(String userId) {
        this.nick = userId;
    }

    public void CannelActive(ChannelHandlerContext ctx) {
        System.out.println("Подключился клиент" + nick);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
        try {
           ctx.writeAndFlush(new AuthMessage());
           /* if (message.equals(null)) {
                return;
            }*/
            if (message instanceof DownloadMessage) {
                DownloadMessage downloadRequest = (DownloadMessage) message;
                if (Files.exists(Paths.get("server_" + nick + "/" + downloadRequest.getFilename()))) {
                    FileMessage fileMessage = new FileMessage(Paths.get("server_" + nick +
                            "/" + downloadRequest.getFilename()));
                    ctx.writeAndFlush(fileMessage);
                }
            }
            if (message instanceof DeleteMessage) {
                DeleteMessage deleteRequest = (DeleteMessage) message;
                Files.delete(Paths.get("server_" + nick + "/" + deleteRequest.getFilename()));
                refreshServerListVew(ctx);
            }
            if (message instanceof FileMessage) {
                FileMessage fileMessage = (FileMessage) message;
                Files.write(Paths.get("server_" + nick + "/" + fileMessage.getFilename()),
                        fileMessage.getData(), StandardOpenOption.CREATE);
                refreshServerListVew(ctx);
            }
            if (message instanceof RefreshServerFileListMessage){
                refreshServerListVew(ctx);
            }

        } finally {
            ReferenceCountUtil.release(message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void refreshServerListVew(ChannelHandlerContext ctx) {
        try {
            ArrayList<String> serverFileList = new ArrayList<>();
            Files.list(Paths.get("server_" + nick + "/")).map(p -> p.getFileName().toString()).forEach(serverFileList::add);
            ctx.writeAndFlush(new RefreshServerFileListMessage(serverFileList));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
