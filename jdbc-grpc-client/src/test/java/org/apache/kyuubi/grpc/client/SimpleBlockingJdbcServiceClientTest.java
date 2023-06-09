package org.apache.kyuubi.grpc.client;

import io.grpc.*;
import org.apache.kyuubi.grpc.*;
import org.apache.kyuubi.grpc.Status;
import org.apache.kyuubi.grpc.jdbc.DirectStatusResp;
import org.apache.kyuubi.grpc.jdbc.connection.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class SimpleBlockingJdbcServiceClientTest {

  private final int port = 0;

  private Server server = null;
  private SimpleBlockingJdbcClient client = null;
  DummyJdbcService dummyFrontendService = new DummyJdbcService();
  TestConnectionService dummyConnectionService = new TestConnectionService();
  TestStatementService dummyStatementService = new TestStatementService(dummyConnectionService);

  public SimpleBlockingJdbcServiceClientTest() throws IOException {
  }

  @Before
  public void setUp() throws IOException {
    ServerCredentials serverCredentials = InsecureServerCredentials.create();
    ServerBuilder<?> builder = Grpc.newServerBuilderForPort(port, serverCredentials)
      .addService(dummyConnectionService)
      .addService(dummyStatementService)
      .addService(dummyFrontendService);
    server = builder.build();
    server.start();
    client = new SimpleBlockingJdbcClient(server.getPort());
  }

  @After
  public void tearDown() {
    dummyConnectionService.stop();
    if (server != null) {
      try {
        server.awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        // do nothing
      }
    }
  }

  @Test
  public void testServer() {
    assertNotEquals(port, server.getPort());
    assertFalse(server.isTerminated());
  }

  @Test
  public void testOpenConnection() {
    Map<String, String> configs = new HashMap<>();
    DirectStatusResp resp = client.openConnection(configs, Optional.empty());
    Status status = resp.getStatus();
    String connectionId = resp.getIdentifier();
    assertEquals(StatusCode.OK, status.getStatusCode());
    assertEquals("00000", status.getSqlState());
    assertEquals("Serverless SQL on Lakehouse", resp.getExtraInfoOrThrow("Kyuubi"));

    configs.put("apache", "kyuubi");
    configs.put("kent", "yao");
    DirectStatusResp resp1 = client.openConnection(configs, Optional.of(connectionId));
    Status status1 = resp1.getStatus();
    assertEquals(connectionId, resp1.getIdentifier());
    assertEquals(StatusCode.OK, status1.getStatusCode());
    assertEquals("kyuubi", resp1.getExtraInfoOrThrow("apache"));
  }

  @Test
  public void testCloseConnection() {
    String connectionId = UUID.randomUUID().toString();
    DirectStatusResp resp1 = client.closeConnection(connectionId);
    Status resp1Status = resp1.getStatus();
    assertEquals(StatusCode.ERROR, resp1Status.getStatusCode());
    assertEquals("2E000", resp1Status.getSqlState());
    assertEquals("invalid connection id " + connectionId, resp1Status.getErrorMessage());
    DirectStatusResp openConn = client.openConnection(Collections.emptyMap(), Optional.empty());
    connectionId = openConn.getIdentifier();
    DirectStatusResp resp2 = client.closeConnection(connectionId);
    Status resp2Status = resp2.getStatus();
    assertEquals(StatusCode.OK, resp2Status.getStatusCode());
    assertEquals("00000", resp2Status.getSqlState());
  }

  @Test
  public void testSetClientInfo() {
    DirectStatusResp resp = client.setClientInfo("apache kyuubi", Collections.emptyMap());
    Status status = resp.getStatus();
    assertEquals(StatusCode.OK, status.getStatusCode());
    assertEquals("00000", status.getSqlState());
  }

  @Test
  public void testGetCatalogs() {
    DirectStatusResp resp = client.getCatalogs("kyuubi");
    Status status = resp.getStatus();
    assertEquals(StatusCode.OK, status.getStatusCode());
    assertEquals("00000", status.getSqlState());
    DirectStatusResp resp1 = client.getCatalogs("");
    assertEquals("", resp1.getIdentifier());
    assertEquals(StatusCode.ERROR, resp1.getStatus().getStatusCode());
  }

  @Test
  public void testNativeSQL() {
    NativeSQLResp resp = client.nativeSQL("kyuubi", "SELECT {fn NOW()}");
    Status status = resp.getStatus();
    assertEquals(StatusCode.OK, status.getStatusCode());
    String sql = resp.getSql();
    assertFalse(sql.contains("fn"));
  }


  @Test
  public void testSetAndGetAutoCommit() {
    GetAutoCommitResp resp = client.getAutoCommit("kyuubi");
    assertTrue(resp.getAutoCommit());
    client.setAutoCommit("kyuubi", false);
    GetAutoCommitResp resp1 = client.getAutoCommit("kyuubi");
    assertFalse(resp1.getAutoCommit());
  }

  @Test
  public void testCommitAndRollback() {
    DirectStatusResp resp = client.commit("kyuubi");
    assertEquals(StatusCode.OK, resp.getStatus().getStatusCode());
    DirectStatusResp resp1 = client.rollback("kyuubi");
    assertEquals(StatusCode.OK, resp1.getStatus().getStatusCode());
    DirectStatusResp resp2 = client.rollback("kyuubi", "kyuubi_savepoint");
    assertEquals(StatusCode.ERROR, resp2.getStatus().getStatusCode());
  }

  @Test
  public void testSetReadOnly() {
    DirectStatusResp resp = client.setReadOnly("kyuubi", true);
    assertEquals(StatusCode.OK, resp.getStatus().getStatusCode());
    DirectStatusResp resp1 = client.setReadOnly("kyuubi", false);
    assertEquals(StatusCode.OK, resp1.getStatus().getStatusCode());
  }

  @Test
  public void testIsReadOnly() {
    IsReadOnlyResp resp = client.isReadOnly("kyuubi");
    assertEquals(StatusCode.OK, resp.getStatus().getStatusCode());
    boolean readOnly = resp.getReadOnly();
    assertFalse(readOnly);
  }

  @Test
  public void testSetAndGetCatalog() {
    // H2 doesn't support switch catalog and the op will be ignored without any exception
    DirectStatusResp resp = client.setCatalog("kyuubi", "kyuubi");
    assertEquals(StatusCode.OK, resp.getStatus().getStatusCode());
    GetCatalogResp resp1 = client.getCatalog("kyuubi");
    assertEquals(StatusCode.OK, resp1.getStatus().getStatusCode());
    String catalog = resp1.getCatalog();
    assertEquals(dummyConnectionService.defaultCatalogName, catalog);
  }

  @Test
  public void testSetAndGetTransactionIsolationLevel() {
    // case 1: set/get transaction isolation level
    DirectStatusResp resp = client.setTransactionIsolation("kyuubi", 1);
    assertEquals(StatusCode.OK, resp.getStatus().getStatusCode());
    // case 2: get transaction isolation level
    GetTransactionIsolationResp resp1 = client.getTransactionIsolation("kyuubi");
    assertEquals(StatusCode.OK, resp1.getStatus().getStatusCode());
    int level = resp1.getLevel();
    assertEquals(1, level);
  }

  @Test
  public void testBuildSQLWarnings() {
    java.sql.SQLWarning warning1 = new java.sql.SQLWarning("warning1");
    warning1.setNextWarning(new java.sql.SQLWarning("warning2"));
    SQLWarning warning = GrpcUtils.toProto(warning1);
    assertEquals("warning1", warning.getReason());
    assertEquals("warning2", warning.getNextWarning().getReason());
  }

  @Test
  public void testGetAndClearWarnings() {
    GetWarningsResp resp = client.getWarnings("kyuubi");
    assertEquals(StatusCode.OK, resp.getStatus().getStatusCode());
    SQLWarning warning = resp.getWarnings();
    assertEquals(SQLWarning.getDefaultInstance(), warning);
    DirectStatusResp resp1 = client.clearWarnings("kyuubi");
    assertEquals(StatusCode.OK, resp1.getStatus().getStatusCode());
  }

  @Test
  public void testGetAndSetSchema() {
    // case 1: set/get schema
    DirectStatusResp resp = client.setSchema("kyuubi", "kyuubi");
    assertEquals(StatusCode.ERROR, resp.getStatus().getStatusCode());
    assertTrue(resp.getStatus().getErrorMessage().contains("not found"));
    DirectStatusResp resp0 = client.setSchema("kyuubi", "PUBLIC");
    assertEquals(StatusCode.OK, resp0.getStatus().getStatusCode());
    GetSchemaResp resp1 = client.getSchema("kyuubi");
    assertEquals(StatusCode.OK, resp1.getStatus().getStatusCode());
    String schema = resp1.getSchema();
    assertEquals("PUBLIC", schema);
  }

  @Test
  public void testGetAndSetNetworkTimeout() {
    // case 1: set/get network timeout
    DirectStatusResp resp = client.setNetworkTimeout("kyuubi", 1000);
    assertEquals(StatusCode.OK, resp.getStatus().getStatusCode());
    GetNetworkTimeoutResp resp1 = client.getNetworkTimeout("kyuubi");
    assertEquals(StatusCode.OK, resp1.getStatus().getStatusCode());
    int timeout = resp1.getMilliseconds();
    assertEquals(0, timeout);
  }

  @Test
  public void testGetAndSetClientInfo() {
    HashMap<String, String> configs = new HashMap<>();
    configs.put("key1", "value1");
    DirectStatusResp resp = client.setClientInfo("kyuubi", configs);

    assertEquals(StatusCode.ERROR, resp.getStatus().getStatusCode());
    assertTrue(resp.getStatus().getErrorMessage().contains("not supported"));
    configs.clear();
    configs.put("ApplicationName", "value1");
    DirectStatusResp resp0 = client.setClientInfo("kyuubi", configs);
    assertEquals(StatusCode.OK, resp0.getStatus().getStatusCode());
    GetClientInfoResp resp1 = client.getClientInfo("kyuubi");
    assertEquals(StatusCode.OK, resp1.getStatus().getStatusCode());
    Map<String, String> clientInfo = resp1.getConfigsMap();
    configs.put("numServers", "0");
    assertEquals(configs, clientInfo);
  }

  @Test
  public void testGetAndSetClientInfoString() {
    DirectStatusResp resp = client.setClientInfo("kyuubi", "key1", "value1");
    assertEquals(StatusCode.ERROR, resp.getStatus().getStatusCode());
    assertTrue(resp.getStatus().getErrorMessage().contains("not supported"));
    DirectStatusResp resp0 = client.setClientInfo("kyuubi", "ApplicationName", "value1");
    assertEquals(StatusCode.OK, resp0.getStatus().getStatusCode());
    GetClientInfoResp resp1 = client.getClientInfo("kyuubi");
    assertEquals(StatusCode.OK, resp1.getStatus().getStatusCode());
    Map<String, String> clientInfo = resp1.getConfigsMap();
    assertEquals("value1", clientInfo.get("ApplicationName"));
  }

  @Test
  public void testAbortConnection() {
    DirectStatusResp resp = client.abortConnection("kyuubi");
    assertEquals(StatusCode.OK, resp.getStatus().getStatusCode());
  }

  @Test
  public void testExecuteQuery() {
    String sql = "SELECT * FROM UNNEST(ARRAY['a', 'b', 'c'])";
    DirectStatusResp resp = client.executeQuery("kyuubi", sql);
    assertEquals(StatusCode.ERROR, resp.getStatus().getStatusCode());
    assertTrue(resp.getStatus().getErrorMessage().contains("Statement Id kyuubi not found"));
    DirectStatusResp resp1 = client.createStatement("kyuubi", Optional.empty());
    String statementId = resp1.getIdentifier();
    DirectStatusResp resp2 = client.executeQuery(statementId, sql);
    assertEquals(StatusCode.OK, resp2.getStatus().getStatusCode());
    client.closeStatement(statementId);
  }
}