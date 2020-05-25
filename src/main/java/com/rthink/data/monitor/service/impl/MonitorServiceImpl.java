package com.rthink.data.monitor.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.mountcloud.graphql.GraphqlClient;
import org.mountcloud.graphql.request.query.DefaultGraphqlQuery;
import org.mountcloud.graphql.request.query.GraphqlQuery;
import org.mountcloud.graphql.response.GraphqlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.rthink.data.monitor.service.MonitorService;
import com.rthink.data.monitor.util.InfluxDbUtils;
import com.rthink.data.monitor.util.RegexUtils;

@Service
public class MonitorServiceImpl implements MonitorService {
	private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
	private static final String ALARM_DESC = "%s液位告警%.2f/%.2f(超出%.2fcm)";
	
	@Autowired
	private InfluxDbUtils influxDbUtils;

	@Override
	public boolean isCurrChannelId(String message) {
		String regex = "\\$(.*?)\\$";
		return StringUtils.equals(RegexUtils.interceptRegexStr(regex, message), "28d39cd5-3757-42de-9a59-1bb1fdefd4db");
	}

	@Override
	@Async
	public void monitor(String message) {
		logger.info("message:{}", message);
		// 截取消息中的液位
		String channel = RegexUtils.interceptRegexStr("\\$(.*?)\\$", message);
		String publisher = RegexUtils.interceptRegexStr(".*\\$(.*?)\"", message);
		double currWaterLevel = NumberUtils.toDouble(RegexUtils.interceptRegexStr("\"v\":(.*?)}", message));
		logger.info("channel:{} publisher:{} currWaterLevel:{}", channel, publisher, currWaterLevel);
		// 查询neo4j中的告警液位
		Map<String, String> assetMap = queryAssetInfo(publisher);
		logger.info("assetMap:{}", assetMap);
		if (!assetMap.containsKey("alarmWaterLevel")) {
			logger.info("no alarm water level config");
			return;
		}
		double alarmWaterLevel = NumberUtils.toDouble(assetMap.get("alarmWaterLevel"));
		// 比较液位信息，并产生告警(写入influxdb)
		double exceedWaterLevel = currWaterLevel - alarmWaterLevel;
		if (exceedWaterLevel <= 0 ) {
			logger.info("no exceed alarm water level [{}/{}]", currWaterLevel, alarmWaterLevel);
			return;
		}
		// 将告警信息插入influxdb
		InfluxDB influxDB = influxDbUtils.getInfluxDB();
		String alarmDesc = String.format(ALARM_DESC, assetMap.get("assetName"), currWaterLevel, alarmWaterLevel, exceedWaterLevel);
		influxDB.write(Point.measurement("alarm_info")
				.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.addField("channel", channel)
				.addField("publisher", publisher)
				.addField("type", "0")
				.addField("asset_name", assetMap.get("assetName").toString())
				.addField("curr_value", currWaterLevel)
				.addField("alarm_value", alarmWaterLevel)
				.addField("desc", alarmDesc)
				.build());
		logger.info("alarmDesc:{}", alarmDesc);
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, String> queryAssetInfo(String id) {
		Map<String, String> assetMap = new HashMap<String, String>();
		try {
			String graphqlUrl = "http://127.0.0.1:4001/api";
			GraphqlClient graphqlClient = GraphqlClient.buildGraphqlClient(graphqlUrl);
			GraphqlQuery query = new DefaultGraphqlQuery("Device");
			query.addParameter("id", id);
			query.addResultAttributes("id", "name", "asset{assetType{waterLevel}}");
			GraphqlResponse response = graphqlClient.doQuery(query);
			Map<String, Object> resultMap = response.getData();
			if (resultMap != null && resultMap.containsKey("data")) {
				// {Device=[{id=28d39cd5-3757-42de-9a59-1bb1fdefd4db, name=d西河泵站, asset=[{assetType={waterLevel=[120, 110, 120]}}]}]}
				Map<String, Object>  dataMap =  (Map<String, Object>) resultMap.get("data");
				if (dataMap == null || !dataMap.containsKey("Device")) {
					return assetMap;
				}
				List<Map<String, Object>> devices = (List<Map<String, Object>>) dataMap.get("Device");
				if (CollectionUtils.isEmpty(devices) || !devices.get(0).containsKey("asset")) {
					return assetMap;
				}
				assetMap.put("assetName", devices.get(0).get("name").toString());
				List<Map<String, Object>> assets = (List<Map<String, Object>>) devices.get(0).get("asset");
				if (!CollectionUtils.isEmpty(assets) && assets.get(0).containsKey("assetType")) {
					Map<String, List<Double>> assetTypeMap = (Map<String, List<Double>>) assets.get(0).get("assetType");
					List<Double> waterLevels = assetTypeMap.get("waterLevel");
					// 下标1：井深 下标2：预警水深 下标3：危急报警水深
					if (!CollectionUtils.isEmpty(waterLevels) && waterLevels.size() >= 3) {
						assetMap.put("alarmWaterLevel", String.valueOf(waterLevels.get(2)));
					}
				}
			}
		} catch (Exception e) {
			logger.error("queryWaterLevels exception:", e);
		}
		return assetMap;
	}
}
