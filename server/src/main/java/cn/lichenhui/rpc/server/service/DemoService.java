package cn.lichenhui.rpc.server.service;

import cn.lichenhui.rpc.server.annotations.RpcService;
import cn.lichenhui.rpc.thrift.service.IDemoService;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Service
@RpcService("demoService")
public class DemoService implements IDemoService.Iface {

	@Override
	public String echoHello(String msg) throws TException {
		return msg;
	}

	@Override
	public void ping() throws TException {

	}
}
