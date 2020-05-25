package com.rthink.data.monitor.task;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.logimethods.connector.nats.to_spark.NatsToSparkConnector;
import com.rthink.data.monitor.service.MonitorService;


@Component
public class NatsToSparkService implements ApplicationRunner, Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(NatsToSparkService.class);
	
	private static final String SPACE = " ";
	
	// transient告诉编译器不需要序列化，否则后续引用都需要序列化
	@Autowired
	private transient MonitorService monitorService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		try {
			LOGGER.info("start receive nats message");
			SparkSession sparkSession = SparkSession
				      .builder()
				      .appName("Java Spark SQL basic example")
				      .config("spark.some.config.option", "some-value")
				      .master("local[*]")
				      .getOrCreate();
			JavaSparkContext javaSparkContext = JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
			JavaStreamingContext javaStreamingContext = new JavaStreamingContext(javaSparkContext, new Duration(5000));
			JavaReceiverInputDStream<String> messages = NatsToSparkConnector
					.receiveFromNats(String.class, StorageLevel.MEMORY_ONLY())
					.withNatsURL("nats://118.31.19.149:4222")
					.withSubjects("channels.*")
					.asStreamOf(javaStreamingContext);
			// 处理消息
			JavaDStream<String> javaDStream = messages.flatMap(new FlatMapFunction<String, String>() {
				private static final long serialVersionUID = 1L;
				
				public Iterator<String> call(String str) throws Exception {
					Iterator<String> iterator = Arrays.asList(str.split(SPACE)).iterator();
					while (iterator.hasNext()) {
						String message = iterator.next();
						if (monitorService.isCurrChannelId(message)) {
							monitorService.monitor(message);
						}
					}
					return iterator;
				}
			});
			javaDStream.print();
			javaStreamingContext.start();
			javaStreamingContext.awaitTermination();
		} catch (Exception e) {
			LOGGER.error("natsToSpark exception:", e);
		}
		
	}
}
