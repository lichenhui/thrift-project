package cn.lichenhui.rpc.client.manage.discover;

import cn.lichenhui.rpc.client.manage.ZookeeperFactory;
import com.wmz7year.thrift.pool.config.ThriftServerInfo;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DiscoverService {

	private static final Logger log = LoggerFactory.getLogger(DiscoverService.class);

	private List<ThriftServerInfo> serverInfoList = new ArrayList<>();

	private Map<String, String> serviceMap = new HashMap<>();

	private CuratorFramework zkClient;
	private PathChildrenCache pathChildrenCache;

	private static String serviceNodePath = "/service-node";
	private static String serviceClassesPath = "/service-class";

	private boolean start = false;

	@Autowired
	private ZookeeperFactory zookeeperFactory;

	@Autowired
	private ConnectionPool connectionPool;

	public List<ThriftServerInfo> getServerInfoList() {
		if (serverInfoList.size() == 0 && !start) {
			try {
				discover();
			} catch (Exception e) {
				throw new Error("服务发现发生错误", e);
			}
		}
		return serverInfoList;
	}

	public Map<String, String> getServiceMap() {
		if (serviceMap.size() == 0 && !start) {
			try {
				discover();
			} catch (Exception e) {
				throw new Error("服务发现发生错误", e);
			}
		}
		return serviceMap;
	}

	private void discover() throws Exception {
		log.info("服务发现...");
		if (zkClient == null) {
			this.zkClient = zookeeperFactory.getZkClient();
		}
		List<String> hosts = zkClient.getChildren().forPath(serviceNodePath);
		for (String host : hosts) {
			String[] arr = host.split(":");
			ThriftServerInfo serverInfo = new ThriftServerInfo(arr[0], Integer.valueOf(arr[1]));
			serverInfoList.add(serverInfo);
		}
		log.info("服务节点列表:{}", serverInfoList);
		List<String> serviceClasses = zkClient.getChildren().forPath(serviceClassesPath);
		for (String serviceClass : serviceClasses) {
			String className = new String(zkClient.getData().forPath(serviceClassesPath + "/" + serviceClass), Charset.forName("utf-8"));
			serviceMap.put(serviceClass, className);
		}
		serviceNodeWatcher();
		start = true;
	}

	private void serviceNodeWatcher() {
		pathChildrenCache = new PathChildrenCache(zkClient, serviceNodePath, true);
		log.info("监听...path:{}", serviceNodePath);
		try {
			pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
		} catch (Exception e) {
			throw new Error("启动服务节点监控错误", e);

		}
		pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
				PathChildrenCacheEvent.Type type = pathChildrenCacheEvent.getType();
				log.info("zookeeper event type:{}", type.name());
				if (type == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
					ChildData data = pathChildrenCacheEvent.getData();
					ThriftServerInfo serverInfo = getServiceInfo(data);
					connectionPool.removeServer(serverInfo);
					serverInfoList.remove(serverInfo);
					log.info("从连接池删除服务节点:{}", serverInfo);
					log.info("删除后服务节点列表:{}", serverInfoList);
				}
				if (type == PathChildrenCacheEvent.Type.CHILD_ADDED) {
					ChildData data = pathChildrenCacheEvent.getData();
					ThriftServerInfo serverInfo = getServiceInfo(data);
					connectionPool.addServer(serverInfo);
					log.info("从连接池增加服务节点:{}", serverInfo);
					serverInfoList.add(serverInfo);
					log.info("增加后服务节点列表:{}", serverInfoList);
				}
				pathChildrenCache.rebuild();
			}
		});
	}

	private ThriftServerInfo getServiceInfo(ChildData data) {
		String[] arr = data.getPath().split("/");
		String delHost = arr[arr.length - 1];
		String[] hostInfo = delHost.split(":");
		return new ThriftServerInfo(hostInfo[0], Integer.valueOf(hostInfo[1]));
	}

}
