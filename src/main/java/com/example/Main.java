/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import static javax.measure.unit.SI.KILOGRAM;
import javax.measure.quantity.Mass;
import org.jscience.physics.model.RelativisticModel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jscience.physics.amount.Amount;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@SpringBootApplication
public class Main {

  @Value("${spring.datasource.url}")
  private String dbUrl;

  @Autowired
  private DataSource dataSource;

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Main.class, args);
  }

  @RequestMapping("/")
  String index() {
    return "index";
  }

  @RequestMapping("/hello")
  String hello(Map<String, Object> model) {
    RelativisticModel.select();
    Amount<Mass> m = Amount.valueOf("12 GeV").to(KILOGRAM);
    model.put("science", "E=mc^2: 12 GeV = " + m.toString());
    return "hello";
  }

  @RequestMapping(value = "/transportation", method = RequestMethod.POST)
  public ResponseEntity<Object> transportation(@RequestBody Map<String, Object> payload) throws Exception {
    // TransportationProblem t = new TransportationProblem();

    String filename = "src/main/java/com/example/input0.txt";
    TransportationProblem.updateFile(payload);
    TransportationProblem.init(filename);
    TransportationProblem.northWestCornerRule();
    TransportationProblem.steppingStone();
    TransportationProblem.printResult(filename);
    // System.out.println(payload.toString());
    Map<String, Object> a = TransportationProblem.printResult(filename);
    System.out.println("json " + a.toString());
    return new ResponseEntity<Object>(a, HttpStatus.OK);
  }

  @RequestMapping("/db")
  String db(Map<String, Object> model) {
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
      stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
      ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

      ArrayList<String> output = new ArrayList<String>();
      while (rs.next()) {
        output.add("Read from DB: " + rs.getTimestamp("tick"));
      }

      model.put("records", output);
      return "db";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }
  }

  @RequestMapping(value = "/hungarian", method = RequestMethod.POST)
  public ResponseEntity<Object> hungarian(@RequestBody Map<String, Object> payload) throws Exception {
    JSONObject pay = new JSONObject(payload);
    int[][] dataMatrix = new int[pay.getJSONArray("teste").length()][];
    System.out.println(pay.getJSONArray("teste").length());
    int index = 0;
    for (Object var : pay.getJSONArray("teste")) {
      System.out.println(var);
      List<String> list = Arrays.asList(var.toString().split(" "));
      List<Integer> listInt = list.stream().map(s -> Integer.parseInt(s)).collect(Collectors.toList());
      dataMatrix[index++] = listInt.stream().mapToInt(i -> i).toArray();
    }
    HungarianAlgorithm ha = new HungarianAlgorithm(dataMatrix);
    int[][] assignment = ha.findOptimalAssignment();
    Map<String, Object> map = new HashMap<>();
    String value = "";
    for (int i = 0; i < assignment.length; i++) {
      for (int j = 0; j < assignment[i].length; j++) {
        System.out.print(assignment[i][j] + " ");
        value = value.concat(Integer.toString(assignment[i][j]) + " ");
      }
      if (value != "") {
        map.put(i + "", value);
        value = "";
      }
      System.out.println("");
    }
    return new ResponseEntity<Object>(map, HttpStatus.OK);
  }

  @Bean
  public DataSource dataSource() throws SQLException {
    if (dbUrl == null || dbUrl.isEmpty()) {
      return new HikariDataSource();
    } else {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(dbUrl);
      return new HikariDataSource(config);
    }
  }

}
