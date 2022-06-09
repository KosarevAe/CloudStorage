package ru.geekbrains.netty.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import ru.geekbrains.netty.common.AuthMessage;
import ru.geekbrains.netty.common.RegistrationMessage;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;

public class RegistrationHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {

            if (message instanceof RegistrationMessage) {
                RegistrationMessage registration = (RegistrationMessage) message;
                String nick = DBConnection.getIdByLoginAndPass(registration.login, registration.password);
                if (nick != null) {
                    RegistrationMessage r = new RegistrationMessage("/not_null_userId");
                    ctx.writeAndFlush(r);
                } else {
                    DBConnection.registrationByLoginPassAndNick(registration.login, registration.password, registration.nick);
                    Files.createDirectory(Paths.get("server_" + registration.nick));
                    RegistrationMessage r = new RegistrationMessage("/registration_Ok " + registration.nick);
                    ctx.writeAndFlush(r);
                }
                return;
            }


            ctx.fireChannelRead(message);

    }

    public void exeptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
