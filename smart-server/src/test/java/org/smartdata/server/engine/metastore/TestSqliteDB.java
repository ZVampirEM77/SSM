/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartdata.server.engine.metastore;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.smartdata.server.engine.MetaStore;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test operations with sqlite database.
 */
public class TestSqliteDB extends TestDaoUtil {

  private MetaStore metaStore;

  @Before
  public void initDB() throws Exception {
    initDao();
    metaStore = new MetaStore(druidPool);
  }

  @After
  public void closeDB() throws Exception {
    metaStore = null;
    closeDao();
  }

  @Test
  public void testCreateNewSqliteDB() throws Exception {
    MetaUtil.initializeDataBase(metaStore.getConnection());
  }

  @Test
  public void testDropTablesSqlite() throws SQLException, ClassNotFoundException {
    Connection conn = metaStore.getConnection();
    Statement s = conn.createStatement();
    metaStore.dropAllTables();
    for (int i = 0; i < 10; i++) {
      metaStore.execute("DROP TABLE IF EXISTS tb_"+i+";");
      metaStore.execute("CREATE TABLE tb_"+i+" (a INT(11));");
    }
    ResultSet rs = s.executeQuery("select tbl_name from sqlite_master;");
    List<String> list = new ArrayList<>();
    while (rs.next()) {
      list.add(rs.getString(1));
    }
    metaStore.dropAllTables();
    rs = s.executeQuery("select tbl_name from sqlite_master;");
    List<String> list1 = new ArrayList<>();
    while (rs.next()) {
      list1.add(rs.getString(1));
    }
    assertEquals(10,list.size()-list1.size());
  }

  /*@Test
  public void testDropAllTablesMysql() throws SQLException {
    Connection conn = null;
    try {
      String url = "jdbc:mysql://localhost:3306/";
      conn = DriverManager.getConnection(url, "root", "linux123");
      Statement s = conn.createStatement();
      String db = "abcd";
      s.executeUpdate("DROP DATABASE IF EXISTS "+db+";");
      s.executeUpdate("CREATE DATABASE "+db+";");
      s.execute("use "+db+";");
      conn = MetaUtil.createConnection(url+db+"?","root","linux123");
      MetaStore adapter = new MetaStore(conn);
      adapter.dropAllTables();

      for (int i = 0; i < 10; i++) {
        adapter.execute("DROP TABLE IF EXISTS tb_"+i+";");
        adapter.execute("CREATE TABLE tb_"+i+" (a INT(11));");
      }
      List<String> list = new ArrayList<>();
      ResultSet rs = adapter.executeQuery("SELECT TABLE_NAME FROM " +
          "INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + db + "';");
      while (rs.next()) {
        list.add(rs.getString(1));
      }

      adapter.dropAllTables();

      List<String> list1 = new ArrayList<>();
      rs = adapter.executeQuery("SELECT TABLE_NAME FROM " +
          "INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + db + "';");
      while (rs.next()) {
        list1.add(rs.getString(1));
      }
      assertEquals(10,list.size()-list1.size());
      adapter.executeUpdate("DROP DATABASE IF EXISTS abc");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
  }*/

  @Test
  public void testSqliteDBBlankStatements() throws Exception {
    String[] presqls = new String[] {
        "INSERT INTO rules (state, rule_text, submit_time, checked_count, "
            + "cmdlets_generated) VALUES (0, 'file: every 1s \n" + " | "
            + "accessCount(5s) > 3 | cache', 1494903787619, 0, 0);"
    };

    for (int i = 0; i< presqls.length; i++) {
      String sql = presqls[i];
      metaStore.execute(sql);
    }

    String[] sqls = new String[] {
        "DROP TABLE IF EXISTS 'VIR_ACC_CNT_TAB_1_accessCount_5000';",
        "CREATE TABLE 'VIR_ACC_CNT_TAB_1_accessCount_5000' "
            + "AS SELECT * FROM 'blank_access_count_info';",
        "SELECT fid from 'VIR_ACC_CNT_TAB_1_accessCount_5000';",
        "SELECT path FROM files WHERE (fid IN (SELECT fid FROM "
            + "'VIR_ACC_CNT_TAB_1_accessCount_5000' WHERE ((count > 3))));"
    };

    for (int i = 0; i< sqls.length * 3; i++) {
      int idx = i % sqls.length;
      String sql = sqls[idx];
      metaStore.execute(sql);
    }
  }
}
