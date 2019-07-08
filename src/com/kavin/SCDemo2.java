package com.kavin;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SCDemo2 {
    public static void main(String[] args) throws Exception {
        //0.获取选择器
        Selector selc = Selector.open();

        //1.创建SocketChannel
        SocketChannel sc = SocketChannel.open();
        //2.设置为非阻塞模式
        sc.configureBlocking(false);
        //3.注册到选择器中，关注CONNECT
        sc.register(selc, SelectionKey.OP_CONNECT);
        //4.发起Connect操作
        sc.connect(new InetSocketAddress("127.0.0.1",9988));

        //5.循环从选择器重获取就绪的键处理
        while(true){
            //进行选择操作，如果没有任何键就绪就会阻塞，直到有任何键就绪才会放开阻塞
            selc.select();
            //选择出已经就绪的键组成的集合
            Set<SelectionKey> set = selc.selectedKeys();
            //循环处理已经就绪的键
            Iterator<SelectionKey> it = set.iterator();
            while(it.hasNext()){
                //获取当前遍历到的就绪的键
                SelectionKey key = it.next();
                //如果是一个Connect键
                if(key.isConnectable()){
                    //获取可以完成连接的键
                    SocketChannel scx = (SocketChannel) key.channel();
                    //完成连接
                    scx.finishConnect();
                    //重新在选择器上进行注册，关注WRITE，要注意重新注册意味着之前的注册被覆盖了
                    scx.register(selc, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                }

                //如果是一个Write键
                if(key.isWritable()){
                    //获取就绪键对应的通道
                    SocketChannel scx = (SocketChannel) key.channel();

                    //准备缓冲区写出数据
                    String data = "abcd";
                    String len = data.length()+"\r\n";

                    ByteBuffer headBuf = ByteBuffer.wrap(len.getBytes());
                    ByteBuffer bodyBuf = ByteBuffer.wrap(data.getBytes());
                    while(bodyBuf.hasRemaining()){
                        scx.write(new ByteBuffer[]{headBuf,bodyBuf});
                    }

                    //删除WRITE注册，防止写出很多次
                    scx.register(selc, key.interestOps() & ~SelectionKey.OP_WRITE);
                }

                //删除已经处理的键，防止重复处理
                it.remove();
            }
        }

    }
}
