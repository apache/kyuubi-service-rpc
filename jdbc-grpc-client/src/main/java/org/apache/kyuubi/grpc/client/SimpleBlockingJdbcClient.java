package org.apache.kyuubi.grpc.client;

import io.grpc.*;
import org.apache.kyuubi.grpc.GetWarningsResp;
import org.apache.kyuubi.grpc.common.ConnectionHandle;
import org.apache.kyuubi.grpc.common.StatementHandle;
import org.apache.kyuubi.grpc.jdbc.*;
import org.apache.kyuubi.grpc.jdbc.JdbcGrpc.JdbcBlockingStub;
import org.apache.kyuubi.grpc.jdbc.connection.*;
import org.apache.kyuubi.grpc.jdbc.statement.*;

import java.util.Map;
import java.util.Optional;

public class SimpleBlockingJdbcClient implements JdbcGrpcClient {
  private JdbcBlockingStub blockingStub = null;
  private ConnectionGrpc.ConnectionBlockingStub connectionBlockingStub = null;
  private StatementGrpc.StatementBlockingStub statementBlockingStub = null;

  public SimpleBlockingJdbcClient(Channel channel) {
    blockingStub = JdbcGrpc.newBlockingStub(channel);
    connectionBlockingStub = ConnectionGrpc.newBlockingStub(channel);
    statementBlockingStub = StatementGrpc.newBlockingStub(channel);
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
    ConnectionHandle.Builder builder = ConnectionHandle.newBuilder();
    connectionId.ifPresent(builder::setId);
    OpenConnectionReq req = OpenConnectionReq.newBuilder()
      .setConnectionId(builder.build())
      .putAllConfigs(configs)
      .build();
    return connectionBlockingStub.openConnection(req);
  }

  @Override
  public DirectStatusResp closeConnection(String connectionId) {
    ConnectionHandle.Builder builder = ConnectionHandle.newBuilder();
    ConnectionHandle req = builder
      .setId(connectionId)
      .build();
    return connectionBlockingStub.closeConnection(req);
  }

  @Override
  public DirectStatusResp abortConnection(String connectionId) {
    ConnectionHandle.Builder builder = ConnectionHandle.newBuilder();
    ConnectionHandle req = builder
      .setId(connectionId)
      .build();
    return connectionBlockingStub.abortConnection(req);
  }

  @Override
  public DirectStatusResp setClientInfo(String connectionId, Map<String, String> info) {
    SetClientInfoReq.Builder builder = SetClientInfoReq.newBuilder();
    SetClientInfoReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .putAllConfigs(info)
      .build();
    return connectionBlockingStub.setClientInfo(req);
  }

  @Override
  public DirectStatusResp setClientInfo(String connectionId, String name, String value) {
    SetClientInfoReq.Builder builder = SetClientInfoReq.newBuilder();
    SetClientInfoReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .putConfigs(name, value)
      .build();
    return connectionBlockingStub.setClientInfo(req);
  }

  @Override
  public GetClientInfoResp getClientInfo(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.getClientInfo(req);
  }

  @Override
  public DirectStatusResp setTypeMap(String connectionId, Map<String, String> map) {
    SetTypeMapReq.Builder builder = SetTypeMapReq.newBuilder();
    SetTypeMapReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .putAllTypeToClass(map)
      .build();
    return connectionBlockingStub.setTypeMap(req);
  }

  @Override
  public GetTypeMapResp getTypeMap(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.getTypeMap(req);
  }

  @Override
  public DirectStatusResp setHoldability(String connectionId, int holdability) {
    SetHoldabilityReq.Builder builder = SetHoldabilityReq.newBuilder();
    SetHoldabilityReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setHoldability(holdability)
      .build();
    return connectionBlockingStub.setHoldability(req);
  }

  @Override
  public GetHoldabilityResp getHoldability(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.getHoldability(req);
  }

  @Override
  public DirectStatusResp setSchema(String connectionId, String schema) {
    SetSchemaReq.Builder builder = SetSchemaReq.newBuilder();
    SetSchemaReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setSchema(schema)
      .build();
    return connectionBlockingStub.setSchema(req);
  }

  @Override
  public GetSchemaResp getSchema(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.getSchema(req);
  }

  @Override
  public DirectStatusResp setNetworkTimeout(String connectionId, int milliseconds) {
    SetNetworkTimeoutReq.Builder builder = SetNetworkTimeoutReq.newBuilder();
    SetNetworkTimeoutReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setMilliseconds(milliseconds)
      .build();
    return connectionBlockingStub.setNetworkTimeout(req);
  }

  @Override
  public GetNetworkTimeoutResp getNetworkTimeout(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.getNetworkTimeout(req);
  }

  @Override
  public SetSavepointResp setSavepoint(String connectionId) {
    SetSavepointReq.Builder builder = SetSavepointReq.newBuilder();
    SetSavepointReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .build();
    return connectionBlockingStub.setSavepoint(req);
  }

  @Override
  public SetSavepointResp setSavepoint(String connectionId, String name) {
    SetSavepointReq.Builder builder = SetSavepointReq.newBuilder();
    SetSavepointReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setSavepointName(name)
      .build();
    return connectionBlockingStub.setSavepoint(req);
  }

  @Override
  public DirectStatusResp releaseSavepoint(String connectionId, Savepoint savepoint) {
    ReleaseSavepointReq.Builder builder = ReleaseSavepointReq.newBuilder();
    ReleaseSavepointReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setSavepoint(savepoint)
      .build();
    return connectionBlockingStub.releaseSavepoint(req);
  }


  @Override
  public DirectStatusResp setSchema(String connectionId, String schema, String catalog) {
    SetSchemaReq.Builder builder = SetSchemaReq.newBuilder();
    SetSchemaReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setSchema(schema)
      .build();
    return connectionBlockingStub.setSchema(req);
  }

  @Override
  public IsValidResp isValid(String connectionId, int timeout) {
    IsValidReq.Builder builder = IsValidReq.newBuilder();
    IsValidReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setTimeout(timeout)
      .build();
    return connectionBlockingStub.isValid(req);
  }

  @Override
  public NativeSQLResp nativeSQL(String connectionId, String sql) {
    NativeSQLReq.Builder builder = NativeSQLReq.newBuilder();
    NativeSQLReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setSql(sql)
      .build();
    return connectionBlockingStub.nativeSQL(req);
  }

  @Override
  public DirectStatusResp setAutoCommit(String connectionId, boolean autoCommit) {
    SetAutoCommitReq.Builder builder = SetAutoCommitReq.newBuilder();
    SetAutoCommitReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setAutoCommit(autoCommit)
      .build();
    return connectionBlockingStub.setAutoCommit(req);
  }

  @Override
  public GetAutoCommitResp getAutoCommit(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.getAutoCommit(req);
  }

  @Override
  public DirectStatusResp commit(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.commit(req);
  }

  @Override
  public DirectStatusResp rollback(String connectionId) {
    RollbackReq.Builder builder = RollbackReq.newBuilder();
    RollbackReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .build();
    return connectionBlockingStub.rollback(req);
  }

  @Override
  public DirectStatusResp rollback(String connectionId, int savepointId) {
    Savepoint sp = Savepoint.newBuilder()
      .setSavepointId(savepointId)
      .build();
    RollbackReq req = RollbackReq.newBuilder()
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
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
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
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
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setSavepoint(sp)
      .build();
    return connectionBlockingStub.rollback(req);
  }

  @Override
  public DirectStatusResp setReadOnly(String connectionId, boolean readOnly) {
    SetReadOnlyReq.Builder builder = SetReadOnlyReq.newBuilder();
    SetReadOnlyReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setReadOnly(readOnly)
      .build();
    return connectionBlockingStub.setReadOnly(req);
  }

  @Override
  public IsReadOnlyResp isReadOnly(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.isReadOnly(req);
  }

  @Override
  public DirectStatusResp setCatalog(String connectionId, String catalog) {
    SetCatalogReq.Builder builder = SetCatalogReq.newBuilder();
    SetCatalogReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setCatalog(catalog)
      .build();
    return connectionBlockingStub.setCatalog(req);
  }

  @Override
  public GetCatalogResp getCatalog(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.getCatalog(req);
  }


  @Override
  public DirectStatusResp setTransactionIsolation(String connectionId, int level) {
    SetTransactionIsolationReq.Builder builder = SetTransactionIsolationReq.newBuilder();
    SetTransactionIsolationReq req = builder
      .setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build())
      .setLevel(level)
      .build();
    return connectionBlockingStub.setTransactionIsolation(req);
  }

  @Override
  public GetTransactionIsolationResp getTransactionIsolation(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.getTransactionIsolation(req);
  }

  @Override
  public DirectStatusResp clearWarnings(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.clearWarnings(req);
  }

  @Override
  public GetWarningsResp getWarnings(String connectionId) {
    ConnectionHandle req = ConnectionHandle.newBuilder()
      .setId(connectionId)
      .build();
    return connectionBlockingStub.getWarnings(req);
  }

  public DirectStatusResp createStatement(String connectionId, Optional<String> statementId) {
    CreateStatementReq.Builder builder = CreateStatementReq.newBuilder();
    builder.setConnectionId(ConnectionHandle.newBuilder().setId(connectionId).build());
    if (statementId.isPresent()) {
      StatementHandle handle = StatementHandle.newBuilder()
        .setId(statementId.get())
        .build();
      builder.setStatementId(handle);
    }
    return statementBlockingStub.createStatement(builder.build());
  }

  public DirectStatusResp closeStatement(String statementId) {
    StatementHandle req = StatementHandle.newBuilder()
      .setId(statementId)
      .build();
    return statementBlockingStub.closeStatement(req);
  }

  public DirectStatusResp executeQuery(String statementId, String sql) {
    StatementHandle handle = StatementHandle.newBuilder()
      .setId(statementId)
      .build();
    ExecuteQueryReq req = ExecuteQueryReq.newBuilder()
      .setStatementId(handle)
      .setSql(sql)
      .build();
    return statementBlockingStub.executeQuery(req);
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
