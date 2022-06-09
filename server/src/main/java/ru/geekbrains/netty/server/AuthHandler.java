package ru.geekbrains.netty.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import ru.geekbrains.netty.common.AuthMessage;
import ru.geekbrains.netty.common.RegistrationMessage;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authOk = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
        try {
           if (authOk ) {
                ctx.fireChannelRead(message);
               return;
           }
           if (message instanceof AuthMessage) {
                AuthMessage authMessage = (AuthMessage) message;
                String nick = DBConnection.getIdByLoginAndPass(authMessage.login, authMessage.password);
                if (nick != null) {
                    authOk = true;
                    ctx.pipeline().addLast(new ServerMainHandler(nick));
                    ctx.writeAndFlush(new AuthMessage("/authOk " + nick));
                } else if(authMessage.message.equals("/connection_close")){
                    authOk = false;
                    ctx.write(new AuthMessage("/null_userId"));
                }else{
                    ctx.write(new AuthMessage("/null_userId"));
                }
            }
        } finally {
          ReferenceCountUtil.release(message);
        }
    }

    public void exeptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
