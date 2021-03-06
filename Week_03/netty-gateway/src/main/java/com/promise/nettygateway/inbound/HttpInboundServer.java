package com.promise.nettygateway.inbound;

import com.promise.nettygateway.constant.HttpGateWayConstant;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

/**
 * Created by yandex on 2020/11/1.
 */
@Slf4j
public class HttpInboundServer {
    /**
     * TODO 对外访问的 port
     */
    private String proxyPort = System.getProperty("proxyPort","8888");

    public void run() {
        EventLoopGroup boosGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(12);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.option(ChannelOption.SO_BACKLOG, 128) // 建立连接前的握手连接数 Mac/Linux 默认128  win 200
                    .option(ChannelOption.TCP_NODELAY, true) // 高并发下 建议 打开 Nagle 算法优化，将内存拼接在一起
                    .option(ChannelOption.SO_REUSEADDR, true) // 地址复用
                    .option(ChannelOption.SO_RCVBUF, 32 * 1024) //
                    .option(ChannelOption.SO_SNDBUF, 32 * 1024)
                    .option(EpollChannelOption.SO_REUSEPORT, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT); // 让netty 分配 byteBuf 进行管理

            /**
             * 绑定 注册事件
             */
            bootstrap.group(boosGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HttpInboundInitializer());

            Channel ch = bootstrap.bind(Integer.parseInt(proxyPort)).sync().channel();
            log.info("开启 netty http 服务器,监听地址和端口为: http://127.0.0.1:{}/",proxyPort);
            ch.closeFuture().sync();
        } catch (InterruptedException e) {
           log.info("{},{} started at http://localhost:{} start failed...", HttpGateWayConstant.GATEWAY_NAME,
                   HttpGateWayConstant.GATEWAY_VERSION,proxyPort);
           log.error("HttpInboundServer:run error ==> " + e);
        } finally {

            /**
             * 异步 关闭 EventLoop
             */
            Future<?> eventFuture = boosGroup.shutdownGracefully();
            Future<?> workerFuture = workerGroup.shutdownGracefully();

            /**
             * 等待关闭成功
             */
            eventFuture.syncUninterruptibly();
            workerFuture.syncUninterruptibly();

        }
    }

}
