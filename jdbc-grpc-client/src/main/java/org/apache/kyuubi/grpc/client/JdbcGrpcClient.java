package org.apache.kyuubi.grpc.client;

import java.util.Map;
import java.util.Optional;

import org.apache.kyuubi.grpc.jdbc.DirectStatusResp;
import org.apache.kyuubi.grpc.jdbc.connection.*;

public interface JdbcGrpcClient {
  DirectStatusResp openConnection(Map<String, String> configs, Optional<String> connectionId);

  DirectStatusResp closeConnection(String connectionId);

  DirectStatusResp abortConnection(String connectionId);

  DirectStatusResp setAutoCommit(String connectionId, boolean autoCommit);

  GetAutoCommitResp getAutoCommit(String connectionId);

  NativeSQLResp nativeSQL(String connectionId, String sql);

  DirectStatusResp commit(String connectionId);

  DirectStatusResp rollback(String connectionId);

  DirectStatusResp rollback(String connectionId, int savepointId);

  DirectStatusResp rollback(String connectionId, String savepointName);

  DirectStatusResp rollback(String connectionId, int savepointId, String savepointName);

  DirectStatusResp setReadOnly(String connectionId, boolean readOnly);

  IsReadOnlyResp isReadOnly(String connectionId);

  DirectStatusResp setCatalog(String connectionId, String catalog);

  GetCatalogResp getCatalog(String connectionId);

  DirectStatusResp setTransactionIsolation(String connectionId, int level);

  GetTransactionIsolationResp getTransactionIsolation(String connectionId);

  GetWarningsResp getWarnings(String connectionId);

  DirectStatusResp clearWarnings(String connectionId);

  DirectStatusResp setClientInfo(String connectionId, Map<String, String> info);

  DirectStatusResp setClientInfo(String connectionId, String name, String value);

  GetClientInfoResp getClientInfo(String connectionId);

  DirectStatusResp setTypeMap(String connectionId, Map<String, String> map);

  GetTypeMapResp getTypeMap(String connectionId);

  DirectStatusResp setHoldability(String connectionId, int holdability);

  GetHoldabilityResp getHoldability(String connectionId);

  DirectStatusResp setSchema(String connectionId, String schema);

  GetSchemaResp getSchema(String connectionId);

  DirectStatusResp setNetworkTimeout(String connectionId, int milliseconds);

  GetNetworkTimeoutResp getNetworkTimeout(String connectionId);

  SetSavepointResp setSavepoint(String connectionId);

  SetSavepointResp setSavepoint(String connectionId, String name);

  DirectStatusResp releaseSavepoint(String connectionId, Savepoint savepoint);

  DirectStatusResp setSchema(String connectionId, String schema, String catalog);

  IsValidResp isValid(String connectionId, int timeout);

  DirectStatusResp getCatalogs(String connectionId);
}
