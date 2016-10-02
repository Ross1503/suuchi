package in.ashwanthkumar.suuchi.rpc

import com.google.protobuf.ByteString
import in.ashwanthkumar.suuchi.membership.MemberAddress
import in.ashwanthkumar.suuchi.partitioner.ConsistentHashPartitioner
import org.slf4j.LoggerFactory

trait RoutingStrategy {
  /**
   * Decides if the incoming message should be forwarded or handled by the current node itself.
   *
   * @tparam ReqT Type of the input Message
   * @return  Some(MemberAddress) - if the request is meant to be forwarded
   *          <p> None - if the request can be handled by the current node itself
   */
  def route[ReqT]: PartialFunction[ReqT, Option[MemberAddress]]
}
object RoutingStrategy {
  type WithKey = {def getKey: ByteString}
}

/**
 * Consistently forward the requests to a node - useful in tests or while debugging
 * @param memberAddress
 */
class AlwaysRouteTo(memberAddress: MemberAddress) extends RoutingStrategy {
  private val log = LoggerFactory.getLogger(getClass)
  /**
   * @inheritdoc
   */
  override def route[ReqT]: PartialFunction[ReqT, Option[MemberAddress]] = {
    case msg: RoutingStrategy.WithKey =>
      Some(memberAddress)
  }
}

class ConsistentHashingRouter(partitioner: ConsistentHashPartitioner) extends RoutingStrategy {
  /**
   * Uses a ConsistentHash based Partitioner to find the right node for the incoming message.
   *
   * @tparam ReqT Type of the input Message
   * @return Some(MemberAddress) - if the request is meant to be forwarded
   *         <p> None - if the request can be handled by the current node itself
   */
  override def route[ReqT]: PartialFunction[ReqT, Option[MemberAddress]] = {
    case msg: RoutingStrategy.WithKey => partitioner.find(msg.getKey.toByteArray).map(vnode => MemberAddress(vnode.node.host, 0)).headOption
  }
}