package io.github.kimmking.gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

public class CustomHeaderFilter extends ChannelInboundHandlerAdapter implements HttpRequestFilter {
    private String header;
    private String value;  

    public CustomHeaderFilter(String header, String value) {

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            FullHttpRequest fullRequest = (FullHttpRequest) msg;
            filter(fullRequest, ctx);
            super.channelRead(ctx, msg);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void filter(FullHttpRequest fullRequest, ChannelHandlerContext ctx) {
        fullRequest.headers().set(header, value);
    }
  
}
