package io.github.kimmking.gateway.outbound.okhttp;

import java.io.IOException;
import java.util.concurrent.*;

import org.apache.http.util.EntityUtils;

import io.github.kimmking.gateway.outbound.IHttpOutboundHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class OkhttpOutboundHandler implements IHttpOutboundHandler {

  private String backendUrl;
  private OkHttpClient httpClient;

  public OkhttpOutboundHandler(String backendUrl) {
    this.backendUrl = backendUrl.endsWith("/") ? backendUrl.substring(0, backendUrl.length() - 1) : backendUrl;
    int cores = Runtime.getRuntime().availableProcessors() * 2;
    long keepAliveTime = 1000;
    ConnectionPool connectionPool = new ConnectionPool(cores, keepAliveTime, TimeUnit.MILLISECONDS);
    httpClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS).retryOnConnectionFailure(false).connectionPool(connectionPool).build();
  }

  @Override
  public void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx) {
    fullRequest.headers();
    final String url = this.backendUrl + fullRequest.uri();
    fetchGet(fullRequest, ctx, url);
  }

  private void fetchGet(final FullHttpRequest inbound, final ChannelHandlerContext ctx, final String url) {
    Request request = new Request.Builder().get().url(url).build();
    System.out.println(inbound.headers().get("CUSTOM"));
    httpClient.newCall(request).enqueue(new Callback() {

      @Override
      public void onFailure(Call call, IOException e) {
        e.printStackTrace();
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        try {
          handleResponse(inbound, ctx, response);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }

  private void handleResponse(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx, final Response endpointResponse) throws Exception {
    FullHttpResponse response = null;
    try {
        byte[] body = endpointResponse.body().byteStream().readAllBytes();
        response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(body));
        response.headers().set("Content-Type", "application/json");
        response.headers().setInt("Content-Length", Integer.parseInt(endpointResponse.header("Content-Length")));
    } catch (Exception e) {
        e.printStackTrace();
        response = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
        exceptionCaught(ctx, e);
    } finally {
        if (fullRequest != null) {
            if (!HttpUtil.isKeepAlive(fullRequest)) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                ctx.write(response);
            }
        }
        ctx.flush();
    }
    
}

}
