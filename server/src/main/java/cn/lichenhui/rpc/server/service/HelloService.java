package cn.lichenhui.rpc.server.service;

import cn.lichenhui.rpc.server.annotations.RpcService;
import cn.lichenhui.rpc.thrift.service.IHelloService;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;


@Service
@RpcService("helloService")
public class HelloService implements IHelloService.Iface {

	@Override
	public int sum(int a, int b) throws TException {
		return a + b;
	}

	@Override
	public void ping() throws TException {

	}
}
