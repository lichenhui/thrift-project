package cn.lichenhui.rpc.client.manage;

import cn.lichenhui.rpc.client.manage.discover.ConnectionPool;
import com.wmz7year.thrift.pool.connection.ThriftConnection;
import com.wmz7year.thrift.pool.exception.ThriftConnectionPoolException;
import org.apache.thrift.TServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceClientFactory {

	@Autowired
	private ConnectionPool pool;

	public <K extends TServiceClient> K getClient(String serviceName, Class<K> clazz) throws ThriftConnectionPoolException {
		ThriftConnection<TServiceClient> connection = pool.getConnection();
		return connection.getClient(serviceName, clazz);
	}

}
