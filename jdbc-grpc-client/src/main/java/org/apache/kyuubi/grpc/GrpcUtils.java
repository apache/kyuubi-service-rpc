package org.apache.kyuubi.grpc;

public class GrpcUtils {
  final static Status OK = Status.newBuilder().setStatusCode(StatusCode.OK).setSqlState("00000").build();
}
