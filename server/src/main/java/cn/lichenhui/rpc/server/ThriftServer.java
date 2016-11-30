package cn.lichenhui.rpc.server;

import cn.lichenhui.rpc.server.annotations.RpcService;
import cn.lichenhui.rpc.server.manage.register.RegisterService;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Thrift server
 *
 */
public class ThriftServer {
    private static final Logger log = LoggerFactory.getLogger(ThriftServer.class);

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath*:spring-context.xml");
        ctx.start();

        RegisterService register = ((RegisterService) ctx.getBean("registerService"));

        TMultiplexedProcessor multiplexedProcessor = new TMultiplexedProcessor();
//		PhotoRpcApiImpl photoRpcApi = (PhotoRpcApiImpl) ctx.getBean("photoRpcApiImpl");
//		PhotoApi.Processor photoProcessor = new PhotoApi.Processor(photoRpcApi);
//		multiplexedProcessor.registerProcessor("photoRpcApi", photoProcessor);
        try {
            multiplexedProcessor = registerProcessor(multiplexedProcessor, ctx, register);
            starServer(multiplexedProcessor, register.getPort(), register);
        } catch (Exception e) {
            log.error("start thrift server error", e);
        } finally {
            ctx.close();
            System.exit(1);
        }

    }

    public static void starServer(TProcessor tProcessor, int port, RegisterService registerService) throws TTransportException {
        TServerTransport serverTransport = new TServerSocket(port);

        TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
        serverArgs.processor(tProcessor);
        serverArgs.protocolFactory(new TBinaryProtocol.Factory(true, true));
        serverArgs.transportFactory(new TTransportFactory());
        TServer server = new TThreadPoolServer(serverArgs);

        try {
            registerService.registerHost();
        } catch (Exception e) {
            throw new Error("注册服务失败", e);
        }
        server.serve();
    }

    private static TMultiplexedProcessor registerProcessor(TMultiplexedProcessor multiplexedProcessor, ClassPathXmlApplicationContext ctx, RegisterService registerService) throws Exception {
        //扫描所有@RpcService注解的类,注册到thrift服务
        Map<String, Object> rpcServiceMap = ctx.getBeansWithAnnotation(RpcService.class);
        for (Map.Entry<String, Object> entry : rpcServiceMap.entrySet()) {
            Object rpcServiceObject = entry.getValue();
            RpcService apiClass = AnnotationUtils.findAnnotation(rpcServiceObject.getClass(), RpcService.class);
            if (StringUtils.isEmpty(apiClass.value())) {
                //忽略没有名称的service
                continue;
            }
            Class<?>[] interfaceClasses = ClassUtils.getAllInterfaces(rpcServiceObject);
            if (interfaceClasses == null || interfaceClasses.length == 0) {
                continue;
            }

            String ifaceSuperClassName = null;
            for (Class<?> interfaceClass : interfaceClasses) {
                if (StringUtils.endsWithIgnoreCase(interfaceClass.getName(), "$Iface")) {
                    Class<?> enclosingClass = interfaceClass.getEnclosingClass();
                    if (enclosingClass == null) {
                        continue;
                    }

                    ifaceSuperClassName = interfaceClass.getEnclosingClass().getName();
                    if (StringUtils.isEmpty(ifaceSuperClassName)) {
                        continue;
                    }

                    break;
                }
            }

            // 没找到iface父类，跳过
            if (StringUtils.isEmpty(ifaceSuperClassName)) {
                continue;
            }
            log.info("service name: {}, service class:{}", apiClass.value(), ifaceSuperClassName);
            registerService.registerService(apiClass.value(), ifaceSuperClassName);
            // 生成Processor对象
            Class<?> processorClass = ClassUtils.forName(ifaceSuperClassName + "$Processor", ClassUtils.getDefaultClassLoader());

            TProcessor apiProcessor = (TProcessor) ConstructorUtils.invokeConstructor(processorClass, rpcServiceObject);

            // 注册到Thrift服务
            multiplexedProcessor.registerProcessor(apiClass.value(), apiProcessor);
        }
        return multiplexedProcessor;
    }
}
