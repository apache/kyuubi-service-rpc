package org.apache.kyuubi.grpc.client;

import io.grpc.*;
import org.apache.kyuubi.grpc.jdbc.*;
import org.apache.kyuubi.grpc.jdbc.JdbcGrpc.JdbcBlockingStub;
import org.apache.kyuubi.grpc.jdbc.connection.*;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class SimpleBlockingJdbcClient implements JdbcGrpcClient {
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private JdbcBlockingStub blockingStub = null;
  private ConnectionGrpc.ConnectionBlockingStub connectionBlockingStub = null;

  public SimpleBlockingJdbcClient(Channel channel) {
    blockingStub = JdbcGrpc.newBlockingStub(channel);
    connectionBlockingStub = ConnectionGrpc.newBlockingStub(channel);
  }

  public SimpleBlockingJdbcClient(ManagedChannelBuilder builder) {
    this(builder.build());
  }

  public SimpleBlockingJdbcClient(String host, int port, ChannelCredentials creds) {
    this(Grpc.newChannelBuilderForAddress(host, port, creds));
  }

  public SimpleBlockingJdbcClient(String host, int port) {
    this(host, port, InsecureChannelCredentials.create());
  }

  public SimpleBlockingJdbcClient(int port) {
    this("localhost", port);
  }

  @Override
  public DirectStatusResp openConnection(
    Map<String, String> configs,
    Optional<String> connectionId) {
    OpenConnectionReq.Builder builder = OpenConnectionReq.newBuilder();
    if (connectionId.isPresent()) {
      logger.info("Reconnecting to server with existing connection" + connectionId.get());
      builder.setConnectionId(connectionId.get());
    }
    OpenConnectionReq req = builder
      .putAllConfigs(configs)
      .build();
    return connectionBlockingStub.openConnection(req);
  }

  @Override
  public DirectStatusResp closeConnection(String connectionId) {
    CloseConnectionReq.Builder builder = CloseConnectionReq.newBuilder();
    CloseConnectionReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.closeConnection(req);
  }

  @Override
  public DirectStatusResp abortConnection(String connectionId) {
    AbortConnectionReq.Builder builder = AbortConnectionReq.newBuilder();
    AbortConnectionReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.abortConnection(req);
  }

  @Override
  public DirectStatusResp setClientInfo(String connectionId, Map<String, String> info) {
    SetClientInfoReq.Builder builder = SetClientInfoReq.newBuilder();
    SetClientInfoReq req = builder
      .setConnectionId(connectionId)
      .putAllConfigs(info)
      .build();
    return connectionBlockingStub.setClientInfo(req);
  }

  @Override
  public DirectStatusResp setClientInfo(String connectionId, String name, String value) {
    SetClientInfoReq.Builder builder = SetClientInfoReq.newBuilder();
    SetClientInfoReq req = builder
      .setConnectionId(connectionId)
      .putConfigs(name, value)
      .build();
    return connectionBlockingStub.setClientInfo(req);
  }

  @Override
  public GetClientInfoResp getClientInfo(String connectionId) {
    GetClientInfoReq.Builder builder = GetClientInfoReq.newBuilder();
    GetClientInfoReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.getClientInfo(req);
  }

  @Override
  public DirectStatusResp setTypeMap(String connectionId, Map<String, String> map) {
    SetTypeMapReq.Builder builder = SetTypeMapReq.newBuilder();
    SetTypeMapReq req = builder
      .setConnectionId(connectionId)
      .putAllTypeToClass(map)
      .build();
    return connectionBlockingStub.setTypeMap(req);
  }

  @Override
  public GetTypeMapResp getTypeMap(String connectionId) {
    GetTypeMapReq.Builder builder = GetTypeMapReq.newBuilder();
    GetTypeMapReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.getTypeMap(req);
  }

  @Override
  public DirectStatusResp setHoldability(String connectionId, int holdability) {
    SetHoldabilityReq.Builder builder = SetHoldabilityReq.newBuilder();
    SetHoldabilityReq req = builder
      .setConnectionId(connectionId)
      .setHoldability(holdability)
      .build();
    return connectionBlockingStub.setHoldability(req);
  }

  @Override
  public GetHoldabilityResp getHoldability(String connectionId) {
    GetHoldabilityReq.Builder builder = GetHoldabilityReq.newBuilder();
    GetHoldabilityReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.getHoldability(req);
  }

  @Override
  public DirectStatusResp setSchema(String connectionId, String schema) {
    SetSchemaReq.Builder builder = SetSchemaReq.newBuilder();
    SetSchemaReq req = builder
      .setConnectionId(connectionId)
      .setSchema(schema)
      .build();
    return connectionBlockingStub.setSchema(req);
  }

  @Override
  public GetSchemaResp getSchema(String connectionId) {
    GetSchemaReq.Builder builder = GetSchemaReq.newBuilder();
    GetSchemaReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.getSchema(req);
  }

  @Override
  public DirectStatusResp setNetworkTimeout(String connectionId, int milliseconds) {
    SetNetworkTimeoutReq.Builder builder = SetNetworkTimeoutReq.newBuilder();
    SetNetworkTimeoutReq req = builder
      .setConnectionId(connectionId)
      .setMilliseconds(milliseconds)
      .build();
    return connectionBlockingStub.setNetworkTimeout(req);
  }

  @Override
  public GetNetworkTimeoutResp getNetworkTimeout(String connectionId) {
    GetNetworkTimeoutReq.Builder builder = GetNetworkTimeoutReq.newBuilder();
    GetNetworkTimeoutReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.getNetworkTimeout(req);
  }

  @Override
  public SetSavepointResp setSavepoint(String connectionId) {
    SetSavepointReq.Builder builder = SetSavepointReq.newBuilder();
    SetSavepointReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.setSavepoint(req);
  }

  @Override
  public SetSavepointResp setSavepoint(String connectionId, String name) {
    SetSavepointReq.Builder builder = SetSavepointReq.newBuilder();
    SetSavepointReq req = builder
      .setConnectionId(connectionId)
      .setSavepointName(name)
      .build();
    return connectionBlockingStub.setSavepoint(req);
  }

  @Override
  public DirectStatusResp releaseSavepoint(String connectionId, Savepoint savepoint) {
    ReleaseSavepointReq.Builder builder = ReleaseSavepointReq.newBuilder();
    ReleaseSavepointReq req = builder
      .setConnectionId(connectionId)
      .setSavepoint(savepoint)
      .build();
    return connectionBlockingStub.releaseSavepoint(req);
  }


  @Override
  public DirectStatusResp setSchema(String connectionId, String schema, String catalog) {
    SetSchemaReq.Builder builder = SetSchemaReq.newBuilder();
    SetSchemaReq req = builder
      .setConnectionId(connectionId)
      .setSchema(schema)
      .build();
    return connectionBlockingStub.setSchema(req);
  }

  @Override
  public IsValidResp isValid(String connectionId, int timeout) {
    IsValidReq.Builder builder = IsValidReq.newBuilder();
    IsValidReq req = builder
      .setConnectionId(connectionId)
      .setTimeout(timeout)
      .build();
    return connectionBlockingStub.isValid(req);
  }

  @Override
  public NativeSQLResp nativeSQL(String connectionId, String sql) {
    NativeSQLReq.Builder builder = NativeSQLReq.newBuilder();
    NativeSQLReq req = builder
      .setConnectionId(connectionId)
      .setSql(sql)
      .build();
    return connectionBlockingStub.nativeSQL(req);
  }

  @Override
  public DirectStatusResp setAutoCommit(String connectionId, boolean autoCommit) {
    SetAutoCommitReq.Builder builder = SetAutoCommitReq.newBuilder();
    SetAutoCommitReq req = builder
      .setConnectionId(connectionId)
      .setAutoCommit(autoCommit)
      .build();
    return connectionBlockingStub.setAutoCommit(req);
  }

  @Override
  public GetAutoCommitResp getAutoCommit(String connectionId) {
    GetAutoCommitReq.Builder builder = GetAutoCommitReq.newBuilder();
    GetAutoCommitReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.getAutoCommit(req);
  }

  @Override
  public DirectStatusResp commit(String connectionId) {
    CommitReq.Builder builder = CommitReq.newBuilder();
    CommitReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.commit(req);
  }

  @Override
  public DirectStatusResp rollback(String connectionId) {
    RollbackReq.Builder builder = RollbackReq.newBuilder();
    RollbackReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.rollback(req);
  }

  @Override
  public DirectStatusResp rollback(String connectionId, int savepointId) {
    Savepoint sp = Savepoint.newBuilder()
      .setSavepointId(savepointId)
      .build();
    RollbackReq req = RollbackReq.newBuilder()
      .setConnectionId(connectionId)
      .setSavepoint(sp)
      .build();
    return connectionBlockingStub.rollback(req);
  }

  @Override
  public DirectStatusResp rollback(String connectionId, int savepointId, String savepointName) {
    Savepoint sp = Savepoint.newBuilder()
      .setSavepointId(savepointId)
      .setSavepointName(savepointName)
      .build();
    RollbackReq req = RollbackReq.newBuilder()
      .setConnectionId(connectionId)
      .setSavepoint(sp)
      .build();
    return connectionBlockingStub.rollback(req);
  }

  @Override
  public DirectStatusResp rollback(String connectionId, String savepointName) {
    Savepoint sp = Savepoint.newBuilder()
      .setSavepointName(savepointName)
      .build();
    RollbackReq req = RollbackReq.newBuilder()
      .setConnectionId(connectionId)
      .setSavepoint(sp)
      .build();
    return connectionBlockingStub.rollback(req);
  }

  @Override
  public DirectStatusResp setReadOnly(String connectionId, boolean readOnly) {
    SetReadOnlyReq.Builder builder = SetReadOnlyReq.newBuilder();
    SetReadOnlyReq req = builder
      .setConnectionId(connectionId)
      .setReadOnly(readOnly)
      .build();
    return connectionBlockingStub.setReadOnly(req);
  }

  @Override
  public IsReadOnlyResp isReadOnly(String connectionId) {
    IsReadOnlyReq.Builder builder = IsReadOnlyReq.newBuilder();
    IsReadOnlyReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.isReadOnly(req);
  }

  @Override
  public DirectStatusResp setCatalog(String connectionId, String catalog) {
    SetCatalogReq.Builder builder = SetCatalogReq.newBuilder();
    SetCatalogReq req = builder
      .setConnectionId(connectionId)
      .setCatalog(catalog)
      .build();
    return connectionBlockingStub.setCatalog(req);
  }

  @Override
  public GetCatalogResp getCatalog(String connectionId) {
    GetCatalogReq.Builder builder = GetCatalogReq.newBuilder();
    GetCatalogReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.getCatalog(req);
  }


  @Override
  public DirectStatusResp setTransactionIsolation(String connectionId, int level) {
    SetTransactionIsolationReq.Builder builder = SetTransactionIsolationReq.newBuilder();
    SetTransactionIsolationReq req = builder
      .setConnectionId(connectionId)
      .setLevel(level)
      .build();
    return connectionBlockingStub.setTransactionIsolation(req);
  }

  @Override
  public GetTransactionIsolationResp getTransactionIsolation(String connectionId) {
    GetTransactionIsolationReq.Builder builder = GetTransactionIsolationReq.newBuilder();
    GetTransactionIsolationReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.getTransactionIsolation(req);
  }

  @Override
  public DirectStatusResp clearWarnings(String connectionId) {
    ClearWarningsReq.Builder builder = ClearWarningsReq.newBuilder();
    ClearWarningsReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.clearWarnings(req);
  }

  @Override
  public GetWarningsResp getWarnings(String connectionId) {
    GetWarningsReq.Builder builder = GetWarningsReq.newBuilder();
    GetWarningsReq req = builder
      .setConnectionId(connectionId)
      .build();
    return connectionBlockingStub.getWarnings(req);
  }

  @Override
  public DirectStatusResp getCatalogs(String connectionId) {
    GetCatalogsReq.Builder builder = GetCatalogsReq.newBuilder();
    GetCatalogsReq req = builder
      .setConnectionId(connectionId)
      .build();
    return blockingStub.getCatalogs(req);
  }
}
