package cn.lichenhui.rpc.client.controller;

import cn.lichenhui.rpc.client.manage.ServiceClientFactory;
import cn.lichenhui.rpc.thrift.service.IHelloService;
import com.wmz7year.thrift.pool.exception.ThriftConnectionPoolException;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Controller
public class HelloController {

	@Autowired
	private ServiceClientFactory clientFactory;

	@RequestMapping(value = "/sum", method = RequestMethod.GET)
	public ModelAndView sum(@RequestParam int a, @RequestParam int b) {
		Map<String, Object> resultMap = new HashMap<String, Object>();


		IHelloService.Client helloServiceClient = null;
		try {
			helloServiceClient = clientFactory.getClient("helloService", IHelloService.Client.class);
		} catch (ThriftConnectionPoolException e) {
			e.printStackTrace();
		}
		int sum = 0;
		try {
			sum = helloServiceClient.sum(a, b);
		} catch (TException e) {
			e.printStackTrace();
		}

		resultMap.put("sum", sum);
		return new ModelAndView("sum", resultMap);
	}
}
