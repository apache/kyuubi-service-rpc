package org.apache.kyuubi.grpc;

import io.grpc.stub.StreamObserver;
import org.apache.kyuubi.grpc.jdbc.DirectStatusResp;
import org.apache.kyuubi.grpc.jdbc.GetCatalogsReq;
import org.apache.kyuubi.grpc.jdbc.JdbcGrpc.JdbcImplBase;

import java.io.IOException;

import static org.apache.kyuubi.grpc.GrpcUtils.OK;

public class DummyJdbcService extends JdbcImplBase {


  public DummyJdbcService() throws IOException {
  }



  @Override
  public void getCatalogs(GetCatalogsReq req, StreamObserver<DirectStatusResp> respOb) {
    DirectStatusResp.Builder builder = DirectStatusResp.newBuilder();
    try {
      if (req.getConnectionId().isEmpty()) {
        throw new IllegalArgumentException("Connection Id can not be empty");
      } else {
        builder.setIdentifier("apache kyuubi").setStatus(OK);
      }
    } catch (Exception e) {
      Status status = Status.newBuilder()
        .setStatusCode(StatusCode.ERROR)
        .setSqlState("2E000")
        .setErrorMessage("invalid connection id" + req.getConnectionId())
        .build();
      builder.setStatus(status);
    }
    respOb.onNext(builder.build());
    respOb.onCompleted();
  }
}
