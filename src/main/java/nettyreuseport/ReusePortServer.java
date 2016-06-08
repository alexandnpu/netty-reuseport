package nettyreuseport;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ReusePortServer {
  private final int port;
  private List<Channel> bindingChannels = new LinkedList<>();

  public ReusePortServer(int port) {
    this.port = port;
  }

  private void initSignals() {
    Signal.handle(new Signal("BUS"), new SignalHandler() {
      @Override public void handle(Signal signal) {
        System.out.println("signal arrived");
        closeSocket();
      }
    });
  }

  synchronized private void closeSocket() {
    for (Channel channel : bindingChannels) {
      if (channel instanceof io.netty.channel.epoll.EpollServerSocketChannel) {
        try {
          ((EpollServerSocketChannel)channel).fd().close();
          log("Channel " + channel.toString() + " socket has been closed");
        } catch (IOException e) {
          log("Exception while close channel " + channel.toString());
        }
      }
    }
  }

  synchronized private void registerChannel(Channel channel) {
    bindingChannels.add(channel);
    log("Channel added " + channel.toString());
  }

  synchronized private void unregisterChannel(Channel channel) {
    bindingChannels.remove(channel);
    log("Channel removed " + channel.toString());
  }

  public void start() throws Exception {
    initSignals();

    EventLoopGroup group = new EpollEventLoopGroup();
    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(group)
              .channel(EpollServerSocketChannel.class)
              .option(EpollChannelOption.SO_REUSEPORT, true)
              .localAddress(new InetSocketAddress(port))
              .childHandler(new ChannelInitializer<SocketChannel>(){
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                  ch.pipeline().addLast(new ReusePortHandler());
                }
              });

      ChannelFuture f = b.bind().sync();
      log(String.format("%s started and listen on %s", ReusePortServer.class.getName(), f.channel().localAddress()));
      registerChannel(f.channel());
      f.channel().closeFuture().sync();
    } finally {
      group.shutdownGracefully().sync();
      log("server shutdown");
    }
  }

  private final static SimpleDateFormat datefmt = new SimpleDateFormat("HH:mm:ss ");

  public static void log(final String msg) {
    System.out.print(datefmt.format(new Date()));
    System.out.println(msg);
    System.out.flush();
  }

  public static void main(final String[] args) throws Exception {
    int port = 12355;
    new ReusePortServer(port).start();
  }
}
