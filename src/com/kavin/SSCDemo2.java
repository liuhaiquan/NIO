package com.kavin;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
/**

 */
public class SSCDemo2 {
    public static void main(String[] args) throws Exception {
        // 0.准备选择器
        Selector selc = Selector.open();
        // 1.创建ssc
        ServerSocketChannel ssc = ServerSocketChannel.open();
        // 2.开启非阻塞模式
        ssc.configureBlocking(false);
        //3.指定监听9988端口
        ssc.socket().bind(new InetSocketAddress(9988));

        //4.注册到选择器中，关注accept操作
        ssc.register(selc, SelectionKey.OP_ACCEPT);

        //5.死循环，重复执行select操作，来处理选择器上就绪的键
        while (true) {
            //选择就绪的键，如果没有就绪的键，则会此行代码阻塞，直到有就绪的键可以被处理，阻塞才会放开
            selc.select();
            //获取选择到的键，由于可能select时同时又多个键就绪，所以返回的是一个集合
            Set<SelectionKey> set = selc.selectedKeys();
            //迭代处理集合中所有的已经就绪的键
            Iterator<SelectionKey> it = set.iterator();
            while (it.hasNext()) {
                //得到当前遍历到的就绪键
                SelectionKey sk = it.next();

                //如果是一个Accept的键
                if (sk.isAcceptable()) {
                    //获取当前键对应的通道
                    ServerSocketChannel sscx = (ServerSocketChannel) sk.channel();
                    //完成accept操作，获取代表客户端通道的SocketChannle对象
                    SocketChannel sc = sscx.accept();
                    //设置为非阻塞模式
                    sc.configureBlocking(false);
                    //向选择器中注册，关注READ事件
                    sc.register(selc, SelectionKey.OP_READ);
                }
                if (sk.isConnectable()) {

                }

                //如果是READ键
                if (sk.isReadable()) {
                    //获取当前键对应的通道，执行读取操作
                    SocketChannel scx = (SocketChannel) sk.channel();
                    //创建缓冲区
                    ByteBuffer tempBuf = ByteBuffer.allocate(1);
                    String head = "";
                    while(!head.endsWith("\r\n")){
                        scx.read(tempBuf);
                        tempBuf.flip();
                        head = head + new String(tempBuf.array());
                        tempBuf.clear();
                    }

                    int len = Integer.parseInt(head.substring(0,head.length()-2));

                    ByteBuffer buf = ByteBuffer.allocate(len);
                    //读取数据到缓冲区，由于是读取操作是非阻塞的，要自己判断边界条件
                    while(buf.hasRemaining()){
                        scx.read(buf);
                    }
                    //读取完成转换为字符串进行打印
                    String str = new String(buf.array());
                    System.out.println(str);
                }
                if (sk.isWritable()) {

                }

                //清除迭代器，防止重复处理
                it.remove();
            }
        }
    }
}
