package cn.lichenhui.rpc.client.manage.discover;

import com.wmz7year.thrift.pool.ThriftConnectionPool;
import com.wmz7year.thrift.pool.config.ThriftConnectionPoolConfig;
import com.wmz7year.thrift.pool.config.ThriftServerInfo;
import com.wmz7year.thrift.pool.connection.ThriftConnection;
import com.wmz7year.thrift.pool.exception.ThriftConnectionPoolException;
import org.apache.thrift.TServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class ConnectionPool {

	private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);

	private ThriftConnectionPool<TServiceClient> pool;

	@Autowired
	private DiscoverService discoverService;

	public ThriftConnection<TServiceClient> getConnection() throws ThriftConnectionPoolException {
		if (pool == null) {
			try {
				initPool();
			} catch (Exception e) {
				throw new Error("初始化thrift连接池错误", e);
			}
		}
		return pool.getConnection();
	}

	@PostConstruct
	public void initPool() throws Exception {
		if (pool != null) {
			return;
		}
		ThriftConnectionPoolConfig config = new ThriftConnectionPoolConfig(ThriftConnectionPoolConfig.ThriftServiceType.MULTIPLEXED_INTERFACE);
		config.setConnectTimeout(3000);
		config.setThriftProtocol(ThriftConnectionPoolConfig.TProtocolType.BINARY);
		List<ThriftServerInfo> serverInfos = discoverService.getServerInfoList();
		Map<String, String> serviceClassMap = discoverService.getServiceMap();
		for (ThriftServerInfo server : serverInfos) {
			config.addThriftServer(server);
		}
		Set<Map.Entry<String, String>> classEntry = serviceClassMap.entrySet();
		for (Map.Entry<String, String> entry : classEntry) {
			config.addThriftClientClass(entry.getKey(), Class.forName(entry.getValue() + "$Client").asSubclass(TServiceClient.class));
		}

		config.setMaxConnectionPerServer(5);
		config.setMinConnectionPerServer(5);
		config.setIdleMaxAge(2, TimeUnit.SECONDS);
		config.setMaxConnectionAge(2);
		config.setLazyInit(false);
		config.setAcquireIncrement(2);
		config.setAcquireRetryDelay(2000);

		config.setAcquireRetryAttempts(1);
		config.setMaxConnectionCreateFailedCount(1);
		config.setConnectionTimeoutInMs(5000);

		pool = new ThriftConnectionPool<>(config);
	}

	public void removeServer(ThriftServerInfo serverInfo) {
		pool.removeThriftServer(serverInfo);
	}

	public void addServer(ThriftServerInfo serverInfo) {
		try {
			pool.addThriftServer(serverInfo);
		} catch (ThriftConnectionPoolException e) {
			log.warn("添加节点错误", e);
		}
	}
}
