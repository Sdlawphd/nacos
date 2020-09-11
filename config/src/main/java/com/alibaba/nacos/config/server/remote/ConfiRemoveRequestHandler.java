/*
 * Copyright 1999-2020 Alibaba Group Holding Ltd.
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

package com.alibaba.nacos.config.server.remote;

import com.alibaba.nacos.api.config.remote.request.ConfigRemoveRequest;
import com.alibaba.nacos.api.config.remote.response.ConfigRemoveResponse;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.remote.request.RequestMeta;
import com.alibaba.nacos.config.server.model.event.ConfigDataChangeEvent;
import com.alibaba.nacos.config.server.service.ConfigChangePublisher;
import com.alibaba.nacos.config.server.service.repository.PersistService;
import com.alibaba.nacos.config.server.service.trace.ConfigTraceService;
import com.alibaba.nacos.config.server.utils.ParamUtils;
import com.alibaba.nacos.config.server.utils.TimeUtils;
import com.alibaba.nacos.core.remote.RequestHandler;
import com.alibaba.nacos.core.utils.Loggers;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

/**
 * handler to remove config.
 *
 * @author liuzunfei
 * @version $Id: ConfiRemoveRequestHandler.java, v 0.1 2020年07月16日 5:49 PM liuzunfei Exp $
 */
@Component
public class ConfiRemoveRequestHandler extends RequestHandler<ConfigRemoveRequest, ConfigRemoveResponse> {
    
    @Autowired
    private PersistService persistService;
    
    @Override
    public ConfigRemoveResponse handle(ConfigRemoveRequest request, RequestMeta meta) throws NacosException {
        ConfigRemoveRequest myrequest = (ConfigRemoveRequest) request;
        // check tenant
        String tenant = myrequest.getTenant();
        String dataId = myrequest.getDataId();
        String group = myrequest.getGroup();
        String tag = myrequest.getTag();
        
        try {
            ParamUtils.checkTenant(tenant);
            ParamUtils.checkParam(dataId, group, "datumId", "rm");
            ParamUtils.checkParam(tag);
            
            String clientIp = meta.getClientIp();
            if (StringUtils.isBlank(tag)) {
                persistService.removeConfigInfo(dataId, group, tenant, clientIp, null);
            } else {
                persistService.removeConfigInfoTag(dataId, group, tenant, tag, clientIp, null);
            }
            final Timestamp time = TimeUtils.getCurrentTime();
            ConfigTraceService.logPersistenceEvent(dataId, group, tenant, null, time.getTime(), clientIp,
                    ConfigTraceService.PERSISTENCE_EVENT_REMOVE, null);
            ConfigChangePublisher
                    .notifyConfigChange(new ConfigDataChangeEvent(false, dataId, group, tenant, tag, time.getTime()));
            return ConfigRemoveResponse.buildSuccessResponse();
            
        } catch (Exception e) {
            Loggers.RPC_DIGEST.error("remove config error,error msg is {}", e.getMessage(), e);
            return ConfigRemoveResponse.buildFailResponse(e.getMessage());
        }
    }
    
}