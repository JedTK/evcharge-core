package com.evcharge;

import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.ThreadUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 使用Netty框架搭建TCP服务器
 */
public class NettyTcpServer {

    //region 属性
    /**
     * TCP服务名，用于服务器区分多个TCP服务
     */
    public String name;
    /**
     * 端口号
     */
    private int port;
    /**
     * 读空闲时间，如果在指定时间内没有收到客户端数据，则触发空闲处理
     */
    private long readerIdleTime = 0;
    /**
     * 写空闲时间,如果在指定时间内没有向客户端发送数据，则触发空闲处理
     */
    private long writerIdleTime = 0;
    /**
     * 读写都空闲时间，如果在指定时间内既没有读也没有写，则触发空闲处理
     */
    private long allIdleTime = 0;
    /**
     * 通道处理程序
     */
    private ChannelHandler channelHandler;
    /**
     * 解码器
     */
    private Supplier<ChannelHandler> decoderSupplier;
    /**
     * 编码器
     */
    private Supplier<ChannelHandler> encoderSupplier;


    public NettyTcpServer setName(String name) {
        this.name = name;
        return this;
    }

    public NettyTcpServer setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * 通道处理程（链接、接收、发送、关闭、空闲等等处理程序）
     *
     * @param channelHandler
     * @return
     */
    public NettyTcpServer setChannelHandler(ChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
        return this;
    }

    /**
     * 读空闲时间，如果在指定时间内没有收到客户端数据，则触发空闲处理
     *
     * @param readerIdleTime
     * @return
     */
    public NettyTcpServer setReaderIdleTime(long readerIdleTime) {
        this.readerIdleTime = readerIdleTime;
        return this;
    }

    /**
     * 写空闲时间,如果在指定时间内没有向客户端发送数据，则触发空闲处理
     *
     * @param writerIdleTime
     * @return
     */
    public NettyTcpServer setWriterIdleTime(long writerIdleTime) {
        this.writerIdleTime = writerIdleTime;
        return this;
    }

    /**
     * 读写都空闲时间，如果在指定时间内既没有读也没有写，则触发空闲处理
     *
     * @param allIdleTime
     * @return
     */
    public NettyTcpServer setAllIdleTime(long allIdleTime) {
        this.allIdleTime = allIdleTime;
        return this;
    }

    /**
     * 设置解码器
     *
     * @param decoderSupplier
     * @return
     */
    public NettyTcpServer setDecoder(Supplier<ChannelHandler> decoderSupplier) {
        this.decoderSupplier = decoderSupplier;
        return this;
    }

    /**
     * 设置编码器
     *
     * @param encoderSupplier
     * @return
     */
    public NettyTcpServer setEncoder(Supplier<ChannelHandler> encoderSupplier) {
        this.encoderSupplier = encoderSupplier;
        return this;
    }
    //endregion

    /**
     * 获得实例
     *
     * @return
     */
    public static NettyTcpServer getInstance() {
        return new NettyTcpServer();
    }

    /**
     * 多线程启动
     */
    public void run() {
        ThreadUtil.getInstance().execute(String.format("[%s] Netty.TCP服务 - 准备中 - 端口：%s", this.name, this.port), this::start);
    }

    /**
     * 开启
     */
    private void start() {
        try {
            EventLoopGroup bossGroup = new NioEventLoopGroup(); // 用于接受连接
            EventLoopGroup workerGroup = new NioEventLoopGroup(); // 用于处理已接受的连接
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class) // 使用 NIO 通信
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                // 为每个SocketChannel添加日志处理器
//                                ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                                // 如果自定义解码器为空，则使用默认的 StringDecoder 作为解码器
//                                ch.pipeline().addLast(Objects.requireNonNullElseGet(decoder, StringDecoder::new));
//                                ch.pipeline().addLast(decoderSupplier != null ? decoderSupplier.get() : new StringDecoder());
                                // 如果自定义编码器为空，则使用默认的 StringEncoder 作为编码器
//                                ch.pipeline().addLast(Objects.requireNonNullElseGet(encoder, StringEncoder::new));
//                                ch.pipeline().addLast(encoderSupplier != null ? encoderSupplier.get() : new StringEncoder());

                                if (decoderSupplier != null) ch.pipeline().addLast(decoderSupplier.get());
                                if (encoderSupplier != null) ch.pipeline().addLast(encoderSupplier.get());

                                if (readerIdleTime > 0 || writerIdleTime > 0 || allIdleTime > 0) {
                                    ch.pipeline().addLast(new IdleStateHandler(
                                            readerIdleTime // 读空闲时间，如果在指定时间内没有收到客户端数据，则触发
                                            , writerIdleTime // 写空闲时间,如果在指定时间内没有向客户端发送数据，则触发
                                            , allIdleTime // 读写都空闲时间，如果在指定时间内既没有读也没有写，则触发
                                            , TimeUnit.MILLISECONDS
                                    ));
                                }
                                // 自定义处理器，必须要放最后
                                ch.pipeline().addLast(channelHandler);
                            }
                        });

                // 绑定端口并启动服务器
                ChannelFuture f = b.bind(port).sync();

                LogsUtil.info(this.getClass().getSimpleName(), "[%s]Netty.TCP服务 - 启动成功 - 端口：%s", this.name, this.port);

                // 等待服务器 socket 关闭
                f.channel().closeFuture().sync();
            } finally {
                // 优雅关闭
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        } catch (InterruptedException e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "NettyTcpServer启动错误");
        }
    }
}
