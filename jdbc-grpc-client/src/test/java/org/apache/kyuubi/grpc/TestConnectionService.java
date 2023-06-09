package org.apache.kyuubi.grpc;


import io.grpc.stub.StreamObserver;
import org.apache.kyuubi.grpc.common.ConnectionHandle;
import org.apache.kyuubi.grpc.jdbc.*;
import org.apache.kyuubi.grpc.jdbc.connection.ConnectionGrpc;
import org.apache.kyuubi.grpc.jdbc.connection.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.apache.kyuubi.grpc.GrpcUtils.OK;

public class TestConnectionService extends ConnectionGrpc.ConnectionImplBase {
  private final Path _tempDir = Files.createTempDirectory(getClass().getSimpleName());
  public String defaultCatalogName = _tempDir.getFileName().toString().toUpperCase();
  private final String jdbcUrl = "jdbc:h2:" + _tempDir + ";MODE=DB2;user=testUser;password=testPass";
  private Map<ConnectionHandle, Connection> connections = new HashMap<>();
  private Executor executor = Executors.newSingleThreadExecutor();
  private final Map<Savepoint, java.sql.Savepoint> savepoints = new HashMap<>();
  public Connection getConnection(ConnectionHandle connectionId, Properties properties) throws SQLException {
    if (connections.containsKey(connectionId)) {
      return connections.get(connectionId);
    } else {
      Connection conn = DriverManager.getConnection(jdbcUrl, properties);
      connections.put(connectionId, conn);
      return conn;
    }
  }

  public Connection getConnection(ConnectionHandle connectionId) throws SQLException {
    if (connections.containsKey(connectionId)) {
      return connections.get(connectionId);
    } else {
      Connection conn = DriverManager.getConnection(jdbcUrl, new Properties());
      connections.put(connectionId, conn);
      return conn;
    }
  }

  public TestConnectionService() throws IOException {
  }

  public void stop() {
    for (Connection conn : connections.values()) {
      try {
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    connections.clear();
    connections = null;
    executor = null;
  }

  public Status errorStatus(Exception e) {
    return Status.newBuilder()
      .setSqlState("38808")
      .setErrorMessage(e.getMessage())
      .setStatusCode(StatusCode.ERROR)
      .build();
  }

  public DirectStatusResp error(Exception e) {
    return DirectStatusResp
      .newBuilder()
      .setStatus(errorStatus(e))
      .build();
  }

  public DirectStatusResp ok(String id) {
    return DirectStatusResp
      .newBuilder()
      .setStatus(OK)
      .setIdentifier(id)
      .build();
  }

  @Override
  public void openConnection(OpenConnectionReq req, StreamObserver<DirectStatusResp> respOb) {
    DirectStatusResp.Builder builder = DirectStatusResp.newBuilder();
    try {
      ConnectionHandle connectionId = null;
      Properties properties = new Properties();
      properties.putAll(req.getConfigsMap());
      if (req.getConnectionId() == ConnectionHandle.getDefaultInstance()) {
        connectionId = ConnectionHandle.newBuilder()
          .setId(UUID.randomUUID().toString())
          .build();
      } else {
        connectionId = req.getConnectionId();
      }
      getConnection(connectionId, properties);
      builder.setIdentifier(connectionId.getId());
      builder.putExtraInfo("apache", "kyuubi");
      builder.putExtraInfo("Kyuubi", "Serverless SQL on Lakehouse");
      builder.setStatus(OK);
      respOb.onNext(builder.build());
    } catch (SQLException e) {
      Status status = Status
        .newBuilder()
        .setStatusCode(StatusCode.ERROR)
        .setErrorMessage(e.getMessage())
        .setSqlState("0AS86")
        .build();
      builder.setStatus(status);
      respOb.onNext(builder.build());
    }

    respOb.onCompleted();
  }

  @Override
  public void closeConnection(ConnectionHandle req, StreamObserver<DirectStatusResp> respOb) {
    DirectStatusResp.Builder builder = DirectStatusResp.newBuilder();
    try {
      if (!connections.containsKey(req)) {
        throw new IllegalArgumentException("invalid connection id " + req.getId());
      } else {
        getConnection(req).close();
        builder.setIdentifier(req.getId());
      }
      builder.setStatus(OK);
    } catch (Exception e) {
      Status status = Status.newBuilder()
        .setStatusCode(StatusCode.ERROR)
        .setSqlState("2E000")
        .setErrorMessage(e.getMessage())
        .build();
      builder.setStatus(status);
    }
    respOb.onNext(builder.build());
    respOb.onCompleted();
  }

  @Override
  public void nativeSQL(NativeSQLReq request, StreamObserver<NativeSQLResp> responseObserver) {
    NativeSQLResp.Builder builder = NativeSQLResp.newBuilder();
    try {
      String nativeSQL = getConnection(request.getConnectionId()).nativeSQL(request.getSql());
      NativeSQLResp resp = builder
        .setSql(nativeSQL)
        .setStatus(OK)
        .build();
      responseObserver.onNext(resp);
    } catch (Exception e) {
      Status status = Status.newBuilder()
        .setSqlState("38808")
        .setErrorMessage(e.getMessage())
        .setStatusCode(StatusCode.ERROR)
        .build();
      NativeSQLResp resp = builder.setStatus(status).build();
      responseObserver.onNext(resp);
    }
    responseObserver.onCompleted();
  }

  @Override
  public void setAutoCommit(SetAutoCommitReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection(request.getConnectionId()).setAutoCommit(request.getAutoCommit());
      responseObserver.onNext(ok(request.getConnectionId().getId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getAutoCommit(ConnectionHandle request, StreamObserver<GetAutoCommitResp> responseObserver) {
    GetAutoCommitResp.Builder builder = GetAutoCommitResp.newBuilder();
    try {
      boolean autoCommit = getConnection(request).getAutoCommit();
      GetAutoCommitResp resp = builder
        .setStatus(OK)
        .setAutoCommit(autoCommit)
        .build();
      responseObserver.onNext(resp);
    } catch (SQLException e) {
      responseObserver.onNext(builder.setStatus(errorStatus(e)).build());
    }
    responseObserver.onCompleted();
  }

  @Override
  public void commit(ConnectionHandle request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection(request).commit();
      responseObserver.onNext(ok(request.getId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void rollback(RollbackReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      Connection conn = getConnection(request.getConnectionId());
      Savepoint savepoint = request.getSavepoint();
      if (savepoint == Savepoint.getDefaultInstance()) {
        conn.rollback();
      } else {
        conn.rollback(savepoints.get(savepoint));
      }
      responseObserver.onNext(ok(request.getConnectionId().getId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void setReadOnly(SetReadOnlyReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection(request.getConnectionId()).setReadOnly(request.getReadOnly());
      responseObserver.onNext(ok(request.getConnectionId().getId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void isReadOnly(ConnectionHandle request, StreamObserver<IsReadOnlyResp> responseObserver) {
    IsReadOnlyResp.Builder builder = IsReadOnlyResp.newBuilder();
    try {
      IsReadOnlyResp resp = builder
        .setStatus(OK)
        .setReadOnly(getConnection(request).isReadOnly())
        .build();
      responseObserver.onNext(resp);
    } catch (SQLException e) {
      IsReadOnlyResp resp = builder
        .setStatus(errorStatus(e))
        .build();
      responseObserver.onNext(resp);
    }
    responseObserver.onCompleted();
  }

  @Override
  public void setCatalog(SetCatalogReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection(request.getConnectionId()).setCatalog(request.getCatalog());
      responseObserver.onNext(ok(request.getConnectionId().getId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getCatalog(ConnectionHandle request, StreamObserver<GetCatalogResp> responseObserver) {
    GetCatalogResp.Builder builder = GetCatalogResp.newBuilder();
    try {
      String catalog = getConnection(request).getCatalog();
      GetCatalogResp resp = builder
        .setStatus(OK)
        .setCatalog(catalog)
        .build();
      responseObserver.onNext(resp);
    } catch (SQLException e) {
      GetCatalogResp resp = GetCatalogResp.newBuilder()
        .setStatus(errorStatus(e))
        .build();
      responseObserver.onNext(resp);
    }
    responseObserver.onCompleted();
  }

  @Override
  public void setTransactionIsolation(SetTransactionIsolationReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection(request.getConnectionId()).setTransactionIsolation(request.getLevel());
      responseObserver.onNext(ok(request.getConnectionId().getId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getTransactionIsolation(ConnectionHandle request, StreamObserver<GetTransactionIsolationResp> responseObserver) {
    GetTransactionIsolationResp.Builder builder = GetTransactionIsolationResp.newBuilder();
    try {
      int level = getConnection(request).getTransactionIsolation();
      GetTransactionIsolationResp resp = builder
        .setStatus(OK)
        .setLevel(level)
        .build();
      responseObserver.onNext(resp);
    } catch (SQLException e) {
      GetTransactionIsolationResp resp = builder
        .setStatus(errorStatus(e))
        .build();
      responseObserver.onNext(resp);
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getWarnings(ConnectionHandle request, StreamObserver<GetWarningsResp> responseObserver) {
    GetWarningsResp.Builder builder = GetWarningsResp.newBuilder();
    try {
      SQLWarning warnings = GrpcUtils.toProto(getConnection(request).getWarnings());

      if (warnings != null) {
        builder.setWarnings(warnings);
      }
      responseObserver.onNext(builder.setStatus(OK).build());
    } catch (SQLException e) {
      responseObserver.onNext(builder.setStatus(errorStatus(e)).build());
    }
    responseObserver.onCompleted();
  }

  @Override
  public void clearWarnings(ConnectionHandle request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection(request).clearWarnings();
      responseObserver.onNext(ok(request.getId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }


  @Override
  public void setTypeMap(SetTypeMapReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      Map<String, Class<?>> classMap = new HashMap<String, Class<?>>();
      for (Map.Entry<String, String> entry : request.getTypeToClassMap().entrySet()) {
        classMap.put(entry.getKey(), Class.forName(entry.getValue()));
      }
      getConnection(request.getConnectionId()).setTypeMap(classMap);
      responseObserver.onNext(ok(request.getConnectionId().getId()));
    } catch (Exception e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getTypeMap(ConnectionHandle request, StreamObserver<GetTypeMapResp> responseObserver) {
    GetTypeMapResp.Builder builder = GetTypeMapResp.newBuilder();
    try {
      Map<String, Class<?>> typeMap = getConnection(request).getTypeMap();
      if (typeMap != null) {
        Map<String, String> typeToClassMap = new HashMap<String, String>();
        for (Map.Entry<String, Class<?>> entry : typeMap.entrySet()) {
          typeToClassMap.put(entry.getKey(), entry.getValue().getName());
        }
        builder.putAllTypeToClass(typeToClassMap);
      }
      responseObserver.onNext(builder.setStatus(OK).build());
    } catch (SQLException e) {
      responseObserver.onNext(builder.setStatus(errorStatus(e)).build());
    }
    responseObserver.onCompleted();

  }

  @Override
  public void setSchema(SetSchemaReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection(request.getConnectionId()).setSchema(request.getSchema());
      responseObserver.onNext(ok(request.getConnectionId().getId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getSchema(ConnectionHandle request, StreamObserver<GetSchemaResp> responseObserver) {
    GetSchemaResp.Builder builder = GetSchemaResp.newBuilder();
    try {
      String schema = getConnection(request).getSchema();
      // schema can be null, can you verify this?
      GetSchemaResp resp = builder
        .setStatus(OK)
        .setSchema(schema)
        .build();
      responseObserver.onNext(resp);
    } catch (SQLException e) {
      GetSchemaResp resp = builder
        .setStatus(errorStatus(e))
        .build();
      responseObserver.onNext(resp);
    }
    responseObserver.onCompleted();
  }

  @Override
  public void setNetworkTimeout(SetNetworkTimeoutReq request, StreamObserver<DirectStatusResp> responseObserver) {
     try {
        getConnection(request.getConnectionId()).setNetworkTimeout(executor, request.getMilliseconds());
        responseObserver.onNext(ok(request.getConnectionId().getId()));
      } catch (SQLException e) {
        responseObserver.onNext(error(e));
      }
      responseObserver.onCompleted();
  }

  @Override
  public void getNetworkTimeout(ConnectionHandle request, StreamObserver<GetNetworkTimeoutResp> responseObserver) {
    GetNetworkTimeoutResp.Builder builder = GetNetworkTimeoutResp.newBuilder();
    try {
      int timeout = getConnection(request).getNetworkTimeout();
      GetNetworkTimeoutResp resp = builder
        .setStatus(OK)
        .setMilliseconds(timeout)
        .build();
      responseObserver.onNext(resp);
    } catch (SQLException e) {
      GetNetworkTimeoutResp resp = builder
        .setStatus(errorStatus(e))
        .build();
      responseObserver.onNext(resp);
    }
    responseObserver.onCompleted();
  }

  @Override
  public void isValid(IsValidReq request, StreamObserver<IsValidResp> responseObserver) {
    IsValidResp.Builder builder = IsValidResp.newBuilder();
    try {
      boolean valid = getConnection(request.getConnectionId()).isValid(request.getTimeout());
      IsValidResp resp = builder
        .setStatus(OK)
        .setValid(valid)
        .build();
      responseObserver.onNext(resp);
    } catch (SQLException e) {
      IsValidResp resp = builder
        .setStatus(errorStatus(e))
        .build();
      responseObserver.onNext(resp);
    }
  }

  @Override
  public void abortConnection(ConnectionHandle request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection(request).abort(executor);
      responseObserver.onNext(ok(request.getId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void setClientInfo(SetClientInfoReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      Properties properties = new Properties();
      properties.putAll(request.getConfigsMap());
      getConnection(request.getConnectionId()).setClientInfo(properties);
      responseObserver.onNext(ok(request.getConnectionId().getId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getClientInfo(ConnectionHandle request, StreamObserver<GetClientInfoResp> responseObserver) {
    GetClientInfoResp.Builder builder = GetClientInfoResp.newBuilder();
    try {
      Properties properties = getConnection(request).getClientInfo();
      // use a loop to put all properties into the builder
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        builder.putConfigs(entry.getKey().toString(), entry.getValue().toString());
      }
      responseObserver.onNext(builder.setStatus(OK).build());
    } catch (SQLException e) {
      responseObserver.onNext(builder.setStatus(errorStatus(e)).build());
    }
    responseObserver.onCompleted();
  }
}
