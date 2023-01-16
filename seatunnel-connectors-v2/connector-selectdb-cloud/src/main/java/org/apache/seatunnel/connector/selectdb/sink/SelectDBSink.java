/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.connector.selectdb.sink;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.common.PrepareFailException;
import org.apache.seatunnel.api.common.SeaTunnelAPIErrorCode;
import org.apache.seatunnel.api.serialization.DefaultSerializer;
import org.apache.seatunnel.api.serialization.Serializer;
import org.apache.seatunnel.api.sink.SeaTunnelSink;
import org.apache.seatunnel.api.sink.SinkAggregatedCommitter;
import org.apache.seatunnel.api.sink.SinkCommitter;
import org.apache.seatunnel.api.sink.SinkWriter;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.config.CheckConfigUtil;
import org.apache.seatunnel.common.config.CheckResult;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.connector.selectdb.exception.SelectDBConnectorException;
import org.apache.seatunnel.connector.selectdb.sink.committer.SelectDBCommittable;
import org.apache.seatunnel.connector.selectdb.sink.committer.SelectDBCommitter;
import org.apache.seatunnel.connector.selectdb.sink.writer.SelectDBWriter;
import org.apache.seatunnel.connector.selectdb.sink.writer.SelectDBWriterState;
import org.apache.seatunnel.shade.com.typesafe.config.Config;

import static org.apache.seatunnel.connector.selectdb.config.SelectDBConfig.JDBC_URL;
import static org.apache.seatunnel.connector.selectdb.config.SelectDBConfig.LOAD_URL;
import static org.apache.seatunnel.connector.selectdb.config.SelectDBConfig.CLUSTER_NAME;
import static org.apache.seatunnel.connector.selectdb.config.SelectDBConfig.USERNAME;
import static org.apache.seatunnel.connector.selectdb.config.SelectDBConfig.TABLE_IDENTIFIER;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AutoService(SeaTunnelSink.class)
public class SelectDBSink implements SeaTunnelSink<SeaTunnelRow, SelectDBWriterState, SelectDBCommittable, SelectDBCommittable> {
    private Config pluginConfig;
    private SeaTunnelRowType seaTunnelRowType;


    @Override
    public String getPluginName() {
        return "SelectDBSink";
    }

    @Override
    public void prepare(Config pluginConfig) throws PrepareFailException {
        this.pluginConfig = pluginConfig;
        CheckResult result = CheckConfigUtil.checkAllExists(pluginConfig, JDBC_URL.key(), LOAD_URL.key(), CLUSTER_NAME.key(), USERNAME.key(), TABLE_IDENTIFIER.key());
        if (!result.isSuccess()) {
            throw new SelectDBConnectorException(SeaTunnelAPIErrorCode.CONFIG_VALIDATION_FAILED,
                    String.format("PluginName: %s, PluginType: %s, Message: %s",
                            getPluginName(), PluginType.SINK, result.getMsg()));
        }
    }

    @Override
    public void setTypeInfo(SeaTunnelRowType seaTunnelRowType) {
        this.seaTunnelRowType = seaTunnelRowType;
    }

    @Override
    public SeaTunnelDataType<SeaTunnelRow> getConsumedType() {
        return this.seaTunnelRowType;
    }


    @Override
    public SinkWriter<SeaTunnelRow, SelectDBCommittable, SelectDBWriterState> createWriter(SinkWriter.Context context) throws IOException {
        SelectDBWriter dorisWriter = new SelectDBWriter(context, Collections.emptyList(), seaTunnelRowType, pluginConfig);
        dorisWriter.initializeLoad(Collections.emptyList());
        return dorisWriter;
    }

    @Override
    public SinkWriter<SeaTunnelRow, SelectDBCommittable, SelectDBWriterState> restoreWriter(SinkWriter.Context context, List<SelectDBWriterState> states) throws IOException {
        SelectDBWriter dorisWriter = new SelectDBWriter(context, states, seaTunnelRowType, pluginConfig);
        dorisWriter.initializeLoad(states);
        return dorisWriter;
    }

    @Override
    public Optional<Serializer<SelectDBWriterState>> getWriterStateSerializer() {
        return Optional.of(new DefaultSerializer<>());
    }

    @Override
    public Optional<SinkCommitter<SelectDBCommittable>> createCommitter() throws IOException {
        return Optional.of(new SelectDBCommitter(pluginConfig));
    }


    @Override
    public Optional<Serializer<SelectDBCommittable>> getCommitInfoSerializer() {
        return Optional.of(new DefaultSerializer<>());
    }

    @Override
    public Optional<SinkAggregatedCommitter<SelectDBCommittable, SelectDBCommittable>> createAggregatedCommitter() throws IOException {
        return Optional.empty();
    }

    @Override
    public Optional<Serializer<SelectDBCommittable>> getAggregatedCommitInfoSerializer() {
        return Optional.empty();
    }
}
