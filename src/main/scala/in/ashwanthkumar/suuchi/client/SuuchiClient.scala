package in.ashwanthkumar.suuchi.client

import java.lang.{String => JString}
import java.util.concurrent.TimeUnit

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.rpc.SuuchiRPC.{GetRequest, PutRequest}
import in.ashwanthkumar.suuchi.rpc.{SuuchiPutGrpc, SuuchiReadGrpc}
import io.grpc.netty.NettyChannelBuilder
import org.slf4j.LoggerFactory

class SuuchiClient(host: String, port: Int) {
  private val log = LoggerFactory.getLogger(getClass)

  private val channel = NettyChannelBuilder.forAddress(host, port)
    .usePlaintext(true)
    .build()

  private val writeStub = SuuchiPutGrpc.newBlockingStub(channel)
  private val readStub = SuuchiReadGrpc.newBlockingStub(channel)

  def shutdown() = {
    channel.awaitTermination(5, TimeUnit.SECONDS)
  }

  def put(key: Array[Byte], value: Array[Byte]): Boolean = {
    log.info(s"Doing Put with key=${new String(key)} value=${new String(value)}")
    val request = PutRequest.newBuilder()
      .setKey(ByteString.copyFrom(key))
      .setValue(ByteString.copyFrom(value))
      .build()

    writeStub.put(request).getStatus
  }

  def get(key: Array[Byte]): Option[Array[Byte]] = {
    log.info("Doing Get with key={}", new String(key))
    val request = GetRequest.newBuilder()
      .setKey(ByteString.copyFrom(key))
      .build()

    val response = readStub.get(request)
    if (response.getValue.isEmpty) {
      None
    } else {
      Some(response.getKey.concat(response.getValue).toByteArray)
    }
  }
}

object SuuchiClient extends App {
  private val log = LoggerFactory.getLogger(getClass)
  val client = new SuuchiClient("localhost", 5051)

  val status = client.put(Array(59.toByte), Array(65.toByte))
  log.info("Put Status={}", status)

  val value = client.get(Array(59.toByte))
  log.info("Got value={}", new String(value.get))

  client.shutdown()
}