package com.nepxion.discovery.platform.server.service;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 *
 * @author Ning Zhang
 * @version 1.0
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nepxion.discovery.common.util.JsonUtil;
import com.nepxion.discovery.console.resource.ConfigResource;
import com.nepxion.discovery.console.resource.ServiceResource;
import com.nepxion.discovery.platform.server.annotation.TransactionReader;
import com.nepxion.discovery.platform.server.annotation.TransactionWriter;
import com.nepxion.discovery.platform.server.constant.PlatformConstant;
import com.nepxion.discovery.platform.server.entity.base.BaseEntity;
import com.nepxion.discovery.platform.server.entity.dto.RouteGatewayDto;
import com.nepxion.discovery.platform.server.entity.enums.Operation;
import com.nepxion.discovery.platform.server.entity.po.RouteGatewayPo;
import com.nepxion.discovery.platform.server.mapper.RouteGatewayMapper;
import com.nepxion.discovery.platform.server.tool.CommonTool;
import com.nepxion.discovery.platform.server.tool.SequenceTool;

public class RouteGatewayServiceImpl extends ServiceImpl<RouteGatewayMapper, RouteGatewayDto> implements RouteGatewayService {
    @Autowired
    private RouteService routeService;

    @Autowired
    private ServiceResource serviceResource;

    @Autowired
    private ConfigResource configResource;

    @TransactionWriter
    @Override
    public void publish() throws Exception {
        List<RouteGatewayDto> routeGatewayDtoList = list();

        if (CollectionUtils.isEmpty(routeGatewayDtoList)) {
            List<String> gatewayNameList = serviceResource.getGatewayList(GATEWAY_TYPE);
            for (String gatewayName : gatewayNameList) {
                String group = serviceResource.getGroup(gatewayName);
                updateConfig(gatewayName, group, new ArrayList<RouteGatewayPo>());
            }
            return;
        }

        List<RouteGatewayDto> toUpdateList = new ArrayList<>(routeGatewayDtoList.size());
        List<RouteGatewayDto> toDeleteList = new ArrayList<>(routeGatewayDtoList.size());
        Map<String, List<RouteGatewayDto>> unusedMap = new HashMap<>();

        Map<String, List<RouteGatewayPo>> newGatewayRouteMap = new HashMap<>();
        for (RouteGatewayDto routeGatewayDto : routeGatewayDtoList) {
            if (routeGatewayDto.getDeleteFlag()) {
                toDeleteList.add(routeGatewayDto);
                addKV(unusedMap, routeGatewayDto.getGatewayName(), routeGatewayDto);
                continue;
            } else if (!routeGatewayDto.getEnableFlag()) {
                addKV(unusedMap, routeGatewayDto.getGatewayName(), routeGatewayDto);
                toUpdateList.add(routeGatewayDto);
                continue;
            }

            toUpdateList.add(routeGatewayDto);

            RouteGatewayPo routeGatewayPo = new RouteGatewayPo();
            routeGatewayPo.setId(routeGatewayDto.getRouteId());
            routeGatewayPo.setUri(routeGatewayDto.getUri());

            if (StringUtils.isNotEmpty(routeGatewayDto.getPredicates())) {
                routeGatewayPo.setPredicates(Arrays.asList(routeGatewayDto.getPredicates().split(PlatformConstant.ROW_SEPARATOR)));
            }
            if (StringUtils.isNotEmpty(routeGatewayDto.getUserPredicates())) {
                List<RouteGatewayPo.Predicate> predicateList = parse(routeGatewayDto.getUserPredicates(), RouteGatewayPo.Predicate.class);
                routeGatewayPo.setUserPredicates(predicateList);
            }
            if (StringUtils.isNotEmpty(routeGatewayDto.getFilters())) {
                routeGatewayPo.setFilters(Arrays.asList(routeGatewayDto.getFilters().split(PlatformConstant.ROW_SEPARATOR)));
            }
            if (StringUtils.isNotEmpty(routeGatewayDto.getUserFilters())) {
                List<RouteGatewayPo.Filter> filterList = parse(routeGatewayDto.getUserFilters(), RouteGatewayPo.Filter.class);
                routeGatewayPo.setUserFilters(filterList);
            }

            routeGatewayPo.setOrder(routeGatewayDto.getOrder());
            routeGatewayPo.setMetadata(CommonTool.asMap(routeGatewayDto.getMetadata(), PlatformConstant.ROW_SEPARATOR));

            if (newGatewayRouteMap.containsKey(routeGatewayDto.getGatewayName())) {
                newGatewayRouteMap.get(routeGatewayDto.getGatewayName()).add(routeGatewayPo);
            } else {
                List<RouteGatewayPo> routeGatewayPoList = new ArrayList<>();
                routeGatewayPoList.add(routeGatewayPo);
                newGatewayRouteMap.put(routeGatewayDto.getGatewayName(), routeGatewayPoList);
            }
        }

        if (CollectionUtils.isEmpty(newGatewayRouteMap)) {
            for (Map.Entry<String, List<RouteGatewayDto>> pair : unusedMap.entrySet()) {
                String gatewayName = pair.getKey();
                String group = serviceResource.getGroup(gatewayName);
                updateConfig(gatewayName, group, new ArrayList<RouteGatewayPo>());
            }
        } else {
            for (Map.Entry<String, List<RouteGatewayPo>> pair : newGatewayRouteMap.entrySet()) {
                String gatewayName = pair.getKey();
                String group = serviceResource.getGroup(gatewayName);
                updateConfig(gatewayName, group, pair.getValue());
            }
        }

        if (!CollectionUtils.isEmpty(toDeleteList)) {
            delete(toDeleteList.stream().map(BaseEntity::getId).collect(Collectors.toSet()));
        }

        if (!CollectionUtils.isEmpty(toUpdateList)) {
            for (RouteGatewayDto routeGatewayDto : toUpdateList) {
                routeGatewayDto.setPublishFlag(true);
            }
            updateBatchById(toUpdateList, toUpdateList.size());
        }
    }

    @SuppressWarnings("unchecked")
    @TransactionReader
    @Override
    public IPage<RouteGatewayDto> page(String description, Integer pageNum, Integer pageSize) {
        QueryWrapper<RouteGatewayDto> queryWrapper = new QueryWrapper<>();
        LambdaQueryWrapper<RouteGatewayDto> lambda = queryWrapper.lambda().orderByAsc(RouteGatewayDto::getCreateTime);
        if (StringUtils.isNotEmpty(description)) {
            lambda.eq(RouteGatewayDto::getDescription, description);
        }
        return page(new Page<>(pageNum, pageSize), queryWrapper);
    }

    @TransactionReader
    @Override
    public RouteGatewayDto getById(Long id) {
        if (id == null) {
            return null;
        }
        return super.getById(id);
    }

    @TransactionWriter
    @Override
    public void insert(RouteGatewayDto routeGatewayDto) {
        if (routeGatewayDto == null) {
            return;
        }

        Integer nextMaxCreateTimesInDayOfGateway = routeService.getNextMaxCreateTimesInDayOfGateway();
        if (StringUtils.isEmpty(routeGatewayDto.getRouteId())) {
            routeGatewayDto.setRouteId(SequenceTool.getSequenceId(nextMaxCreateTimesInDayOfGateway));
        }
        routeGatewayDto.setCreateTimesInDay(nextMaxCreateTimesInDayOfGateway);
        routeGatewayDto.setOperation(Operation.INSERT.getCode());
        routeGatewayDto.setPublishFlag(false);
        routeGatewayDto.setDeleteFlag(false);
        save(routeGatewayDto);
    }

    @TransactionWriter
    @Override
    public void update(RouteGatewayDto routeGatewayDto) {
        if (routeGatewayDto == null) {
            return;
        }
        routeGatewayDto.setPublishFlag(false);
        routeGatewayDto.setDeleteFlag(false);
        routeGatewayDto.setOperation(Operation.UPDATE.getCode());
        updateById(routeGatewayDto);
    }

    @TransactionWriter
    @Override
    public void enable(Long id, boolean enableFlag) {
        RouteGatewayDto routeGatewayDto = getById(id);
        routeGatewayDto.setEnableFlag(enableFlag);
        update(routeGatewayDto);
    }

    @TransactionWriter
    @Override
    public void logicDelete(Collection<Long> ids) {
        for (Long id : ids) {
            RouteGatewayDto routeGatewayDto = getById(id);
            if (routeGatewayDto == null) {
                continue;
            }
            routeGatewayDto.setDeleteFlag(true);
            routeGatewayDto.setPublishFlag(false);
            routeGatewayDto.setOperation(Operation.DELETE.getCode());
            updateById(routeGatewayDto);
        }
    }

    @TransactionWriter
    @Override
    public void delete(Collection<Long> ids) {
        removeByIds(ids);
    }

    private void updateConfig(String gatewayName, String group, Object config) throws Exception {
        String serviceId = gatewayName.concat("-").concat(PlatformConstant.GATEWAY_DYNAMIC_ROUTE);
        configResource.updateRemoteConfig(group, serviceId, JsonUtil.toPrettyJson(config));
    }

    private void addKV(Map<String, List<RouteGatewayDto>> map, String key, RouteGatewayDto value) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            List<RouteGatewayDto> routeGatewayDtoList = new ArrayList<>();
            routeGatewayDtoList.add(value);
            map.put(key, routeGatewayDtoList);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends RouteGatewayPo.Clause> List<T> parse(String value, Class<T> tClass) throws Exception {
        List<T> result = new ArrayList<>();
        String[] all = value.split(PlatformConstant.ROW_SEPARATOR);
        for (String item : all) {
            T t = tClass.newInstance();

            int index = item.indexOf("=");
            if (index < 0) {
                continue;
            }
            String name = item.substring(0, index).trim();
            String json = item.substring(index + 1).trim();
            Map<String, String> map = JsonUtil.fromJson(json, Map.class);
            t.setName(name);
            t.setArgs(map);
            result.add(t);
        }
        return result;
    }
}