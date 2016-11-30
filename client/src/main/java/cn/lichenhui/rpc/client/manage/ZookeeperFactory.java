package cn.lichenhui.rpc.client.manage;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@PropertySource(value = "rpc-config.properties")
public class ZookeeperFactory {

	@Value("${zookeeper.server}")
	private String zkHosts;

	// session超时
	@Value("${zookeeper.session.timeout}")
	private String sessionTimeout = "30000";
	@Value("${zookeeper.connection.timeout}")
	private String connectionTimeout = "3000";

	// 全局path前缀,常用来区分不同的应用
	@Value("${zookeeper.namespace}")
	private String namespace;

	private CuratorFramework zkClient;

	public CuratorFramework getZkClient () {
		if (zkClient == null) {
			createZkClient();
		}

		return zkClient;
	}

	@PostConstruct
	private void createZkClient() {
		CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
		zkClient = builder.connectString(zkHosts)
				.sessionTimeoutMs(Integer.valueOf(sessionTimeout))
				.connectionTimeoutMs(Integer.valueOf(connectionTimeout))
				.namespace(namespace)
				.retryPolicy(new ExponentialBackoffRetry(1000, 3))
				.defaultData(null)
				.build();
		zkClient.start();
	}


}
