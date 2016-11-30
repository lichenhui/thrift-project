package cn.lichenhui.rpc.server.manage.register;

import cn.lichenhui.rpc.server.manage.ZookeeperFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Properties;

@Component
public class RegisterService {

	private static final Logger log = LoggerFactory.getLogger(RegisterService.class);

	@Value("${thrift.service.server.port}")
	private String port;

	@Autowired
	private ZookeeperFactory zookeeperFactory;

	private static String serviceNodePath = "/service-node";
	private static String serviceClassesPath = "/service-class";

	private CuratorFramework zkClient;

	/**
	 * 注册服务地址
	 */
	public void registerHost() throws Exception {
		if (zkClient == null) {
			this.zkClient = zookeeperFactory.getZkClient();
		}
		zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(hostPath(port));

	}

	public void registerService(String serviceName, String serviceClass) throws Exception {
		if (zkClient == null) {
			this.zkClient = zookeeperFactory.getZkClient();
		}
		String path = serviceClassPath(serviceName);
		if (zkClient.checkExists().forPath(path) == null) {
			zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
			zkClient.setData().forPath(path, serviceClass.getBytes());
		}
	}


	private static String hostPath(String port) {
		return serviceNodePath + "/" + getServerIp() + ":" + port;
	}

	private static String serviceClassPath(String serviceName) {
		return serviceClassesPath + "/" + serviceName;
	}


	private static String getServerIp() {
		// 一个主机有多个网络接口
		try {
			Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
			while (netInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = netInterfaces.nextElement();
				// 每个网络接口,都会有多个"网络地址",比如一定会有lookback地址,会有siteLocal地址等.以及IPV4或者IPV6 .
				Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if(address instanceof Inet6Address){
						continue;
					}
					if (address.isSiteLocalAddress() && !address.isLoopbackAddress()) {
						String serverIp = address.getHostAddress();
						log.info("resolve server ip :"+ serverIp);
						return serverIp;
					}
				}
			}
		} catch (SocketException e) {
			log.error("resolve service ip error", e);
		}
		return null;
	}

	private Properties readRpcConfig(String configFile) throws Exception {
		Properties properties = new Properties();
		properties.load(new InputStreamReader(RegisterService.class.getClassLoader().getResourceAsStream(configFile), Charset.forName("utf-8")));

		return properties;
	}

	public int getPort() {
		return Integer.valueOf(port);
	}
}
