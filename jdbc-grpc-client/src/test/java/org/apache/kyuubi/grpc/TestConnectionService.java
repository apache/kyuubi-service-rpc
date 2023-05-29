package org.apache.kyuubi.grpc;


import io.grpc.stub.StreamObserver;
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.apache.kyuubi.grpc.GrpcUtils.OK;

public class TestConnectionService extends ConnectionGrpc.ConnectionImplBase {

  private final Path _tempDir = Files.createTempDirectory(getClass().getSimpleName());

  public String defaultCatalogName = _tempDir.getFileName().toString().toUpperCase();
  private final String jdbcUrl = "jdbc:h2:" + _tempDir + ";MODE=DB2;user=testUser;password=testPass";
  private Connection _connection = null;

  private final Executor executor = Executors.newSingleThreadExecutor();

  private final Map<Savepoint, java.sql.Savepoint> savepoints = new HashMap<>();

  public void setConnection(Connection _connection) {
    this._connection = _connection;
  }

  public Connection getConnection() throws SQLException {
    if (this._connection == null) {
      Connection conn = DriverManager.getConnection(jdbcUrl);
      setConnection(conn);
      return conn;
    } else {
      return _connection;
    }
  }

  public TestConnectionService() throws IOException {
  }

  private Status errorStatus(Exception e) {
    return Status.newBuilder()
      .setSqlState("38808")
      .setErrorMessage(e.getMessage())
      .setStatusCode(StatusCode.ERROR)
      .build();
  }

  private DirectStatusResp error(Exception e) {
    return DirectStatusResp
      .newBuilder()
      .setStatus(errorStatus(e))
      .build();
  }

  private DirectStatusResp ok(String id) {
    return DirectStatusResp
      .newBuilder()
      .setStatus(OK)
      .setIdentifier(id)
      .build();
  }

  @Override
  public void openConnection(OpenConnectionReq req, StreamObserver<DirectStatusResp> respOb) {
    DirectStatusResp.Builder builder = DirectStatusResp.newBuilder();
    if (req.getConnectionId().isEmpty()) {
      builder.setIdentifier("hello, kyuubi");
    } else {
      builder.setIdentifier("hello, " + req.getConnectionId());
    }
    try {
      Properties properties = new Properties();
      properties.putAll(req.getConfigsMap());
      setConnection(
        DriverManager.getConnection(jdbcUrl, properties));
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
  public void closeConnection(CloseConnectionReq req, StreamObserver<DirectStatusResp> respOb) {
    DirectStatusResp.Builder builder = DirectStatusResp.newBuilder();
    try {
      if (req.getConnectionId().isEmpty()) {
        throw new IllegalArgumentException("ConnectionId cannot be empty for CloseConnection");
      } else {
        builder.setIdentifier(req.getConnectionId());
      }
      builder.setStatus(OK);
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

  @Override
  public void nativeSQL(NativeSQLReq request, StreamObserver<NativeSQLResp> responseObserver) {
    NativeSQLResp.Builder builder = NativeSQLResp.newBuilder();
    try {
      String nativeSQL = getConnection().nativeSQL(request.getSql());
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
      getConnection().setAutoCommit(request.getAutoCommit());
      responseObserver.onNext(ok(request.getConnectionId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getAutoCommit(GetAutoCommitReq request, StreamObserver<GetAutoCommitResp> responseObserver) {
    GetAutoCommitResp.Builder builder = GetAutoCommitResp.newBuilder();
    try {
      boolean autoCommit = getConnection().getAutoCommit();
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
  public void commit(CommitReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection().commit();
      responseObserver.onNext(ok(request.getConnectionId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void rollback(RollbackReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      Connection conn = getConnection();
      Savepoint savepoint = request.getSavepoint();
      if (savepoint == Savepoint.getDefaultInstance()) {
        conn.rollback();
      } else {
        conn.rollback(savepoints.get(savepoint));
      }
      responseObserver.onNext(ok(request.getConnectionId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void setReadOnly(SetReadOnlyReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection().setReadOnly(request.getReadOnly());
      responseObserver.onNext(ok(request.getConnectionId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void isReadOnly(IsReadOnlyReq request, StreamObserver<IsReadOnlyResp> responseObserver) {
    IsReadOnlyResp.Builder builder = IsReadOnlyResp.newBuilder();
    try {
      IsReadOnlyResp resp = builder
        .setStatus(OK)
        .setReadOnly(getConnection().isReadOnly())
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
      getConnection().setCatalog(request.getCatalog());
      responseObserver.onNext(ok(request.getConnectionId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getCatalog(GetCatalogReq request, StreamObserver<GetCatalogResp> responseObserver) {
    GetCatalogResp.Builder builder = GetCatalogResp.newBuilder();
    try {
      String catalog = getConnection().getCatalog();
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
      getConnection().setTransactionIsolation(request.getLevel());
      responseObserver.onNext(ok(request.getConnectionId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getTransactionIsolation(GetTransactionIsolationReq request, StreamObserver<GetTransactionIsolationResp> responseObserver) {
    GetTransactionIsolationResp.Builder builder = GetTransactionIsolationResp.newBuilder();
    try {
      int level = getConnection().getTransactionIsolation();
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

  public static SQLWarning buildWarning(java.sql.SQLWarning cur) {
    SQLWarning.Builder builder = SQLWarning.newBuilder();
    if (cur.getMessage() != null) {
      builder.setReason(cur.getMessage());
    }
    if (cur.getSQLState() != null) {
      builder.setSqlState(cur.getSQLState());
    }
    if (cur.getErrorCode() != 0) {
      builder.setVendorCode(cur.getErrorCode());
    }
    if (cur.getNextWarning() != null) {
      builder.setNextWarning(buildWarning(cur.getNextWarning()));
    }
    return builder.build();
  }
  @Override
  public void getWarnings(GetWarningsReq request, StreamObserver<GetWarningsResp> responseObserver) {
    GetWarningsResp.Builder builder = GetWarningsResp.newBuilder();
    try {
      java.sql.SQLWarning warnings = getConnection().getWarnings();

      if (warnings != null) {
        builder.setWarnings(buildWarning(getConnection().getWarnings()));
      }
      responseObserver.onNext(builder.setStatus(OK).build());
    } catch (SQLException e) {
      responseObserver.onNext(builder.setStatus(errorStatus(e)).build());
    }
    responseObserver.onCompleted();
  }

  @Override
  public void clearWarnings(ClearWarningsReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection().clearWarnings();
      responseObserver.onNext(ok(request.getConnectionId()));
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
      getConnection().setTypeMap(classMap);
      responseObserver.onNext(ok(request.getConnectionId()));
    } catch (Exception e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getTypeMap(GetTypeMapReq request, StreamObserver<GetTypeMapResp> responseObserver) {
    GetTypeMapResp.Builder builder = GetTypeMapResp.newBuilder();
    try {
      Map<String, Class<?>> typeMap = getConnection().getTypeMap();
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
      getConnection().setSchema(request.getSchema());
      responseObserver.onNext(ok(request.getConnectionId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getSchema(GetSchemaReq request, StreamObserver<GetSchemaResp> responseObserver) {
    GetSchemaResp.Builder builder = GetSchemaResp.newBuilder();
    try {
      String schema = getConnection().getSchema();
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
        getConnection().setNetworkTimeout(executor, request.getMilliseconds());
        responseObserver.onNext(ok(request.getConnectionId()));
      } catch (SQLException e) {
        responseObserver.onNext(error(e));
      }
      responseObserver.onCompleted();
  }

  @Override
  public void getNetworkTimeout(GetNetworkTimeoutReq request, StreamObserver<GetNetworkTimeoutResp> responseObserver) {
    GetNetworkTimeoutResp.Builder builder = GetNetworkTimeoutResp.newBuilder();
    try {
      int timeout = getConnection().getNetworkTimeout();
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
      boolean valid = getConnection().isValid(request.getTimeout());
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
  public void abortConnection(AbortConnectionReq request, StreamObserver<DirectStatusResp> responseObserver) {
    try {
      getConnection().abort(executor);
      responseObserver.onNext(ok(request.getConnectionId()));
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
      getConnection().setClientInfo(properties);
      responseObserver.onNext(ok(request.getConnectionId()));
    } catch (SQLException e) {
      responseObserver.onNext(error(e));
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getClientInfo(GetClientInfoReq request, StreamObserver<GetClientInfoResp> responseObserver) {
    GetClientInfoResp.Builder builder = GetClientInfoResp.newBuilder();
    try {
      Properties properties = getConnection().getClientInfo();
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
