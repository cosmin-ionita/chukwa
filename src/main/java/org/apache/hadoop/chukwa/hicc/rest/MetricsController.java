/*
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
package org.apache.hadoop.chukwa.hicc.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.hadoop.chukwa.datastore.ChukwaHBaseStore;
import org.apache.hadoop.chukwa.hicc.TimeHandler;
import org.apache.hadoop.chukwa.hicc.bean.Series;
import org.json.simple.JSONArray;

import com.google.gson.Gson;

@Path("/metrics")
public class MetricsController {

  @GET
  @Path("series/{metric}/{source}")
  @Produces("application/json")
  public String getSeries(@Context HttpServletRequest request, @PathParam("metric") String metric, @PathParam("source") String source, @QueryParam("start") String start, @QueryParam("end") String end) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    String buffer = "";
    Series series;
    long startTime = 0;
    long endTime = 0;
    TimeHandler time = new TimeHandler(request);
    try {
      if(start!=null) {
        startTime = sdf.parse(start).getTime();
      } else {
        startTime = time.getStartTime();
      }
      if(end!=null) {
        endTime = sdf.parse(end).getTime();
      } else {
        endTime = time.getEndTime();
      }
      series = ChukwaHBaseStore.getSeries(metric, source, startTime, endTime);
      buffer = series.toString();
    } catch (ParseException e) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity("Start/End date parse error.  Format: yyyyMMddHHmmss.").build());
    }
    return buffer;
  }

  @GET
  @Path("series/{metricGroup}/{metric}/session/{sessionKey}")
  @Produces("application/json")
  public String getSeriesBySessionAttribute(@Context HttpServletRequest request, @PathParam("metricGroup") String metricGroup, @PathParam("metric") String metric, @PathParam("sessionKey") String skey, @QueryParam("start") String start, @QueryParam("end") String end) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    String buffer = "";
    long startTime = 0;
    long endTime = 0;
    TimeHandler time = new TimeHandler(request);
    try {
      if(start!=null) {
        startTime = sdf.parse(start).getTime();
      } else {
        startTime = time.getStartTime();
      }
      if(end!=null) {
        endTime = sdf.parse(end).getTime();
      } else {
        endTime = time.getEndTime();
      }
      if(skey!=null) {
          HttpSession session = request.getSession();
          String[] sourcekeys = (session.getAttribute(skey).toString()).split(",");
          JSONArray seriesList = new JSONArray();
          for(String source : sourcekeys) {
        	if (source == null || source.equals("")) {
        		continue;
        	}
            Series output = ChukwaHBaseStore.getSeries(metricGroup, metric, source, startTime, endTime);
            seriesList.add(output.toJSONObject());
          }
          buffer = seriesList.toString();
      } else {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
            .entity("No session attribute key defined.").build());
      }
    } catch (ParseException e) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity("Start/End date parse error.  Format: yyyyMMddHHmmss.").build());
    }
    return buffer;
  }

  @GET
  @Path("schema")
  @Produces("application/json")
  public String getTables() {
    Set<String> metricGroups = ChukwaHBaseStore.getMetricGroups();
    JSONArray groups = new JSONArray();
    for(String metric : metricGroups) {
      groups.add(metric);
    }
    return groups.toString();
  }
  
  @GET
  @Path("schema/{metricGroup}")
  @Produces("application/json")
  public String getMetrics(@PathParam("metricGroup") String metricGroup) {
    Set<String> metricNames = ChukwaHBaseStore.getMetricNames(metricGroup);
    JSONArray metrics = new JSONArray();
    for(String metric : metricNames) {
      metrics.add(metric);
    }
    return metrics.toString();
  }

  @GET
  @Path("source/{metricGroup}")
  @Produces("application/json")
  public String getSourceNames(@Context HttpServletRequest request, @PathParam("metricGroup") String metricGroup) {
    Set<String> sourceNames = ChukwaHBaseStore.getSourceNames(metricGroup);
    JSONArray rows = new JSONArray();
    for(String row : sourceNames) {
      rows.add(row);
    }
    return rows.toString();
  }

}